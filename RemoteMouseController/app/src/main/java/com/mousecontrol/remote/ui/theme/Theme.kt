package com.mousecontrol.remote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7C9DFF),
    secondary = Color(0xFF03DAC6),
    background = Color(0xFF0A0A0F),
    surface = Color(0xFF15151C),
    error = Color(0xFFFF6B6B),
    onPrimary = Color.Black,
    onBackground = Color(0xFFE6E6EA),
    onSurface = Color(0xFFE6E6EA),
)

/** Always applies the dark scheme, regardless of system theme, per spec. */
@Composable
fun RemoteMouseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
