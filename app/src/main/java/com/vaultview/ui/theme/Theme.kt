package com.vaultview.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VaultViewColors: ColorScheme = darkColorScheme(
    background = Color(0xFF080A0F),
    surface = Color(0xFF121722),
    surfaceVariant = Color(0xFF1A2230),
    primary = Color(0xFF72D2FF),
    secondary = Color(0xFFB7C7D9),
    onBackground = Color(0xFFE9EEF7),
    onSurface = Color(0xFFE9EEF7),
    onSurfaceVariant = Color(0xFFB7C7D9)
)

@Composable
fun VaultViewTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VaultViewColors,
        content = content
    )
}
