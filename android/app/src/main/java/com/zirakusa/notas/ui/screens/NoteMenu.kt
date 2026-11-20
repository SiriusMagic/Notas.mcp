package com.zirakusa.notas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.AddToHomeScreen
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.notas.R
import com.zirakusa.notas.data.Attachment
import com.zirakusa.notas.ui.Editing
import com.zirakusa.notas.ui.theme.NotesColors

/** What the ⋮ menu can do; the editor fills it in. */
class MenuActions(
    val pin: () -> Unit, val background: () -> Unit, val lock: () -> Unit, val share: () -> Unit, val home: () -> Unit,
    val remind: () -> Unit, val compress: () -> Unit, val move: () -> Unit, val delete: () -> Unit,
)

/** ⋮ inside a note: Fijar, Fondo, Bloquear, Compartir; then the list; «Creado en» at the bottom. */
@Composable fun NoteMenu(e: Editing, created: String, reminder: String?, a: MenuActions, onDone: () -> Unit) = BottomPanel(onDone) {
    fun run(action: () -> Unit) = { onDone(); action() }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Tile(Icons.Outlined.PushPin, stringResource(if (e.note.pinned) R.string.unpin else R.string.pin), onClick = run(a.pin))
        Tile(Icons.Outlined.Checkroom, stringResource(R.string.background), onClick = run(a.background))
        Tile(if (e.note.locked) Icons.Outlined.LockOpen else Icons.Outlined.Lock, stringResource(if (e.note.locked) R.string.unlock else R.string.lock), onClick = run(a.lock))
        Tile(Icons.Outlined.Share, stringResource(R.string.share), onClick = run(a.share))
    }
    Column(Modifier.clip(RoundedCornerShape(20.dp)).background(NotesColors.Background)) {
        Item(Icons.Outlined.AddToHomeScreen, stringResource(R.string.add_to_home), onClick = run(a.home))
        Item(Icons.Outlined.Alarm, reminder ?: stringResource(R.string.reminder), onClick = run(a.remind))
        Item(Icons.Outlined.Compress, stringResource(R.string.compress_images), enabled = e.attachments.any { it.kind == Attachment.Kind.PHOTO }, onClick = run(a.compress))
        Item(Icons.AutoMirrored.Outlined.DriveFileMove, stringResource(R.string.move_note), onClick = run(a.move))
        Item(Icons.Outlined.Delete, stringResource(R.string.delete), last = true, onClick = run(a.delete))
    }
    Text(stringResource(R.string.created_at, created), color = NotesColors.Muted, fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp))
}

@Composable private fun Item(icon: ImageVector, label: String, enabled: Boolean = true, last: Boolean = false, onClick: () -> Unit) {
    val tint = if (enabled) NotesColors.Text else NotesColors.Muted
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(icon, null, tint = tint)
        Text(label, color = tint, fontSize = 18.sp)
    }
    if (!last) HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = NotesColors.Chip)
}
