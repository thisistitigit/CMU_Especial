package com.example.reviewapp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Mostra 0..5 "★/☆" com base num Double (ex.: 4.3).
 * Arredonda ao inteiro mais próximo para desenho simples.
 */
@Composable
fun StarRating(rating: Double, modifier: Modifier = Modifier) {
    val filled = rating.coerceIn(0.0, 5.0).roundToInt()
    Row(modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        repeat(5) { i ->
            val symbol = if (i < filled) "★" else "☆"
            Text(
                text = symbol,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "  ${"%.1f".format(rating)}",
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
