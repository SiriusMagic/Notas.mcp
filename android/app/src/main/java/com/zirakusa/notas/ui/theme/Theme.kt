package com.zirakusa.notas.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/** Black background, dark grey cards and Stone's yellow, like the phone's own notes. */
object NotesColors {
    val Background = Color(0xFF000000)
    val Card = Color(0xFF1C1C1E)
    val Raised = Color(0xFF2A2A2C)
    val Chip = Color(0xFF2C2C2E)
    val Accent = Color(0xFFF5C518)
    val Text = Color(0xFFF2F2F2)
    val Muted = Color(0xFF9A9A9E)
    val Danger = Color(0xFFFF6B6B)
}

@Composable fun NotesTheme(content: @Composable () -> Unit) = MaterialTheme(
    darkColorScheme(
        primary = NotesColors.Accent, onPrimary = Color.Black, background = NotesColors.Background,
        surface = NotesColors.Raised, surfaceContainer = NotesColors.Raised, onBackground = NotesColors.Text,
        onSurface = NotesColors.Text, onSurfaceVariant = NotesColors.Muted, error = NotesColors.Danger,
    )
) { CompositionLocalProvider(LocalContentColor provides NotesColors.Text, content = content) }
