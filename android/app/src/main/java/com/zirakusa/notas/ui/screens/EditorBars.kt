package com.zirakusa.notas.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zirakusa.notas.R
import com.zirakusa.notas.data.TextEdits
import com.zirakusa.notas.ui.EditorPlatform
import com.zirakusa.notas.ui.Editing
import com.zirakusa.notas.ui.NotesState

@Composable fun UndoRedo(e: Editing, light: Boolean) = IconPill(light) {
    PillIcon(Icons.AutoMirrored.Outlined.Undo, stringResource(R.string.undo), light, e.history.canUndo, onClick = e::undo)
    PillIcon(Icons.AutoMirrored.Outlined.Redo, stringResource(R.string.redo), light, e.history.canRedo, onClick = e::redo)
}

/** While reading: share, background and ⋮. */
@Composable fun ReadingActions(light: Boolean, onShare: () -> Unit, onPanel: (Panel) -> Unit) = IconPill(light) {
    PillIcon(Icons.Outlined.Share, stringResource(R.string.share), light, onClick = onShare)
    PillIcon(Icons.Outlined.Checkroom, stringResource(R.string.background), light) { onPanel(Panel.SKIN) }
    PillIcon(Icons.Outlined.MoreVert, stringResource(R.string.more), light) { onPanel(Panel.MENU) }
}

/**
 * The floating bar over the keyboard: ✦ AI, Aa, camera, drawing, checklist, table and dictation while writing;
 * camera, drawing and checklist while reading.
 */
@Composable fun EditorToolbar(e: Editing, platform: EditorPlatform, writing: Boolean, light: Boolean, onPanel: (Panel) -> Unit) =
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        IconPill(light) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (writing) {
                    PillIcon(Icons.Outlined.AutoAwesome, stringResource(R.string.ai_notes), light) { onPanel(Panel.AI) }
                    PillIcon(Icons.Outlined.TextFields, stringResource(R.string.format), light) { onPanel(Panel.FORMAT) }
                }
                PillIcon(Icons.Outlined.PhotoCamera, stringResource(R.string.camera), light) { onPanel(Panel.CAMERA) }
                PillIcon(Icons.Outlined.Draw, stringResource(R.string.drawing), light) { onPanel(Panel.DRAW) }
                PillIcon(Icons.Outlined.TaskAlt, stringResource(R.string.checklist), light) { e.tool(TextEdits::cycleCheck) }
                if (writing) {
                    PillIcon(Icons.Outlined.GridOn, stringResource(R.string.table), light) { e.tool { TextEdits.table(it) } }
                    PillIcon(Icons.Outlined.Mic, stringResource(R.string.dictate), light, onClick = platform::dictate)
                }
            }
        }
    }

/** Whichever panel is open over the note. */
@Composable fun Panels(panel: Panel, state: NotesState, e: Editing, platform: EditorPlatform, now: Long, onPanel: (Panel) -> Unit) {
    val close = { onPanel(Panel.NONE) }
    when (panel) {
        Panel.NONE -> Unit
        Panel.AI -> AiPanel(close)
        Panel.FORMAT -> FormatPanel(e, close)
        Panel.CAMERA -> CameraPanel(platform, close)
        Panel.SKIN -> SkinPanel(e.note.color, { state.changeOpen { n -> n.copy(color = it) } }, close)
        Panel.DRAW -> DrawingPanel({ platform.saveDrawing(it); close() }, close)
        Panel.MOVE -> MovePanel(state, close)
        Panel.REMIND -> ReminderPicker(e.note.remindAt, { at -> if (at != null) platform.allowNotifications(); state.remindOpen(at); close() }, close)
        Panel.MENU -> NoteMenu(
            e, created = stampCreated(e.note.createdAt.takeIf { it > 0 } ?: now),
            reminder = e.note.remindAt?.let { stampCreated(it) },
            a = MenuActions(
                pin = { state.changeOpen { it.copy(pinned = !it.pinned) } }, background = { onPanel(Panel.SKIN) },
                lock = { state.changeOpen { it.copy(locked = !it.locked) } }, share = platform::share, home = platform::pinToHome,
                remind = { onPanel(Panel.REMIND) }, compress = platform::compressImages, move = { onPanel(Panel.MOVE) },
                delete = state::trashOpen,
            ),
            onDone = close,
        )
    }
}

private fun stampCreated(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm a"))

/** «Mover nota»: «Sin categoría» or one of the folders. */
@Composable fun MovePanel(state: NotesState, onDone: () -> Unit) = BottomPanel(onDone) {
    androidx.compose.material3.Text(stringResource(R.string.move_note), fontSize = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp))
    val current = state.editing?.note?.folderId
    (listOf<Pair<Long?, String>>(null to stringResource(R.string.uncategorized)) + state.folders.map { it.id to it.name }).forEach { (id, name) ->
        androidx.compose.material3.Text(name, Modifier.fillMaxWidth().clickable { state.moveOpen(id); onDone() }.padding(vertical = 12.dp),
            color = if (id == current) com.zirakusa.notas.ui.theme.NotesColors.Accent else com.zirakusa.notas.ui.theme.NotesColors.Text,
            fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp))
    }
}
