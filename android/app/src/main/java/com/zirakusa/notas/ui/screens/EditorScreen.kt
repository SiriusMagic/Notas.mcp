package com.zirakusa.notas.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.notas.R
import com.zirakusa.notas.data.Folder
import com.zirakusa.notas.ui.Editing
import com.zirakusa.notas.ui.theme.NotesColors

/** Writing a note: back (saves), undo/redo, menu, title and text. A note in the trash is only read. */
@Composable fun EditorScreen(
    e: Editing, folders: List<Folder>, info: String, onBack: () -> Unit,
    onMove: (Long?) -> Unit, onTrash: () -> Unit, onRestore: () -> Unit, onDeleteForever: () -> Unit,
) = Column(Modifier.fillMaxSize().background(NotesColors.Background).systemBarsPadding().imePadding().padding(horizontal = 20.dp)) {
    var menu by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        RoundButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), onClick = onBack)
        Spacer(Modifier.weight(1f))
        if (!e.readOnly) {
            RoundButton(Icons.AutoMirrored.Outlined.Undo, stringResource(R.string.undo), enabled = e.history.canUndo, onClick = e::undo)
            Spacer(Modifier.width(4.dp))
            RoundButton(Icons.AutoMirrored.Outlined.Redo, stringResource(R.string.redo), enabled = e.history.canRedo, onClick = e::redo)
            Spacer(Modifier.width(12.dp))
        }
        Box {
            RoundButton(Icons.Outlined.MoreVert, stringResource(R.string.more)) { menu = true }
            DropdownMenu(menu, { menu = false }) {
                if (e.readOnly) {
                    DropdownMenuItem({ Text(stringResource(R.string.restore)) }, { menu = false; onRestore() })
                    DropdownMenuItem({ Text(stringResource(R.string.delete_forever), color = NotesColors.Danger) }, { menu = false; onDeleteForever() })
                } else {
                    DropdownMenuItem({ Text(stringResource(R.string.move_to)) }, { menu = false; moving = true })
                    DropdownMenuItem({ Text(stringResource(R.string.delete), color = NotesColors.Danger) }, { menu = false; onTrash() })
                }
            }
            DropdownMenu(moving, { moving = false }) {
                DropdownMenuItem({ Text(stringResource(R.string.uncategorized)) }, { moving = false; onMove(null) })
                folders.forEach { f -> DropdownMenuItem({ Text(f.name) }, { moving = false; onMove(f.id) }) }
            }
        }
    }
    Text(info, color = NotesColors.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Field(e.draft.title, { e.change(e.draft.copy(title = it.replace("\n", ""))) }, stringResource(R.string.title_hint), e.readOnly,
            TextStyle(color = NotesColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold))
        Field(e.draft.body, { e.change(e.draft.copy(body = it)) }, stringResource(R.string.body_hint), e.readOnly,
            TextStyle(color = NotesColors.Text, fontSize = 18.sp, lineHeight = 26.sp))
    }
}

@Composable private fun Field(value: String, onChange: (String) -> Unit, hint: String, readOnly: Boolean, style: TextStyle) =
    BasicTextField(
        value, onChange, Modifier.fillMaxWidth().padding(vertical = 10.dp), readOnly = readOnly, textStyle = style,
        cursorBrush = SolidColor(NotesColors.Accent),
        decorationBox = { inner -> Box { if (value.isEmpty()) Text(hint, style = style.copy(color = NotesColors.Muted)); inner() } },
    )
