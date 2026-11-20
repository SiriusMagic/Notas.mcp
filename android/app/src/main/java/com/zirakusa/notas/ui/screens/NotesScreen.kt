package com.zirakusa.notas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewSidebar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.notas.R
import com.zirakusa.notas.data.Layout
import com.zirakusa.notas.data.Note
import com.zirakusa.notas.data.NotesRules
import com.zirakusa.notas.data.Section
import com.zirakusa.notas.ui.NotesState
import com.zirakusa.notas.ui.Tab
import com.zirakusa.notas.ui.TasksState
import com.zirakusa.notas.ui.theme.NotesColors

private val layouts = listOf(
    Triple(Layout.LIST, Icons.AutoMirrored.Outlined.ViewList, R.string.view_list),
    Triple(Layout.CARD, Icons.Outlined.ViewAgenda, R.string.view_card),
    Triple(Layout.GRID, Icons.Outlined.GridView, R.string.view_grid),
)

/** Main screen: drawer button, «Notas | Tareas», view menu, big title, the list, and search + new at the bottom. */
@Composable fun NotesScreen(state: NotesState, tasks: TasksState, title: String, date: (Note) -> String, allowNotifications: () -> Unit = {}, onDrawer: () -> Unit) {
    var ai by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(NotesColors.Background).statusBarsPadding()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            if (state.selecting) SelectionTop(state) else TopRow(state, onDrawer)
            val big = if (state.tab == Tab.TASKS) stringResource(R.string.tasks) else title
            Text(big, Modifier.padding(vertical = 16.dp), fontSize = 40.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (state.tab == Tab.TASKS) TasksList(tasks, state.query) else Body(state, date)
        }
        if (state.selecting) SelectionBar(state, Modifier.align(Alignment.BottomCenter))
        else BottomBar(state, Modifier.align(Alignment.BottomCenter), { ai = true }) { if (state.tab == Tab.TASKS) tasks.startNew() else state.newNote() }
        TaskDialog(tasks, allowNotifications)
        if (ai) AiPanel { ai = false }
    }
}

@Composable private fun TopRow(state: NotesState, onDrawer: () -> Unit) = Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    RoundButton(Icons.Outlined.ViewSidebar, stringResource(R.string.folders), onClick = onDrawer)
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Segmented(listOf(stringResource(R.string.notes), stringResource(R.string.tasks)), state.tab.ordinal) { state.tab = Tab.entries[it] }
    }
    var open by remember { mutableStateOf(false) }
    Box(Modifier.size(52.dp)) {
        if (state.tab == Tab.TASKS) return@Box
        RoundButton(layouts.first { it.first == state.layout }.second, stringResource(R.string.view)) { open = true }
        DropdownMenu(open, { open = false }) {
            layouts.forEach { (layout, icon, label) ->
                val tint = if (layout == state.layout) NotesColors.Accent else NotesColors.Text
                DropdownMenuItem({ Text(stringResource(label), color = tint) }, { open = false; state.changeLayout(layout) },
                    leadingIcon = { Icon(icon, null, tint = tint) })
            }
        }
    }
}

/** While selecting: back (leave selection), how many, and select all. */
@Composable private fun SelectionTop(state: NotesState) = Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    RoundButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), onClick = state::clearSelection)
    Text(pluralStringResource(R.plurals.selected, state.selected.size, state.selected.size), Modifier.weight(1f).padding(horizontal = 16.dp),
        fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    val all = stringResource(R.string.select_all)
    Box(Modifier.size(52.dp).clip(CircleShape).background(NotesColors.Card).clickable(onClickLabel = all, onClick = state::selectAll),
        contentAlignment = Alignment.Center) { Tick(state.selected.size == state.visible.size) }
}

@Composable private fun Body(state: NotesState, date: (Note) -> String) {
    if (state.section == Section.Trash && state.visible.isNotEmpty() && !state.selecting) Row(verticalAlignment = Alignment.CenterVertically) {
        Text(pluralStringResource(R.plurals.trash_info, NotesRules.TRASH_DAYS.toInt(), NotesRules.TRASH_DAYS.toInt()), Modifier.weight(1f), color = NotesColors.Muted)
        TextButton(state::emptyTrash) { Text(stringResource(R.string.empty_trash), color = NotesColors.Danger) }
    }
    val notes = state.visible
    if (notes.isEmpty()) Message(stringResource(if (state.query.isBlank()) R.string.no_notes else R.string.no_results))
    else NoteList(notes, state.layout, PaddingValues(bottom = 120.dp), CardActions(date, state.selected, state.selecting,
        onTap = { if (state.selecting) state.toggleSelect(it) else state.open(it) }, onLongPress = state::toggleSelect))
}

@Composable private fun Message(text: String) =
    Box(Modifier.fillMaxWidth().padding(top = 120.dp), contentAlignment = Alignment.Center) { Text(text, color = NotesColors.Muted, fontSize = 17.sp) }

@Composable private fun BottomBar(state: NotesState, modifier: Modifier, onAi: () -> Unit, onNew: () -> Unit) =
    Row(modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).height(56.dp).clip(CircleShape).background(NotesColors.Raised).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, null, tint = NotesColors.Muted)
            Spacer(Modifier.width(10.dp))
            val style = TextStyle(color = NotesColors.Text, fontSize = 18.sp)
            BasicTextField(state.query, { state.query = it }, Modifier.weight(1f), singleLine = true, textStyle = style,
                cursorBrush = SolidColor(NotesColors.Accent),
                decorationBox = { inner -> Box { if (state.query.isEmpty()) Text(stringResource(R.string.search), style = style.copy(color = NotesColors.Muted)); inner() } })
        }
        if (state.tab == Tab.NOTES && state.section != Section.Trash) RoundButton(Icons.Outlined.AutoAwesome, stringResource(R.string.ai_notes), onClick = onAi)
        val label = stringResource(if (state.tab == Tab.TASKS) R.string.new_task else R.string.new_note)
        if (state.tab == Tab.TASKS || state.section != Section.Trash) RoundButton(Icons.Outlined.Edit, label, onClick = onNew)
    }
