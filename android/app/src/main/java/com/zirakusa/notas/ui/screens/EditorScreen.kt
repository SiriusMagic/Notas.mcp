package com.zirakusa.notas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.notas.R
import com.zirakusa.notas.data.Attachment
import com.zirakusa.notas.data.NotesRules
import com.zirakusa.notas.ui.EditorPlatform
import com.zirakusa.notas.ui.Editing
import com.zirakusa.notas.ui.NoteFormat
import com.zirakusa.notas.ui.NotesState
import com.zirakusa.notas.ui.theme.NotesColors
import com.zirakusa.notas.ui.theme.Skins
import com.zirakusa.notas.ui.theme.skin
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class Panel { NONE, AI, FORMAT, CAMERA, DRAW, SKIN, MENU, MOVE, REMIND }

private fun stamp(millis: Long, pattern: String) = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern))

/**
 * A note: reading (share, background, ⋮, short toolbar) or writing (↶ ↷, ⋮, ✓, full toolbar), on its background,
 * with date, title, formatted text, photos and drawings. A note in the trash is only read.
 */
@Composable fun EditorScreen(state: NotesState, e: Editing, platform: EditorPlatform, now: Long) {
    var panel by remember { mutableStateOf(Panel.NONE) }
    var viewing by remember { mutableStateOf<Attachment?>(null) }
    val focus = LocalFocusManager.current
    val skin = Skins.of(e.note.color)
    Box(Modifier.fillMaxSize().skin(skin)) {
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            TopBar(state, e, platform, skin.light, onPanel = { panel = it }, onDone = { focus.clearFocus(); state.saveOpen() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                val info = if (e.readOnly) NotesRules.daysLeft(e.note, now).toInt().let { pluralStringResource(R.plurals.days_left, it, it) }
                else stamp(e.note.updatedAt.takeIf { it > 0 } ?: now, "MM/dd/yyyy, hh:mm a")
                Text(info, color = skin.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
                e.note.remindAt?.let { at ->
                    Row(Modifier.padding(top = 6.dp).clickable { panel = Panel.REMIND }, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Alarm, null, Modifier.padding(end = 6.dp), tint = NotesColors.Accent)
                        Text(stamp(at, "MM/dd/yyyy, hh:mm a"), color = NotesColors.Accent, fontSize = 14.sp)
                    }
                }
                val writing = Modifier.onFocusChanged { if (it.isFocused) e.writing = true }
                Field(e.draft.title, e::changeTitle, stringResource(R.string.title_hint), e.readOnly,
                    TextStyle(color = skin.text, fontSize = 28.sp, fontWeight = FontWeight.Bold), writing)
                BasicTextField(e.body, e::changeBody, writing.fillMaxWidth().padding(vertical = 10.dp), readOnly = e.readOnly,
                    textStyle = TextStyle(color = skin.text, fontSize = 18.sp, lineHeight = 27.sp), cursorBrush = SolidColor(NotesColors.Accent),
                    visualTransformation = remember(skin) { NoteFormat(skin.muted, NotesColors.Accent) },
                    decorationBox = { inner -> Box { if (e.body.text.isEmpty()) Text(stringResource(R.string.body_hint), color = skin.muted, fontSize = 18.sp); inner() } })
                AttachmentList(e.attachments, state.attachmentDir) { viewing = it }
                Spacer(Modifier.padding(bottom = 96.dp))
            }
            if (!e.readOnly) EditorToolbar(e, platform, writing = e.writing, light = skin.light) { panel = it }
        }
        Panels(panel, state, e, platform, now) { panel = it }
        viewing?.let { a -> AttachmentViewer(a, state.attachmentDir, !e.readOnly, { state.detach(a); viewing = null }) { viewing = null } }
    }
}

@Composable private fun TopBar(state: NotesState, e: Editing, platform: EditorPlatform, light: Boolean, onPanel: (Panel) -> Unit, onDone: () -> Unit) =
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), light = light, onClick = state::closeEditor)
        Spacer(Modifier.weight(1f))
        when {
            e.readOnly -> TrashMenu(state, e)
            e.writing -> {
                UndoRedo(e, light)
                Spacer(Modifier.width(10.dp))
                RoundButton(Icons.Outlined.MoreVert, stringResource(R.string.more), light = light) { onPanel(Panel.MENU) }
                Spacer(Modifier.width(10.dp))
                RoundButton(Icons.Outlined.Check, stringResource(R.string.done), light = light, onClick = { e.writing = false; onDone() })
            }
            else -> ReadingActions(light, platform::share, onPanel)
        }
    }

@Composable private fun TrashMenu(state: NotesState, e: Editing) = Box {
    var open by remember { mutableStateOf(false) }
    RoundButton(Icons.Outlined.MoreVert, stringResource(R.string.more)) { open = true }
    DropdownMenu(open, { open = false }) {
        DropdownMenuItem({ Text(stringResource(R.string.restore)) }, { open = false; state.restore(e.note) })
        DropdownMenuItem({ Text(stringResource(R.string.delete_forever), color = NotesColors.Danger) }, { open = false; state.deleteForever(e.note) })
    }
}

@Composable private fun Field(value: String, onChange: (String) -> Unit, hint: String, readOnly: Boolean, style: TextStyle, modifier: Modifier) =
    BasicTextField(
        value, onChange, modifier.fillMaxWidth().padding(vertical = 10.dp), readOnly = readOnly, textStyle = style,
        cursorBrush = SolidColor(NotesColors.Accent),
        decorationBox = { inner -> Box { if (value.isEmpty()) Text(hint, style = style.copy(color = style.color.copy(alpha = 0.45f))); inner() } },
    )
