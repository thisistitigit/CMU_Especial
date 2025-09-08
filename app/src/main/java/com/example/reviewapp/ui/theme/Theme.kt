package com.example.reviewapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary              = BrandLilacLight,
    onPrimary            = Color.Black,
    primaryContainer     = BrandLilacDeep,
    onPrimaryContainer   = Color.White,

    secondary            = BrandMagenta,
    onSecondary          = Color.White,
    secondaryContainer   = BrandMagenta.copy(alpha = .20f),
    onSecondaryContainer = BrandMagenta,

    background           = BrandCream,
    onBackground         = BrandBlack,
    surface              = NeutralSurfaceLight,
    onSurface            = NeutralOnLight,
    surfaceVariant       = Color(0xFFE7E0EC),
    onSurfaceVariant     = Color(0xFF49454F),

    outline              = OutlineVariantLight,
    error                = DangerRed,
    onError              = Color.White
)

private val DarkColors = darkColorScheme(
    primary              = BrandLilacDeep,
    onPrimary            = Color.White,
    primaryContainer     = BrandLilacDeep,
    onPrimaryContainer   = Color.White,

    secondary            = BrandMagenta,
    onSecondary          = Color.Black,
    secondaryContainer   = BrandMagenta.copy(alpha = .25f),
    onSecondaryContainer = Color.Black,

    background           = BrandBlack,
    onBackground         = NeutralOnDark,
    surface              = NeutralSurfaceDark,
    onSurface            = NeutralOnDark,
    surfaceVariant       = Color(0xFF49454F),
    onSurfaceVariant     = Color(0xFFCAC4D0),

    outline              = OutlineVariantDark,
    error                = DangerRed,
    onError              = Color.Black
)

data class AppExtraColors(
    val navContainer: Color,
    val navShadow: Color,
    val navSelectedBg: Color,
    val navSelectedIcon: Color,
    val logout: Color
)

private val LocalAppColors = staticCompositionLocalOf {
    AppExtraColors(
        navContainer   = NeutralSurfaceLight,
        navShadow      = BrandLilacDeep.copy(alpha = .35f),
        navSelectedBg  = BrandLilacLight,
        navSelectedIcon= BrandBlack,
        logout         = DangerRed
    )
}

object AppTheme {
    val colors: AppExtraColors
        @Composable get() = LocalAppColors.current
}

@Composable
fun ReviewAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val material =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        } else {
            if (darkTheme) DarkColors else LightColors
        }

    val appExtras =
        if (darkTheme) {
            AppExtraColors(
                navContainer    = BrandBlack,
                navShadow       = BrandLilacDeep.copy(alpha = .55f),
                navSelectedBg   = BrandLilacLight.copy(alpha = .95f),
                navSelectedIcon = BrandBlack,
                logout          = DangerRed
            )
        } else {
            AppExtraColors(
                navContainer    = NeutralSurfaceLight,
                navShadow       = BrandLilacDeep.copy(alpha = .35f),
                navSelectedBg   = BrandLilacLight,
                navSelectedIcon = BrandBlack,
                logout          = DangerRed
            )
        }

    MaterialTheme(colorScheme = material, typography = Typography) {
        CompositionLocalProvider(LocalAppColors provides appExtras) {
            content()
        }
    }
}
