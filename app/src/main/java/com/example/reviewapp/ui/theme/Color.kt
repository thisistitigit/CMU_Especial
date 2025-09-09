package com.example.reviewapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ## Paleta cromática da aplicação
 *
 * Define as cores de marca e os neutros base utilizados na composição do _color scheme_
 * Material 3. As cores são declaradas como constantes `val` imutáveis (sem alocação dinâmica)
 * para minimizar o custo em recomposições Compose.
 *
 * Convenções:
 * - Sufixos `Light`/`Deep` indicam variações de saturação/valor dentro da mesma cor de marca.
 * - O prefixo `Neutral` identifica cores de superfícies e respetivo contraste de texto.
 * - As cores aqui definidas **não** executam _tone mapping_; esse mapeamento é feito
 *   posteriormente ao construir os esquemas claro/escuro de Material 3.
 */
val BrandMagenta      = Color(0xFFE91E63) // Cor de acento secundária (actions, realces).
val BrandLilacLight   = Color(0xFFE3A4F7) // Primária clara (modo claro: primária).
val BrandLilacDeep    = Color(0xFFCB6CE6) // Primária saturada (modo escuro e containers).
val BrandBlack        = Color(0xFF000000) // Fundo/texto alto contraste em dark.
val BrandCream        = Color(0xFFF6EFE6) // Fundo quente em light (paper-like).

val NeutralSurfaceLight = Color(0xFFFFFFFF) // Superfície base no modo claro.
val NeutralSurfaceDark  = Color(0xFF121212) // Superfície base no modo escuro.
val NeutralOnLight      = Color(0xFF141414) // Texto sobre superfícies claras.
val NeutralOnDark       = Color(0xFFEDEDED) // Texto sobre superfícies escuras.
val OutlineVariantLight = Color(0xFF948F99) // Divisores/contornos em light.
val OutlineVariantDark  = Color(0xFF8E8894) // Divisores/contornos em dark.

val DangerRed = Color(0xFFE53935) // Semântica de erro/estado crítico.
