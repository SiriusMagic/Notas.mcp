package com.zirakusa.notas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zirakusa.notas.R
import com.zirakusa.notas.ui.theme.NotesColors

/**
 * Rounded card with a title, one line of text and «Cancelar» / «Hecho» pills (new task, new folder).
 * Full width on purpose: the platform's default-width dialog with a text field never settles in the tests.
 */
@Composable fun InputDialog(
    title: String, initial: String, max: Int, onCancel: () -> Unit, onDone: (String) -> Unit, onDelete: (() -> Unit)? = null,
    extra: (@Composable () -> Unit)? = null,
) = Dialog(onCancel, DialogProperties(usePlatformDefaultWidth = false)) {
    var text by remember(initial) { mutableStateOf(initial) }
    Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(NotesColors.Raised).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))
        BasicTextField(text, { text = it.replace("\n", "").take(max) },
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NotesColors.Chip).padding(16.dp),
            textStyle = TextStyle(color = NotesColors.Text, fontSize = 18.sp), cursorBrush = SolidColor(NotesColors.Accent))
        extra?.invoke()
        if (onDelete != null) TextButton(onDelete, Modifier.align(Alignment.Start)) { Text(stringResource(R.string.delete), color = NotesColors.Danger) }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill(stringResource(R.string.cancel), NotesColors.Chip, NotesColors.Text, Modifier.weight(1f), onCancel)
            Pill(stringResource(R.string.done), NotesColors.Accent, NotesColors.Background, Modifier.weight(1f)) { onDone(text) }
        }
    }
}

@Composable private fun Pill(label: String, color: Color, textColor: Color, modifier: Modifier, onClick: () -> Unit) =
    Box(modifier.height(52.dp).clip(CircleShape).background(color).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
