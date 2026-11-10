package io.github.katzfuss11.tanzlexikon.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Ein gedecktes Beere/Pflaume als Akzent, passend zu den Entwürfen.
private val Light = lightColorScheme(
    primary = Color(0xFF7A2E5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF1E3EC),
    onPrimaryContainer = Color(0xFF3A1029),
    secondary = Color(0xFF2F5D8A),       // Leader
    secondaryContainer = Color(0xFFE3EDF7),
    tertiary = Color(0xFF8A4B2F),        // Follower
    tertiaryContainer = Color(0xFFF6E8E1),
    background = Color(0xFFF7F5F9),
    surface = Color(0xFFFFFFFF),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFE08BBD),
    onPrimary = Color(0xFF3A1029),
    primaryContainer = Color(0xFF3A2333),
    onPrimaryContainer = Color(0xFFF7D9EA),
    secondary = Color(0xFF8FB8E3),
    secondaryContainer = Color(0xFF1F2C3A),
    tertiary = Color(0xFFE3A68A),
    tertiaryContainer = Color(0xFF3A271F),
    background = Color(0xFF16131B),
    surface = Color(0xFF211D28),
)

/** Ob dunkel gezeichnet wird: nach Wahl in den Einstellungen oder wie am Handy eingestellt. */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun TanzlexikonTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
