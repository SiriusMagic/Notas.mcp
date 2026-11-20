package com.zirakusa.notas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.notas.ui.theme.NotesColors

/** Round dark button with one icon (drawer, view, back, more…). */
@Composable fun RoundButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) =
    Box(
        modifier.size(52.dp).clip(CircleShape).background(NotesColors.Card).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = if (enabled) NotesColors.Text else NotesColors.Muted.copy(alpha = 0.5f)) }

/** The «Notas | Tareas» pill at the top. */
@Composable fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) =
    Row(Modifier.clip(CircleShape).background(NotesColors.Card).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEachIndexed { i, label ->
            Text(
                label,
                Modifier.clip(CircleShape).background(if (i == selected) NotesColors.Raised else NotesColors.Card)
                    .clickable { onSelect(i) }.padding(horizontal = 20.dp, vertical = 10.dp),
                color = if (i == selected) NotesColors.Accent else NotesColors.Text,
                fontSize = 17.sp, fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }

/** Small grey chip with the date under each card. */
@Composable fun DateChip(text: String) = Text(
    text, Modifier.clip(RoundedCornerShape(6.dp)).background(NotesColors.Chip).padding(horizontal = 8.dp, vertical = 3.dp),
    color = NotesColors.Muted, fontSize = 13.sp,
)
