package com.zirakusa.notas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.notas.data.Layout
import com.zirakusa.notas.data.Note
import com.zirakusa.notas.data.NotesRules
import com.zirakusa.notas.data.TextEdits
import com.zirakusa.notas.ui.theme.NotesColors
import com.zirakusa.notas.ui.theme.Skins
import com.zirakusa.notas.ui.theme.skin

/** How a card reacts: tap opens (or ticks while selecting), long press starts selecting. */
class CardActions(val date: (Note) -> String, val selected: Set<Long>, val selecting: Boolean, val onTap: (Note) -> Unit, val onLongPress: (Note) -> Unit)

/** The notes in the chosen view. */
@Composable fun NoteList(notes: List<Note>, layout: Layout, padding: PaddingValues, a: CardActions) {
    val gap = Arrangement.spacedBy(12.dp)
    when (layout) {
        Layout.GRID -> LazyVerticalStaggeredGrid(StaggeredGridCells.Fixed(2), contentPadding = padding, horizontalArrangement = gap, verticalItemSpacing = 12.dp) {
            items(notes, key = { it.id }) { NoteCard(it, previewLines = 2, a) }
        }
        Layout.CARD -> LazyColumn(contentPadding = padding, verticalArrangement = gap) {
            items(notes, key = { it.id }) { NoteCard(it, previewLines = 4, a) }
        }
        Layout.LIST -> LazyColumn(contentPadding = padding) {
            items(notes, key = { it.id }) { NoteRow(it, a); HorizontalDivider(color = NotesColors.Chip) }
        }
    }
}

@Composable private fun NoteCard(note: Note, previewLines: Int, a: CardActions) {
    val skin = Skins.of(note.color)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).then(if (note.color == 0) Modifier.background(NotesColors.Card) else Modifier.skin(skin))
            .combinedClickable(onLongClick = { a.onLongPress(note) }) { a.onTap(note) }.padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(TextEdits.plain(NotesRules.displayTitle(note)), Modifier.weight(1f), fontSize = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = skin.text)
            if (note.pinned) Icon(Icons.Filled.PushPin, null, Modifier.size(18.dp), tint = NotesColors.Accent)
        }
        val preview = TextEdits.plain(NotesRules.preview(note))
        if (preview.isNotEmpty()) {   // the whole text, also when its first line is the title (like the phone's notes)
            Spacer(Modifier.height(6.dp))
            Text(preview, color = skin.muted, fontSize = 16.sp, maxLines = previewLines, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DateChip(a.date(note))
            Spacer(Modifier.weight(1f))
            if (a.selecting) Tick(note.id in a.selected)
        }
    }
}

@Composable private fun NoteRow(note: Note, a: CardActions) = Row(
    Modifier.fillMaxWidth().combinedClickable(onLongClick = { a.onLongPress(note) }) { a.onTap(note) }.padding(vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Column(Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (note.pinned) Icon(Icons.Filled.PushPin, null, Modifier.size(16.dp).padding(end = 4.dp), tint = NotesColors.Accent)
            Text(TextEdits.plain(NotesRules.displayTitle(note)), fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Text(a.date(note), color = NotesColors.Muted, fontSize = 14.sp)
            Text(
                TextEdits.plain(NotesRules.preview(note)).replace('\n', ' '), Modifier.padding(start = 10.dp),
                color = NotesColors.Muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (a.selecting) Tick(note.id in a.selected)
}

/** The square at the corner of a card while selecting; yellow with a check when chosen. */
@Composable fun Tick(on: Boolean) = Box(
    Modifier.size(26.dp).clip(RoundedCornerShape(7.dp)).background(if (on) NotesColors.Accent else NotesColors.Card)
        .border(2.dp, if (on) NotesColors.Accent else NotesColors.Muted, RoundedCornerShape(7.dp)),
    contentAlignment = Alignment.Center,
) { if (on) Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = NotesColors.Background) }
