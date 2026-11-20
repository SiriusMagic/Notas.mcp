package com.zirakusa.notas.ui.screens

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
import com.zirakusa.notas.ui.theme.NotesColors

/** «Ajustes»: Modo MCP (the connector for your AI), privacy and version. */
@Composable fun SettingsScreen(onMcp: () -> Unit, onBack: () -> Unit) {
    var privacy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(NotesColors.Background).systemBarsPadding().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), onClick = onBack)
            Text(stringResource(R.string.settings), Modifier.padding(start = 16.dp), fontSize = 26.sp)
        }
        Group {
            val status = when {
                !McpMode.active -> stringResource(R.string.mcp_status_off)
                McpMode.hubRedirect -> stringResource(R.string.mcp_redirect)
                McpMode.state == RelayLink.State.ONLINE -> stringResource(R.string.mcp_online)
                McpMode.state == RelayLink.State.CONNECTING -> stringResource(R.string.mcp_connecting)
                else -> stringResource(R.string.mcp_offline)
            }
            Item(stringResource(R.string.mcp_title), "$status · ${stringResource(R.string.mcp_settings_hint)}", onMcp)
        }
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
