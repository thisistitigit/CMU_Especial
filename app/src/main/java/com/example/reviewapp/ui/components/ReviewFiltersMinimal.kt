package com.example.reviewapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.reviewapp.R

/**
 * UI mínima para controlo de **ordenação** e **filtros** de reviews.
 *
 * Dois ícones (Sort/Filter) que abrem `DropdownMenu`s com:
 * - ordenação (mais antigas/mais recentes);
 * - filtros (com foto, só minhas, mínimo de estrelas).
 *
 * @param state estado atual.
 * @param onChange chamado com o novo estado após qualquer alteração.
 */
@Composable
fun ReviewFiltersMinimal(
    state: ReviewFilterState,
    onChange: (ReviewFilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortOpen by remember { mutableStateOf(false) }
    var filterOpen by remember { mutableStateOf(false) }

    Row(modifier = modifier) {

        Box {
            IconButton(onClick = { sortOpen = true }) {
                Icon(Icons.Filled.Sort, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reviews_sort_oldest_first)) },
                    leadingIcon = { RadioButton(selected = state.sort == ReviewSort.OLDEST_FIRST, onClick = null) },
                    onClick = { onChange(state.copy(sort = ReviewSort.OLDEST_FIRST)); sortOpen = false }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reviews_sort_newest_first)) },
                    leadingIcon = { RadioButton(selected = state.sort == ReviewSort.NEWEST_FIRST, onClick = null) },
                    onClick = { onChange(state.copy(sort = ReviewSort.NEWEST_FIRST)); sortOpen = false }
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        Box {
            IconButton(onClick = { filterOpen = true }) {
                Icon(Icons.Filled.FilterAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            DropdownMenu(expanded = filterOpen, onDismissRequest = { filterOpen = false }) {

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reviews_filter_with_photo)) },
                    leadingIcon = { Checkbox(checked = state.withPhotoOnly, onCheckedChange = null) },
                    onClick = { onChange(state.copy(withPhotoOnly = !state.withPhotoOnly)) }
                )

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reviews_filter_only_mine)) },
                    leadingIcon = { Checkbox(checked = state.onlyMine, onCheckedChange = null) },
                    onClick = { onChange(state.copy(onlyMine = !state.onlyMine)) }
                )

                Divider()

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reviews_filter_min_stars)) },
                    enabled = false,
                    onClick = {}
                )

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reviews_filter_any_stars)) },
                    leadingIcon = { RadioButton(selected = state.minStars == null, onClick = null) },
                    onClick = { onChange(state.copy(minStars = null)) }
                )

                for (s in 5 downTo 1) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.reviews_filter_min_stars_value, s)) },
                        leadingIcon = { RadioButton(selected = state.minStars == s, onClick = null) },
                        onClick = { onChange(state.copy(minStars = s)) }
                    )
                }
            }
        }
    }
}
