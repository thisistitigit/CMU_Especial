// com/example/reviewapp/ui/components/ReviewFilters.kt
package com.example.reviewapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.reviewapp.R
import com.example.reviewapp.data.models.Review

enum class ReviewSort { OLDEST_FIRST, NEWEST_FIRST }

data class ReviewFilterState(
    val sort: ReviewSort = ReviewSort.OLDEST_FIRST,
    val withPhotoOnly: Boolean = false,
    val onlyMine: Boolean = false,
    val minStars: Int? = null
)

fun applyReviewFilters(
    all: List<Review>,
    f: ReviewFilterState,
    currentUserId: String?
): List<Review> {
    var seq = all.asSequence()
    if (f.withPhotoOnly) {
        seq = seq.filter { (it.photoCloudUrl?.isNotBlank() == true) || (it.photoLocalPath?.isNotBlank() == true) }
    }
    if (f.onlyMine && !currentUserId.isNullOrBlank()) {
        seq = seq.filter { it.userId == currentUserId }
    }
    f.minStars?.let { min -> seq = seq.filter { it.stars >= min } }

    seq = when (f.sort) {
        ReviewSort.OLDEST_FIRST -> seq.sortedBy { it.createdAt }
        ReviewSort.NEWEST_FIRST -> seq.sortedByDescending { it.createdAt }
    }
    return seq.toList()
}

@Composable
fun ReviewFilterBar(
    state: ReviewFilterState,
    onChange: (ReviewFilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMenu by remember { mutableStateOf(false) }
    var filterMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- Botão ORDENAR (dropdown com 2 opções) ---
        Box {
            FilledTonalButton(
                onClick = { sortMenu = true },
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Icon(Icons.Filled.Sort, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (state.sort == ReviewSort.OLDEST_FIRST)
                        stringResource(R.string.reviews_sort_oldest_first)
                    else stringResource(R.string.reviews_sort_newest_first)
                )
            }
            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reviews_sort_oldest_first)) },
                    leadingIcon = {
                        RadioButton(
                            selected = state.sort == ReviewSort.OLDEST_FIRST,
                            onClick = null
                        )
                    },
                    onClick = {
                        onChange(state.copy(sort = ReviewSort.OLDEST_FIRST))
                        sortMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reviews_sort_newest_first)) },
                    leadingIcon = {
                        RadioButton(
                            selected = state.sort == ReviewSort.NEWEST_FIRST,
                            onClick = null
                        )
                    },
                    onClick = {
                        onChange(state.copy(sort = ReviewSort.NEWEST_FIRST))
                        sortMenu = false
                    }
                )
            }
        }

        // --- Botão FILTRO (dropdown com "Com foto", "Só minhas" e "Mín. estrelas") ---
        Box {
            FilledTonalButton(
                onClick = { filterMenu = true },
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Icon(Icons.Filled.FilterAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_filter))
            }
            DropdownMenu(expanded = filterMenu, onDismissRequest = { filterMenu = false }) {
                // Só com foto
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reviews_filter_with_photo)) },
                    leadingIcon = {
                        Checkbox(
                            checked = state.withPhotoOnly,
                            onCheckedChange = null
                        )
                    },
                    onClick = { onChange(state.copy(withPhotoOnly = !state.withPhotoOnly)) }
                )
                // Só minhas
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reviews_filter_only_mine)) },
                    leadingIcon = {
                        Checkbox(
                            checked = state.onlyMine,
                            onCheckedChange = null
                        )
                    },
                    onClick = { onChange(state.copy(onlyMine = !state.onlyMine)) }
                )

                Divider()

                // Cabeçalho informal (desabilitado)
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reviews_filter_min_stars)) },
                    enabled = false,
                    onClick = {}
                )
                // Qualquer nº de estrelas
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reviews_filter_any_stars)) },
                    leadingIcon = {
                        RadioButton(
                            selected = state.minStars == null,
                            onClick = null
                        )
                    },
                    onClick = { onChange(state.copy(minStars = null)) }
                )
                // 5..1 estrelas
                (5 downTo 1).forEach { s ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.reviews_filter_min_stars_value, s)) },
                        leadingIcon = {
                            RadioButton(
                                selected = state.minStars == s,
                                onClick = null
                            )
                        },
                        onClick = { onChange(state.copy(minStars = s)) }
                    )
                }
            }
        }
    }
}
