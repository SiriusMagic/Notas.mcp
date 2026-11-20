package com.zirakusa.notas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.notas.R
import com.zirakusa.notas.data.Section
import com.zirakusa.notas.ui.NotesState
import com.zirakusa.notas.ui.theme.NotesColors

/** Side panel «Carpetas»: fixed folders, the user's folders (long press = delete) and «Nuevo». */
@Composable fun FolderDrawer(state: NotesState, onPick: (Section) -> Unit, onNewFolder: () -> Unit, onDeleteFolder: (Long) -> Unit) =
    Column(
        Modifier.fillMaxHeight().width(320.dp).clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
            .background(NotesColors.Raised).statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
            Text(stringResource(R.string.folders), color = NotesColors.Muted, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onNewFolder) { Text(stringResource(R.string.new_folder_short), color = NotesColors.Accent, fontSize = 16.sp) }
        }
        val fixed = listOf(
            Triple(Section.All, Icons.Outlined.Inbox, R.string.all_notes),
            Triple(Section.Uncategorized, Icons.Outlined.Description, R.string.uncategorized),
            Triple(Section.Locked, Icons.Outlined.Lock, R.string.locked),
            Triple(Section.Trash, Icons.Outlined.Delete, R.string.trash),
        )
        fixed.take(2).forEach { (s, icon, label) -> FolderRow(icon, stringResource(label), state.count(s), state.section == s) { onPick(s) } }
        state.folders.forEach { f ->
            val s = Section.InFolder(f.id)
            FolderRow(Icons.Outlined.Folder, f.name, state.count(s), state.section == s, onLongClick = { onDeleteFolder(f.id) }) { onPick(s) }
        }
        fixed.drop(2).forEach { (s, icon, label) -> FolderRow(icon, stringResource(label), state.count(s), state.section == s) { onPick(s) } }
    }

@Composable private fun FolderRow(icon: ImageVector, name: String, count: Int, selected: Boolean, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) =
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (selected) NotesColors.Chip else NotesColors.Raised)
            .combinedClickable(onLongClick = onLongClick, onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = NotesColors.Muted)
        Text(name, Modifier.weight(1f).padding(start = 16.dp), fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(count.toString(), color = NotesColors.Muted, fontSize = 16.sp)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = NotesColors.Muted)
    }
