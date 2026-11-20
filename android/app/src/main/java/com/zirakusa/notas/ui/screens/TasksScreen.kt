package com.zirakusa.notas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.notas.R
import com.zirakusa.notas.data.Task
import com.zirakusa.notas.ui.TasksState
import com.zirakusa.notas.ui.theme.NotesColors

/** «Tareas»: pending first, done ones crossed out below. Tap the circle to tick, tap the text to edit. */
@Composable fun TasksList(tasks: TasksState, query: String) {
    val list = tasks.visible(query)
    if (list.isEmpty()) {
        Column(Modifier.fillMaxWidth().padding(top = 160.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Sell, null, Modifier.size(72.dp), tint = NotesColors.Chip)
            Text(stringResource(if (query.isBlank()) R.string.no_tasks else R.string.no_results), color = NotesColors.Muted, fontSize = 17.sp)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(list, key = { it.id }) { TaskRow(it, tasks) }
    }
}

@Composable private fun TaskRow(task: Task, tasks: TasksState) =
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(NotesColors.Card).clickable { tasks.edit(task) }
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val label = stringResource(if (task.done) R.string.task_undo else R.string.task_done)
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(if (task.done) NotesColors.Accent else NotesColors.Card)
                .border(2.dp, if (task.done) NotesColors.Accent else NotesColors.Muted, CircleShape).clickable(onClickLabel = label) { tasks.toggle(task) },
            contentAlignment = Alignment.Center,
        ) { if (task.done) Icon(Icons.Filled.Check, label, Modifier.size(18.dp), tint = NotesColors.Background) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(task.text, fontSize = 18.sp, color = if (task.done) NotesColors.Muted else NotesColors.Text,
                textDecoration = if (task.done) TextDecoration.LineThrough else null)
            task.remindAt?.let { Text(when_(it), color = NotesColors.Accent, fontSize = 13.sp) }
        }
    }

/** «Nueva tarea» (or editing one), with «Añadir un recordatorio.» and «Eliminar» for an existing task. */
@Composable fun TaskDialog(tasks: TasksState, allowNotifications: () -> Unit = {}) {
    val task = tasks.editing ?: return
    var remind by remember(task) { mutableStateOf(task.remindAt) }
    var picking by remember(task) { mutableStateOf(false) }
    InputDialog(stringResource(if (task.id == 0L) R.string.new_task else R.string.edit_task), task.text, 500, tasks::cancel, { tasks.done(it, remind) },
        onDelete = if (task.id != 0L) ({ tasks.delete(task) }) else null,
        extra = {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp).clickable { picking = true }, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Alarm, null, tint = if (remind != null) NotesColors.Accent else NotesColors.Muted)
                Spacer(Modifier.width(10.dp))
                Text(remind?.let { when_(it) } ?: stringResource(R.string.add_reminder), color = if (remind != null) NotesColors.Accent else NotesColors.Text, fontSize = 16.sp)
            }
        })
    if (picking) ReminderPicker(remind, { at -> if (at != null) allowNotifications(); remind = at; picking = false }) { picking = false }
}

private fun when_(millis: Long): String = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault())
    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy, hh:mm a"))
