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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
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

/** What the bottom of the side panel opens. */
class DrawerLinks(val widgets: () -> Unit, val feedback: () -> Unit, val settings: () -> Unit)

/**
 * Side panel «Carpetas»: fixed folders, the user's folders (long press = delete) and «Nuevo»; «Más › Añadir widget»;
 * and Comentarios / Ajustes at the bottom.
 */
@Composable fun FolderDrawer(state: NotesState, onPick: (Section) -> Unit, onNewFolder: () -> Unit, onDeleteFolder: (Long) -> Unit, links: DrawerLinks) =
    Column(
        Modifier.fillMaxHeight().width(320.dp).clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
            .background(NotesColors.Raised).statusBarsPadding().navigationBarsPadding().padding(16.dp),
    ) {
      Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
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
        HorizontalDivider(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), color = NotesColors.Chip)
        Text(stringResource(R.string.more_section), color = NotesColors.Muted, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = links.widgets).padding(horizontal = 8.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.add_widget), Modifier.weight(1f), fontSize = 18.sp)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = NotesColors.Muted)
        }
      }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Bottom(Icons.Outlined.HeadsetMic, stringResource(R.string.feedback), links.feedback)
            Bottom(Icons.Outlined.Settings, stringResource(R.string.settings), links.settings)
        }
    }

@Composable private fun androidx.compose.foundation.layout.RowScope.Bottom(icon: ImageVector, label: String, onClick: () -> Unit) =
    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(30.dp), tint = NotesColors.Muted)
        Text(label, color = NotesColors.Muted, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp))
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
