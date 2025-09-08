package com.example.reviewapp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.round

@Composable
fun RatingRow(
    rating: Double,
    count: Int? = null,
    modifier: Modifier = Modifier,
    showSlash5: Boolean = true,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconSize: Dp = 18.dp,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    countStyle: TextStyle = MaterialTheme.typography.labelSmall,
    countPrefix: String = "• "
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.width(iconSize)
        )
        Spacer(Modifier.width(6.dp))
        val rounded = round(rating * 10) / 10.0
        Text(
            text = if (showSlash5) "$rounded / 5" else "$rounded",
            style = textStyle
        )
        count?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$countPrefix$it",
                style = countStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
