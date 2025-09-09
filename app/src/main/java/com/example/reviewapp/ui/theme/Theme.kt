package com.example.reviewapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * ## Esquema Material 3 — Light
 *
 * Constrói o _color scheme_ para tema claro a partir da paleta definida em [Color].
 * Notas:
 * - `primary`/`primaryContainer` assumem os lilases de marca para reforçar identidade.
 * - `surface`/`onSurface` partem de neutros para manter legibilidade em superfícies extensas.
 * - _Error pipeline_ alinhado com semântica de Material 3 via [DangerRed].
 */
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

/**
 * ## Esquema Material 3 — Dark
 *
 * Configuração equivalente ao modo claro, mas:
 * - `background` e `surface` escuros para reduzir luminância global.
 * - `on*` em tons claros para contraste AA/AAA.
 * - Containers preservam _brand tint_ para continuidade.
 */
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

/**
 * ## Extensões cromáticas fora do Material 3
 *
 * Estrutura com cores utilitárias de UI que não pertencem diretamente ao _color scheme_
 * de Material (ex.: _bottom navigation_ com _chip_ selecionado).
 *
 * @property navContainer Cor de fundo da barra de navegação inferior.
 * @property navShadow    Sombra/elevação projetada da barra (quando aplicável).
 * @property navSelectedBg Fundo do botão/ícone selecionado na barra.
 * @property navSelectedIcon Cor do ícone selecionado.
 * @property logout       Cor semântica para ações de saída (sugerida em botões/ícones).
 */
data class AppExtraColors(
    val navContainer: Color,
    val navShadow: Color,
    val navSelectedBg: Color,
    val navSelectedIcon: Color,
    val logout: Color
)

/**
 * `CompositionLocal` para injetar [AppExtraColors] na árvore Compose sem parametrização explícita.
 *
 * Mantém um conjunto _fallback_ para cenários de _preview_ e testes.
 */
private val LocalAppColors = staticCompositionLocalOf {
    AppExtraColors(
        navContainer    = NeutralSurfaceLight,
        navShadow       = BrandLilacDeep.copy(alpha = .35f),
        navSelectedBg   = BrandLilacLight,
        navSelectedIcon = BrandBlack,
        logout          = DangerRed
    )
}

/**
 * Acesso tipado às cores extra do tema da aplicação.
 *
 * Exemplo:
 * ```
 * Surface(color = AppTheme.colors.navContainer) { ... }
 * ```
 */
object AppTheme {
    val colors: AppExtraColors
        @Composable get() = LocalAppColors.current
}

/**
 * ## Tema raiz da aplicação
 *
 * Aplica o _color scheme_ Material 3 (light/dark ou dinâmico) e fornece [AppExtraColors]
 * por `CompositionLocal`, além da tipografia definida em [Typography].
 *
 * @param darkTheme `true` para forçar modo escuro; por omissão segue o sistema.
 * @param dynamicColor ativa Material You (Monet) em Android 12+ (S), usando o extrator de cor
 *                     do sistema. Em versões anteriores, recorre à paleta estática.
 * @param content Conteúdo composto a ser tematizado.
 *
 * ### Boas práticas
 * - Evitar capturar [MaterialTheme.colorScheme] fora de composição.
 * - Usar [AppTheme.colors] para padrões específicos da marca que não existam no esquema Material.
 */
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

    // Definições extra (fora do material) ajustadas por tema.
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
