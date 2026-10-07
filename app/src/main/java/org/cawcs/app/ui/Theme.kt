package org.cawcs.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF1F6F6B)
private val TealLight = Color(0xFF7FC4BE)
private val Sand = Color(0xFFF6F2EA)
private val Ink = Color(0xFF1C2524)
private val Coral = Color(0xFFC4573A)

private val Light = lightColorScheme(
    primary = Teal, onPrimary = Color.White,
    secondary = Color(0xFF5A6E6C), tertiary = Coral,
    background = Sand, surface = Color.White, onBackground = Ink, onSurface = Ink,
    surfaceVariant = Color(0xFFE9E4D9),
)
private val Dark = darkColorScheme(
    primary = TealLight, onPrimary = Color(0xFF00201E),
    secondary = Color(0xFFB1CBC8), tertiary = Color(0xFFFFB4A1),
    background = Color(0xFF111615), surface = Color(0xFF1A201F),
)

@Composable
fun CawcsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
