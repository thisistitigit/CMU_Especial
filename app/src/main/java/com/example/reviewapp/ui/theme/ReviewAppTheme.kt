package com.example.reviewapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GreenPrimary = Color(0xFF2BB24C)  // “TheFork-like”
private val GreenSecondary = Color(0xFF34C759)

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    secondary = GreenSecondary,
    onSecondary = Color.White,
    surface = Color(0xFFF8F8F8),
    onSurface = Color(0xFF1E1E1E),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1E1E1E),
    error = Color(0xFFB00020)
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.Black,
    secondary = GreenSecondary,
    onSecondary = Color.Black,
    surface = Color(0xFF121212),
    onSurface = Color(0xFFEFEFEF),
    background = Color(0xFF0B0B0B),
    onBackground = Color(0xFFEFEFEF),
    error = Color(0xFFFF6B6B)
)

@Composable
fun TheForkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
