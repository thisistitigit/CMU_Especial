package com.example.reviewapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Paleta ReviewApp (inspirada no TheFork)
val ReviewGreen        = Color(0xFF2ECC71)
val ReviewGreenDark    = Color(0xFF27AE60)
val ReviewGreenLight   = Color(0xFF6EE7B7)

val ReviewSurfaceLight = Color(0xFFFFFFFF)
val ReviewSurfaceDark  = Color(0xFF232323)

val ReviewBgLight      = Color(0xFFF8FAF9)
val ReviewBgDark       = Color(0xFF2F2F2F)

val ReviewOnDark       = Color(0xFFEDEDED)
val ReviewOnLight      = Color(0xFF141414)

val ReviewAccent       = Color(0xFF66BB6A)
private val LightColorScheme = lightColorScheme(
    primary = ReviewGreen,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = ReviewGreenLight,
    onPrimaryContainer = ReviewOnLight,
    secondary = ReviewAccent,
    surface = ReviewSurfaceLight,
    onSurface = ReviewOnLight,
    background = ReviewBgLight,
    onBackground = ReviewOnLight
)

private val DarkColorScheme = darkColorScheme(
    primary = ReviewGreen,
    onPrimary = androidx.compose.ui.graphics.Color.Black,
    primaryContainer = ReviewGreenDark,
    onPrimaryContainer = ReviewOnDark,
    secondary = ReviewAccent,
    surface = ReviewSurfaceDark,
    onSurface = ReviewOnDark,
    background = ReviewBgDark,
    onBackground = ReviewOnDark
)

@Composable
fun ReviewAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
