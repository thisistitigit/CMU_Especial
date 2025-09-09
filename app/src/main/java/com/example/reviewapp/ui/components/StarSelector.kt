package com.example.reviewapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Seletor interativo de 1..5 estrelas com caracteres `"★/☆"`.
 *
 * @param selected valor atual.
 * @param onChange callback com o novo valor (1..5).
 */
@Composable
fun StarSelector(
    selected: Int,
    onChange: (Int) -> Unit
) {
    Row(Modifier.padding(vertical = 8.dp)) {
        (1..5).forEach { i ->
            val symbol = if (i <= selected) "★" else "☆"
            Text(
                text = symbol,
                fontSize = 28.sp,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clickable { onChange(i) }
            )
        }
    }
}
