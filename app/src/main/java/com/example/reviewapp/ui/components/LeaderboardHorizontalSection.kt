package com.example.reviewapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.reviewapp.R
import com.example.reviewapp.viewmodels.LeaderboardViewModel
import kotlin.math.round

@Composable
fun LeaderboardHorizontalSection(
    title: String,
    rows: List<LeaderboardViewModel.PlaceRow>,
    onPlaceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(top = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(rows, key = { it.placeId }) { row ->
                LeaderboardPlaceCard(row = row, onClick = { onPlaceClick(row.placeId) })
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LeaderboardPlaceCard(
    row: LeaderboardViewModel.PlaceRow,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .width(240.dp)
            .heightIn(min = 96.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = row.name.ifBlank { "—" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2
            )
            row.address?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(8.dp))

            // ⭐ + “X.X / 5” + contagem — igual ao Place
            RatingRowSimple(rating = row.avg, count = row.count)
        }
    }
}

/** Linha de rating idêntica à usada no PlaceHorizontalSection. */
@Composable
private fun RatingRowSimple(
    rating: Double,
    count: Int?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Icon(Icons.Filled.Star, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(text = ratingToText(rating), style = MaterialTheme.typography.bodyMedium)
        count?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.leaderboard_rating_count_inline, it),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun ratingToText(value: Double): String {
    val rounded = round(value * 10) / 10.0
    return "$rounded / 5"
}
