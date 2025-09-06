package com.example.reviewapp.ui.components
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.reviewapp.R
import com.example.reviewapp.data.models.Place
import kotlin.math.round

/**
 * Secção horizontal genérica para apresentar uma lista de lugares em cartões pequenos.
 * Usa o mesmo visual para "Sugestões" e para "Resultados da pesquisa".
 */
@Composable
fun PlaceHorizontalSection(
    title: String,
    places: List<Place>,
    onPlaceClick: (String) -> Unit,

    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.width(8.dp))
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(places) { p ->
                SuggestionCard(place = p, onClick = { onPlaceClick(p.id) })
            }
        }
    }
}

/** Cartão compacto com nome e rating. */
@Composable
fun SuggestionCard(place: Place, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .widthIn(min = 200.dp)
            .wrapContentHeight()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                place.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(ratingToText(place.avgRating), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun ratingToText(value: Double): String {
    val rounded = round(value * 10) / 10.0 // 1 casa decimal (ex.: 4.3/5)
    return "$rounded / 5"
}
