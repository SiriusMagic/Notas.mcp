package com.zirakusa.notas.ui.screens

import com.zirakusa.notas.IconCard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zirakusa.notas.BuildConfig
import com.zirakusa.notas.R
import com.zirakusa.notas.mcp.McpMode
import com.zirakusa.notas.mcp.RelayLink
import com.zirakusa.notas.ui.theme.Accent
import com.zirakusa.notas.ui.theme.NotesColors
import com.zirakusa.notas.ui.theme.ThemeMode
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** «Ajustes»: Tema (the whole app, apart from each note's «Fondo»), Modo MCP (the connector for your AI), privacy and version. */
@Composable fun SettingsScreen(mode: ThemeMode, accent: Accent, onMode: (ThemeMode) -> Unit, onAccent: (Accent) -> Unit, onMcp: () -> Unit, onBack: () -> Unit) {
    var privacy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(NotesColors.Background).systemBarsPadding().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), onClick = onBack)
            Text(stringResource(R.string.settings), Modifier.padding(start = 16.dp), fontSize = 26.sp)
        }
        Group {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.theme), fontSize = 18.sp)
                Text(stringResource(R.string.theme_hint), color = NotesColors.Muted, fontSize = 14.sp)
                Segmented(listOf(stringResource(R.string.theme_system), stringResource(R.string.theme_light), stringResource(R.string.theme_dark)),
                    mode.ordinal) { onMode(ThemeMode.entries[it]) }
                Text(stringResource(R.string.accent), fontSize = 16.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Accent.entries.forEach { a ->
                        val name = stringResource(accentNames.getValue(a))
                        Box(Modifier.size(40.dp).clip(CircleShape).background(a.color)
                            .border(3.dp, if (a == accent) NotesColors.Text else Color.Transparent, CircleShape)
                            .clickable(onClickLabel = name) { onAccent(a) }.semantics { contentDescription = name },
                            contentAlignment = Alignment.Center) { if (a == accent) Icon(Icons.Filled.Check, null, Modifier.size(20.dp), tint = a.ink) }
                    }
                }
            }
        }
        Group {
            val status = when {
                !McpMode.remote -> stringResource(R.string.mcp_status_off)
                McpMode.state == RelayLink.State.ONLINE -> stringResource(R.string.mcp_online)
                McpMode.state == RelayLink.State.CONNECTING -> stringResource(R.string.mcp_connecting)
                else -> stringResource(R.string.mcp_offline)
            }
            Item(stringResource(R.string.mcp_title), "$status · ${stringResource(R.string.mcp_settings_hint)}", onMcp)
        }
        IconCard(NotesColors.Card, NotesColors.Text, NotesColors.Muted, NotesColors.Accent)
        Group {
            Item(stringResource(R.string.privacy), null) { privacy = true }
            HorizontalDivider(color = NotesColors.Chip)
            Item(stringResource(R.string.about), stringResource(R.string.version, BuildConfig.VERSION_NAME), null)
        }
    }
    if (privacy) AlertDialog({ privacy = false }, confirmButton = { TextButton({ privacy = false }) { Text(stringResource(R.string.done)) } },
        title = { Text(stringResource(R.string.privacy)) }, text = { Text(stringResource(R.string.privacy_text)) })
}

@Composable private fun Group(content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(NotesColors.Card), content = content)

@Composable private fun Item(title: String, subtitle: String?, onClick: (() -> Unit)?) =
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 18.sp)
            if (subtitle != null) Text(subtitle, color = NotesColors.Muted, fontSize = 14.sp)
        }
        if (onClick != null) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = NotesColors.Muted)
    }

private val accentNames = mapOf(
    Accent.AMARILLO to R.string.accent_yellow, Accent.NARANJA to R.string.accent_orange, Accent.ROSA to R.string.accent_pink,
    Accent.MORADO to R.string.accent_purple, Accent.AZUL to R.string.accent_blue, Accent.VERDE to R.string.accent_green,
)
