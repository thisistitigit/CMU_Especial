package com.example.reviewapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * ## Sistema tipográfico
 *
 * Conjunto base de estilos Material 3 com ajustes mínimos para hierarquia e legibilidade:
 * - `titleLarge`: realce de cabeçalhos da aplicação (ex.: _TopAppBar_).
 * - `bodyLarge`: corpo de texto predominante.
 * - `labelSmall`: metadados/auxiliares (chips, _assist_ labels).
 *
 * Os restantes estilos herdados de [Typography] mantêm-se por omissão (Material defaults).
 */
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
