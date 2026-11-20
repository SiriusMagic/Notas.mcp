package com.zirakusa.notas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.zirakusa.notas.R
import com.zirakusa.notas.data.Attachment
import com.zirakusa.notas.ui.theme.NotesColors
import java.io.File

/** The note's photos and drawings under the text, full width; a tap opens one. */
@Composable fun AttachmentList(items: List<Attachment>, dir: File, onOpen: (Attachment) -> Unit) =
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        items.forEach { a ->
            AsyncImage(File(dir, a.file), null, Modifier.fillMaxWidth().heightIn(max = 420.dp).clip(RoundedCornerShape(14.dp)).clickable { onOpen(a) },
                contentScale = ContentScale.Fit)
        }
    }

/** One photo or drawing over everything, with close and delete. */
@Composable fun AttachmentViewer(a: Attachment, dir: File, canDelete: Boolean, onDelete: () -> Unit, onClose: () -> Unit) =
    Dialog(onClose, DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(NotesColors.Background).systemBarsPadding()) {
            AsyncImage(File(dir, a.file), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            RoundButton(Icons.Outlined.Close, stringResource(R.string.back), Modifier.align(Alignment.TopStart).padding(12.dp), onClick = onClose)
            if (canDelete) RoundButton(Icons.Outlined.Delete, stringResource(R.string.delete), Modifier.align(Alignment.TopEnd).padding(12.dp), onClick = onDelete)
        }
    }
