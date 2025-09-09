package com.example.reviewapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.reviewapp.data.models.Place

/**
 * Secção horizontal genérica de locais, com cartões elevados.
 *
 * @param title título apresentado à esquerda.
 * @param places lista de locais a renderizar.
 * @param onPlaceClick ação quando o cartão é selecionado (recebe `id`).
 */
@Composable
fun PlaceHorizontalSection(
    title: String,
    places: List<Place>,
    onPlaceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(places, key = { it.id }) { p ->
                PlaceCardElevated(place = p, onClick = { onPlaceClick(p.id) })
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Cartão compacto de local com nome, morada e **RatingRow**. */
@Composable
private fun PlaceCardElevated(place: Place, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .width(240.dp)
            .heightIn(min = 108.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = place.name.ifBlank { "—" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            place.address?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))
            RatingRow(
                rating = place.avgRating,
                count = place.ratingsCount.takeIf { it > 0 }
            )
        }
    }
}
