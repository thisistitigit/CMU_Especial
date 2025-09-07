package com.example.reviewapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    // Brand primária/secondary
    primary              = BrandMagenta,
    onPrimary            = Color.White,
    primaryContainer     = MagentaContainerLight,
    onPrimaryContainer   = OnMagentaContainer,

    secondary            = BrandLilac,
    onSecondary          = Color.White,
    secondaryContainer   = LilacContainerLight,
    onSecondaryContainer = OnLilacContainer,

    // Fundo e superfícies
    background           = BrandCream,          // cream/beige como fundo global
    onBackground         = BrandBlack,
    surface              = NeutralSurfaceLight, // cartões, barras, etc.
    onSurface            = NeutralOnLight,
    surfaceVariant       = Color(0xFFE7E0EC),
    onSurfaceVariant     = Color(0xFF49454F),

    // Outros
    outline              = OutlineVariantLight,
    error                = Color(0xFFB00020),
    onError              = Color.White
)

private val DarkColors = darkColorScheme(
    primary              = BrandLilac,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFF5E1136),
    onPrimaryContainer   = Color(0xFFFFD9E2),

    secondary            = BrandLilac,
    onSecondary          = Color.Black,
    secondaryContainer   = Color(0xFF3B1E6D),
    onSecondaryContainer = Color(0xFFEADDFF),

    background           = BrandBlack,
    onBackground         = NeutralOnDark,
    surface              = NeutralSurfaceDark,
    onSurface            = NeutralOnDark,
    surfaceVariant       = Color(0xFF49454F),
    onSurfaceVariant     = Color(0xFFCAC4D0),

    outline              = OutlineVariantDark,
    error                = Color(0xFFFF6B6B),
    onError              = Color.Black
)

/**
 * Tema ÚNICO da app. Mantém a identidade (magenta/lilás/preto/cream).
 * Por defeito desligo dynamicColor para não “quebrar” a tua paleta.
 */
@Composable
fun ReviewAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // <- mantém a paleta da marca por defeito
    content: @Composable () -> Unit
) {
    val scheme =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        } else {
            if (darkTheme) DarkColors else LightColors
        }

    MaterialTheme(
        colorScheme = scheme,
        typography  = Typography,   // podes manter o teu Typography.kt
        content     = content
    )
}
