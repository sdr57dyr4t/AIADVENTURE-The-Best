package com.metalfish.aiadventure.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFE6D38C),
    onPrimary = Color(0xFF1A1A1A),
    surface = Color(0xFF0B0C12),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF14162A),
    onSurfaceVariant = Color(0xFFCDD1E0),
    scrim = Color(0xFF000000)
)

@Composable
fun AIAdventureTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
