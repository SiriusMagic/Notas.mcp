package com.zirakusa.notas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.notas.R
import com.zirakusa.notas.ui.EditorPlatform
import com.zirakusa.notas.ui.theme.NotesColors
import com.zirakusa.notas.ui.theme.Skins
import com.zirakusa.notas.ui.theme.skin

/** Dark rounded panel at the bottom (format, camera, background, menu…); a tap above it closes it. */
@Composable fun BottomPanel(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val close = stringResource(R.string.close)
    val scrim = remember { MutableInteractionSource() }
    val sheet = remember { MutableInteractionSource() }
    Box(Modifier.fillMaxSize().background(Color(0x66000000)).clickable(scrim, null, onClick = onDismiss)
        .semantics { contentDescription = close },
        contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).background(NotesColors.Raised)
                .clickable(sheet, null) {}.navigationBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp), content = content,
        )
    }
}

/** A big rounded button with an icon over its name (camera panel, top of the ⋮ menu). */
@Composable fun RowScope.Tile(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) =
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(NotesColors.Chip).clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val tint = if (enabled) NotesColors.Text else NotesColors.Muted
        Icon(icon, null, tint = tint)
        Text(label, color = tint, fontSize = 15.sp, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
    }

/** 📷: take a photo, choose one, or read the text of a picture into the note. */
@Composable fun CameraPanel(platform: EditorPlatform, onDone: () -> Unit) = BottomPanel(onDone) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Tile(Icons.Outlined.PhotoCamera, stringResource(R.string.take_photo)) { onDone(); platform.takePhoto() }
        Tile(Icons.Outlined.Image, stringResource(R.string.pick_photo)) { onDone(); platform.pickPhoto() }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Tile(Icons.Outlined.DocumentScanner, stringResource(R.string.recognize_text)) { onDone(); platform.recognizeText() }
        Box(Modifier.weight(1f))
    }
}

/** 👕 «Fondo»: the backgrounds in a row, the chosen one framed in yellow. */
@Composable fun SkinPanel(selected: Int, onPick: (Int) -> Unit, onDone: () -> Unit) = BottomPanel(onDone) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        itemsIndexed(Skins.all) { i, skin ->
            Box(
                Modifier.width(72.dp).height(96.dp).clip(RoundedCornerShape(14.dp))
                    .border(3.dp, if (i == selected) NotesColors.Accent else Color.Transparent, RoundedCornerShape(14.dp))
                    .then(if (i == 0) Modifier.background(Color(0xFFF2F2F2)) else Modifier.skin(skin)).clickable { onPick(i) },
                contentAlignment = Alignment.Center,
            ) { if (i == 0) Icon(Icons.Outlined.Block, stringResource(R.string.no_background), Modifier.size(32.dp), tint = Color(0xFF999999)) }
        }
    }
}

/** ✦ «Notas de IA»: what it will do, once the AI connects through Modo MCP. */
@Composable fun AiPanel(onDone: () -> Unit) = BottomPanel(onDone) {
    Text(stringResource(R.string.ai_notes), fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    Text(stringResource(R.string.ai_soon), color = NotesColors.Muted, fontSize = 16.sp)
}
