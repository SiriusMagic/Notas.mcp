package com.zirakusa.notas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.notas.R
import com.zirakusa.notas.data.TextEdits
import com.zirakusa.notas.ui.Editing
import com.zirakusa.notas.ui.theme.NotesColors

/** «Aa»: H1–H4 / Cuerpo for the line, B I U S for the selection, and numbered, bullet and check lists. */
@Composable fun FormatPanel(e: Editing, onDone: () -> Unit) = BottomPanel(onDone) {
    val level = e.heading
    Group {
        listOf("H1", "H2", "H3", "H4").forEachIndexed { i, label ->
            Cell(level == i + 1, { e.tool { TextEdits.togglePrefix(it, TextEdits.headings[i]) } }) {
                Text(label, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = color(level == i + 1))
            }
        }
        Cell(level == 0, { e.tool(TextEdits::body) }) { Text(stringResource(R.string.body_text), fontSize = 16.sp, color = color(level == 0)) }
    }
    Group {
        Mark(Icons.Outlined.FormatBold, R.string.bold) { e.tool { TextEdits.wrap(it, "**") } }
        Mark(Icons.Outlined.FormatItalic, R.string.italic) { e.tool { TextEdits.wrap(it, "*") } }
        Mark(Icons.Outlined.FormatUnderlined, R.string.underline) { e.tool { TextEdits.wrap(it, "__") } }
        Mark(Icons.Outlined.FormatStrikethrough, R.string.strike) { e.tool { TextEdits.wrap(it, "~~") } }
    }
    Group {
        Mark(Icons.Outlined.FormatListNumbered, R.string.numbered_list) { e.tool { TextEdits.togglePrefix(it, "1. ") } }
        Mark(Icons.AutoMirrored.Outlined.FormatListBulleted, R.string.bullet_list) { e.tool { TextEdits.togglePrefix(it, TextEdits.BULLET) } }
        Mark(Icons.Outlined.Checklist, R.string.checklist) { e.tool(TextEdits::cycleCheck) }
    }
}

private fun color(on: Boolean) = if (on) NotesColors.OnAccent else NotesColors.Text

@Composable private fun Group(content: @Composable RowScope.() -> Unit) =
    Row(Modifier.height(56.dp).clip(RoundedCornerShape(16.dp)).background(NotesColors.Chip), horizontalArrangement = Arrangement.spacedBy(1.dp), content = content)

@Composable private fun RowScope.Cell(on: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) =
    Box(Modifier.weight(1f).fillMaxHeight().background(if (on) NotesColors.Accent else NotesColors.Chip).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) { content() }

@Composable private fun RowScope.Mark(icon: ImageVector, label: Int, onClick: () -> Unit) =
    Cell(false, onClick) { Icon(icon, stringResource(label), tint = NotesColors.Text) }
