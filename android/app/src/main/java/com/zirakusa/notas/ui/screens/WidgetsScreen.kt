package com.zirakusa.notas.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.notas.R
import com.zirakusa.notas.data.Note
import com.zirakusa.notas.data.NotesRules
import com.zirakusa.notas.data.Task
import com.zirakusa.notas.data.TextEdits
import com.zirakusa.notas.ui.theme.NotesColors

enum class WidgetKind { NOTE, NOTE_SMALL, TASKS, LIST }

private val Paper = Color(0xFFEFE8DA)
private val Ink = Color(0xFF111111)
private val Faded = Color(0xFF8A847A)

/** «Añadir widget»: what each widget looks like with your own notes; a tap asks the home screen to place it. */
@Composable fun WidgetsScreen(notes: List<Note>, tasks: List<Task>, date: (Note) -> String, onPick: (WidgetKind) -> Unit, onBack: () -> Unit) =
    Column(Modifier.fillMaxSize().background(NotesColors.Background).systemBarsPadding().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), onClick = onBack)
            Text(stringResource(R.string.add_widget), Modifier.padding(start = 16.dp), fontSize = 26.sp)
        }
        Text(stringResource(R.string.widget_hint), color = NotesColors.Muted)
        val note = notes.firstOrNull()
        Labeled("4×2") { NoteCard(note, date, big = true, Modifier.fillMaxWidth().aspectRatio(2.1f).clickable { onPick(WidgetKind.NOTE) }) }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) { Labeled("2×2") { NoteCard(note, date, big = false, Modifier.fillMaxWidth().aspectRatio(1f).clickable { onPick(WidgetKind.NOTE_SMALL) }) } }
            Column(Modifier.weight(1.3f)) { Labeled("2×3") { TasksCard(tasks, Modifier.fillMaxWidth().aspectRatio(1.3f).clickable { onPick(WidgetKind.TASKS) }) } }
        }
        Labeled("4×2") { ListCard(notes.take(2), date, Modifier.fillMaxWidth().clickable { onPick(WidgetKind.LIST) }) }
    }

@Composable private fun ColumnScope.Labeled(size: String, content: @Composable () -> Unit) {
    content()
    Text(size, color = NotesColors.Muted, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp, bottom = 10.dp))
}

@Composable private fun Mark(size: Int) = Image(painterResource(R.drawable.widget_icon), null, Modifier.size(size.dp))

@Composable private fun NoteCard(note: Note?, date: (Note) -> String, big: Boolean, modifier: Modifier) =
    Column(modifier.clip(RoundedCornerShape(28.dp)).background(Paper).padding(if (big) 22.dp else 16.dp)) {
        Mark(if (big) 28 else 22)
        Spacer(Modifier.size(10.dp))
        Text(note?.let { TextEdits.plain(NotesRules.displayTitle(it)) } ?: stringResource(R.string.widget_empty), color = Ink,
            fontSize = if (big) 24.sp else 18.sp, fontWeight = FontWeight.Bold, maxLines = if (big) 1 else 2, overflow = TextOverflow.Ellipsis)
        Text(note?.let { TextEdits.plain(NotesRules.preview(it)) }.orEmpty(), Modifier.weight(1f), color = Faded,
            fontSize = if (big) 16.sp else 12.sp, maxLines = if (big) 2 else 3, overflow = TextOverflow.Ellipsis)
        Text(note?.let(date).orEmpty(), color = Faded, fontSize = 13.sp)
    }

@Composable private fun TasksCard(tasks: List<Task>, modifier: Modifier) =
    Column(modifier.clip(RoundedCornerShape(28.dp)).background(Color(0xFFFBF7EC)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.widget_tasks), Modifier.weight(1f), color = Color(0xFF3A2A10), fontSize = 18.sp)
            Text("+", color = Color(0xFF3A2A10), fontSize = 26.sp)
        }
        val open = tasks.filter { !it.done }.take(3)
        if (open.isEmpty()) Text(stringResource(R.string.widget_no_tasks), color = Faded)
        open.forEach { t ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(20.dp).border(2.dp, Color(0xFFE8B10E), CircleShape))
                Text(t.text, Modifier.padding(start = 10.dp), color = Color(0xFF3A2A10), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }

@Composable private fun ListCard(notes: List<Note>, date: (Note) -> String, modifier: Modifier) =
    Column(modifier.clip(RoundedCornerShape(28.dp)).background(Color(0xFF333333)).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Mark(26)
            Text(stringResource(R.string.widget_notes), Modifier.padding(start = 10.dp), color = Color.White, fontSize = 20.sp)
        }
        if (notes.isEmpty()) Text(stringResource(R.string.widget_empty), color = Color(0xFFAAAAAA), modifier = Modifier.padding(top = 8.dp))
        notes.forEachIndexed { i, n ->
            if (i > 0) HorizontalDivider(color = Color(0xFF4A4A4A))
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(TextEdits.plain(NotesRules.displayTitle(n)), color = Color.White, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(date(n) + "  " + TextEdits.plain(NotesRules.preview(n)).replace('\n', ' '), color = Color(0xFFAAAAAA), fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
