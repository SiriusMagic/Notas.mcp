package com.zirakusa.notas.ui.theme

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** «Fondo» of a note: a colour or gradient and maybe ruled lines, a grid or dots. [light] = dark text on it. */
data class Skin(val brush: Brush, val light: Boolean, val pattern: Pattern = Pattern.NONE, val ink: Color = Color(0x22000000)) {
    enum class Pattern { NONE, LINES, GRID, DOTS }

    val text get() = if (light) Color(0xFF222222) else NotesColors.Text
    val muted get() = if (light) Color(0xFF7A7A7A) else NotesColors.Muted
}

/** The backgrounds, in the order of the picker. Index 0 is «sin fondo» (the dark default); the index is kept in the note. */
object Skins {
    private fun solid(c: Long) = Brush.verticalGradient(listOf(Color(c), Color(c)))
    private fun solid(c: Color) = Brush.verticalGradient(listOf(c, c))
    private fun fade(a: Long, b: Long) = Brush.linearGradient(listOf(Color(a), Color(b)))

    val all = listOf(
        Skin(solid(0xFF000000), light = false),
        Skin(fade(0xFFE8697A, 0xFFD9A07A), light = true),
        Skin(solid(0xFFEFE8DA), light = true),
        Skin(solid(0xFFFAFAFA), light = true, Skin.Pattern.LINES),
        Skin(solid(0xFFFAFAFA), light = true, Skin.Pattern.GRID),
        Skin(solid(0xFFFBF6E6), light = true, Skin.Pattern.GRID, Color(0x33D9B44A)),
        Skin(solid(0xFFF5F5F5), light = true, Skin.Pattern.DOTS, Color(0x88000000)),
        Skin(fade(0xFFFBF9F4, 0xFFD6D4CF), light = true),
        Skin(fade(0xFFE8EEF3, 0xFF9DB6CC), light = true),
        Skin(fade(0xFF0E1A2B, 0xFF2B3F5C), light = false),
        Skin(fade(0xFF1E2A1E, 0xFF3D5A3A), light = false),
    )

    /** Index 0 («sin fondo») follows the app's theme (Ajustes › Tema); the others are the note's own. */
    fun of(index: Int) = if (index <= 0 || index >= all.size) Skin(solid(NotesColors.Background), NotesColors.isLight) else all[index]
}

/** Paints [skin] behind the content: colour first, then its lines, grid or dots every 28 dp. */
fun Modifier.skin(skin: Skin): Modifier = background(skin.brush).drawBehind {
    val step = 28.dp.toPx()
    when (skin.pattern) {
        Skin.Pattern.NONE -> Unit
        Skin.Pattern.LINES -> { var y = step * 2; while (y < size.height) { drawLine(skin.ink, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += step } }
        Skin.Pattern.GRID -> {
            var x = 0f; while (x < size.width) { drawLine(skin.ink, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx()); x += step }
            var y = 0f; while (y < size.height) { drawLine(skin.ink, Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += step }
        }
        Skin.Pattern.DOTS -> {
            var y = step; while (y < size.height) { var x = step; while (x < size.width) { drawCircle(skin.ink, 1.5.dp.toPx(), Offset(x, y)); x += step }; y += step }
        }
    }
}
