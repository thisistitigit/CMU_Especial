package com.example.cmu_especial.ui.shared

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * RatingBar minimalista: imprime ★ repetido (1..5).
 * Podes trocar por um componente mais elaborado depois.
 */
@Composable
fun RatingBar(rating: Int) {
    val clamp = rating.coerceIn(1, 5)
    Text("★".repeat(clamp) + "☆".repeat(5 - clamp))
}
