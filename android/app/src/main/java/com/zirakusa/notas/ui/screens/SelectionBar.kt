package com.zirakusa.notas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirakusa.notas.R
import com.zirakusa.notas.data.Section
import com.zirakusa.notas.ui.NotesState
import com.zirakusa.notas.ui.theme.NotesColors

/** Floating bar while selecting: pin, lock, move and delete (restore and delete forever in the trash). */
@Composable fun SelectionBar(state: NotesState, modifier: Modifier) =
    Row(
        modifier.navigationBarsPadding().padding(bottom = 12.dp).clip(CircleShape).background(NotesColors.Raised).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.section == Section.Trash) {
            Action(Icons.Outlined.RestoreFromTrash, stringResource(R.string.restore), state::restoreSelected)
            Action(Icons.Outlined.DeleteForever, stringResource(R.string.delete_forever), state::deleteSelectedForever)
            return@Row
        }
        Action(Icons.Outlined.PushPin, stringResource(R.string.pin), state::pinSelected)
        val locked = state.section == Section.Locked
        Action(if (locked) Icons.Outlined.LockOpen else Icons.Outlined.Lock, stringResource(if (locked) R.string.unlock else R.string.lock), state::lockSelected)
        var moving by remember { mutableStateOf(false) }
        Box {
            Action(Icons.AutoMirrored.Outlined.DriveFileMove, stringResource(R.string.move_to)) { moving = true }
            DropdownMenu(moving, { moving = false }) {
                DropdownMenuItem({ Text(stringResource(R.string.uncategorized)) }, { moving = false; state.moveSelected(null) })
                state.folders.forEach { f -> DropdownMenuItem({ Text(f.name) }, { moving = false; state.moveSelected(f.id) }) }
            }
        }
        Action(Icons.Outlined.Delete, stringResource(R.string.delete), state::trashSelected)
    }

@Composable private fun Action(icon: ImageVector, label: String, onClick: () -> Unit) =
    IconButton(onClick, Modifier.padding(vertical = 6.dp)) { Icon(icon, label, tint = NotesColors.Text) }
