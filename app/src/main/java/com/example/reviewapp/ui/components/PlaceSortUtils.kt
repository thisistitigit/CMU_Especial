package com.example.reviewapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.reviewapp.R
import com.example.reviewapp.data.models.Place
import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

/** Estratégias de ordenação para listas de locais. */
enum class PlaceSort { DEFAULT, GOOGLE_RATING, DISTANCE_ASC, GOOGLE_RATING_COUNT }

/** Estado serializável da ordenação de locais. */
data class PlaceSortState(val selected: PlaceSort = PlaceSort.GOOGLE_RATING)

/** Distância Haversine (m) entre dois pontos geográficos. */
private fun haversineMeters(a: LatLng, b: LatLng): Double {
    val R = 6371000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLng = Math.toRadians(b.longitude - a.longitude)
    val la1 = Math.toRadians(a.latitude)
    val la2 = Math.toRadians(b.latitude)
    val h = sin(dLat / 2).pow(2) + cos(la1) * cos(la2) * sin(dLng / 2).pow(2)
    return 2 * R * asin(min(1.0, sqrt(h)))
}

/**
 * Aplica a ordenação selecionada a uma lista de locais.
 *
 * @param state seleção atual.
 * @param center centro para cálculo de distância (obrigatório para `DISTANCE_ASC`).
 */
fun applyPlaceSort(
    places: List<Place>,
    state: PlaceSortState,
    center: LatLng?
): List<Place> = when (state.selected) {
    PlaceSort.DEFAULT,
    PlaceSort.GOOGLE_RATING -> places.sortedByDescending { it.avgRating }
    PlaceSort.GOOGLE_RATING_COUNT -> places.sortedByDescending { it.ratingsCount }
    PlaceSort.DISTANCE_ASC -> if (center == null) places
    else places.sortedBy { haversineMeters(center, LatLng(it.lat, it.lng)) }
}

/**
 * Botão "Filtro" com `DropdownMenu` de ordenação (Google rating, distância, etc.).
 *
 * Coloca-o no topo da lista, alinhado à direita.
 *
 * @param state estado de ordenação atual.
 * @param onChange callback com o novo [PlaceSortState].
 */
@Composable
fun PlaceSortButton(
    state: PlaceSortState,
    onChange: (PlaceSortState) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        FilledTonalButton(onClick = { open = true }) {
            Icon(Icons.Filled.FilterList, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.search_filter_button))
        }

        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.search_sort_header)) },
                enabled = false,
                onClick = {}
            )
            PlaceSortOptionRow(
                opt = PlaceSort.DEFAULT,
                label = stringResource(R.string.search_sort_default),
                selected = state.selected == PlaceSort.DEFAULT
            ) { onChange(PlaceSortState(it)); open = false }
            PlaceSortOptionRow(
                opt = PlaceSort.GOOGLE_RATING,
                label = stringResource(R.string.search_sort_google_rating),
                selected = state.selected == PlaceSort.GOOGLE_RATING
            ) { onChange(PlaceSortState(it)); open = false }
            PlaceSortOptionRow(
                opt = PlaceSort.DISTANCE_ASC,
                label = stringResource(R.string.search_sort_distance_asc),
                selected = state.selected == PlaceSort.DISTANCE_ASC
            ) { onChange(PlaceSortState(it)); open = false }
            PlaceSortOptionRow(
                opt = PlaceSort.GOOGLE_RATING_COUNT,
                label = stringResource(R.string.search_sort_rating_count),
                selected = state.selected == PlaceSort.GOOGLE_RATING_COUNT
            ) { onChange(PlaceSortState(it)); open = false }
        }
    }
}

@Composable
private fun PlaceSortOptionRow(
    opt: PlaceSort,
    label: String,
    selected: Boolean,
    onSelect: (PlaceSort) -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { RadioButton(selected = selected, onClick = null) },
        onClick = { onSelect(opt) }
    )
}
