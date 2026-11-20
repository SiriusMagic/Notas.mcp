package com.zirakusa.notas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Ajustes › Tema: follow the phone, always light or always dark. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Ajustes › Color de acento. [ink] is the text and icons drawn on the accent. */
enum class Accent(val color: Color, val ink: Color) {
    AMARILLO(Color(0xFFF5C518), Color(0xFF111111)),
    NARANJA(Color(0xFFFF8A3D), Color(0xFF111111)),
    ROSA(Color(0xFFF06292), Color(0xFF111111)),
    MORADO(Color(0xFFA78BFA), Color(0xFF111111)),
    AZUL(Color(0xFF4FA3F7), Color(0xFF111111)),
    VERDE(Color(0xFF4CC38A), Color(0xFF111111)),
}

/** The app's colours for one mode (the notes' own backgrounds, «Fondo», are separate: see Skins). */
data class Palette(val background: Color, val card: Color, val raised: Color, val chip: Color, val text: Color, val muted: Color, val light: Boolean)

private val Dark = Palette(Color(0xFF000000), Color(0xFF1C1C1E), Color(0xFF2A2A2C), Color(0xFF2C2C2E), Color(0xFFF2F2F2), Color(0xFF9A9A9E), light = false)
private val Light = Palette(Color(0xFFF5F5F7), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFE9E9EC), Color(0xFF151515), Color(0xFF7C7C80), light = true)

/**
 * The global theme. Every screen reads its colours here; they follow Ajustes › Tema and Color de acento (state, so a
 * change repaints at once). Default: dark with Stone's yellow, like the phone's own notes.
 */
object NotesColors {
    var palette by mutableStateOf(Dark); private set
    var chosen: com.zirakusa.notas.ui.theme.Accent by mutableStateOf(com.zirakusa.notas.ui.theme.Accent.AMARILLO); private set

    val Background get() = palette.background
    val Card get() = palette.card
    val Raised get() = palette.raised
    val Chip get() = palette.chip
    val Text get() = palette.text
    val Muted get() = palette.muted
    val Accent: Color get() = chosen.color
    val OnAccent: Color get() = chosen.ink
    val Danger = Color(0xFFFF6B6B)
    val isLight get() = palette.light

    /** Changes only what differs, so painting with the same theme never asks for another frame. */
    fun use(dark: Boolean, accent: com.zirakusa.notas.ui.theme.Accent) {
        val next = if (dark) Dark else Light
        if (palette != next) palette = next
        if (chosen != accent) chosen = accent
    }
}

@Composable fun NotesTheme(mode: ThemeMode = ThemeMode.DARK, accent: Accent = Accent.AMARILLO, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    NotesColors.use(dark, accent)
    val c = NotesColors
    val scheme = if (dark) darkColorScheme(primary = c.Accent, onPrimary = c.OnAccent, background = c.Background, surface = c.Raised,
        surfaceContainer = c.Raised, surfaceContainerHigh = c.Raised, onBackground = c.Text, onSurface = c.Text, onSurfaceVariant = c.Muted, error = c.Danger)
    else lightColorScheme(primary = c.Accent, onPrimary = c.OnAccent, background = c.Background, surface = c.Raised,
        surfaceContainer = c.Raised, surfaceContainerHigh = c.Raised, onBackground = c.Text, onSurface = c.Text, onSurfaceVariant = c.Muted, error = c.Danger)
    MaterialTheme(scheme) { CompositionLocalProvider(LocalContentColor provides c.Text, content = content) }
}
