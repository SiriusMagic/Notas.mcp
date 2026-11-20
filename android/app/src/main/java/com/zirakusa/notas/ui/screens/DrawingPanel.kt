package com.zirakusa.notas.ui.screens

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoFixNormal
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.zirakusa.notas.R
import com.zirakusa.notas.ui.theme.NotesColors

/** Pencil, pen, marker (see-through) and eraser (paints the paper's white). */
enum class Pen(val width: Float, val alpha: Float) { PENCIL(4f, 1f), PEN(8f, 1f), MARKER(26f, 0.4f), ERASER(36f, 1f) }

/** One line drawn with a finger. */
class Line(val points: List<Offset>, val color: Color, val pen: Pen)

private val inks = listOf(Color(0xFF111111), Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFF5C518), Color(0xFF8E24AA))

/** Drawing on white paper; ✓ hands over the picture (it becomes a drawing of the note), ✕ throws it away. */
@Composable fun DrawingPanel(onSave: (Bitmap) -> Unit, onClose: () -> Unit) {
    val lines = remember { mutableStateListOf<Line>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var pen by remember { mutableStateOf(Pen.PEN) }
    var ink by remember { mutableStateOf(inks[0]) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    Column(Modifier.fillMaxSize().background(NotesColors.Background).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton(Icons.Outlined.Close, stringResource(R.string.cancel), onClick = onClose)
            Box(Modifier.weight(1f))
            RoundButton(Icons.AutoMirrored.Outlined.Undo, stringResource(R.string.undo), enabled = lines.isNotEmpty()) { lines.removeAt(lines.lastIndex) }
            Box(Modifier.size(12.dp))
            RoundButton(Icons.Outlined.Check, stringResource(R.string.done), enabled = lines.isNotEmpty()) { onSave(render(lines, size)) }
        }
        Canvas(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
                .onSizeChanged { size = it }
                .pointerInput(pen, ink) {
                    detectDragGestures(
                        onDragStart = { current = listOf(it) },
                        onDrag = { change, _ -> current = current + change.position },
                        onDragEnd = { lines.add(Line(current, if (pen == Pen.ERASER) Color.White else ink, pen)); current = emptyList() },
                    )
                },
        ) {
            (lines + Line(current, if (pen == Pen.ERASER) Color.White else ink, pen)).forEach { line ->
                if (line.points.size > 1) drawPath(path(line.points), line.color.copy(alpha = line.pen.alpha),
                    style = Stroke(line.pen.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            val next = inks[(inks.indexOf(ink) + 1) % inks.size]
            Box(Modifier.size(40.dp).clip(CircleShape).background(ink).border(3.dp, NotesColors.Text, CircleShape).clickable { ink = next })
            Tool(Icons.Outlined.Edit, R.string.pencil, pen == Pen.PENCIL) { pen = Pen.PENCIL }
            Tool(Icons.Outlined.Create, R.string.pen, pen == Pen.PEN) { pen = Pen.PEN }
            Tool(Icons.Outlined.Brush, R.string.marker, pen == Pen.MARKER) { pen = Pen.MARKER }
            Tool(Icons.Outlined.AutoFixNormal, R.string.eraser, pen == Pen.ERASER) { pen = Pen.ERASER }
        }
    }
}

@Composable private fun Tool(icon: ImageVector, label: Int, on: Boolean, onClick: () -> Unit) =
    Box(Modifier.size(52.dp).clip(CircleShape).background(if (on) NotesColors.Accent else NotesColors.Card).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) { Icon(icon, stringResource(label), tint = if (on) NotesColors.OnAccent else NotesColors.Text) }

private fun path(points: List<Offset>) = Path().apply { moveTo(points[0].x, points[0].y); points.drop(1).forEach { lineTo(it.x, it.y) } }

/** The drawing as a picture the size of the paper. */
fun render(lines: List<Line>, size: IntSize): Bitmap {
    val bitmap = createBitmap(size.width.coerceAtLeast(1), size.height.coerceAtLeast(1))
    val canvas = android.graphics.Canvas(bitmap).apply { drawColor(android.graphics.Color.WHITE) }
    lines.filter { it.points.size > 1 }.forEach { line ->
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = line.color.copy(alpha = line.pen.alpha).toArgb(); strokeWidth = line.pen.width
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val p = android.graphics.Path().apply { moveTo(line.points[0].x, line.points[0].y); line.points.drop(1).forEach { lineTo(it.x, it.y) } }
        canvas.drawPath(p, paint)
    }
    return bitmap
}
