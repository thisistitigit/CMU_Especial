package com.example.reviewapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.reviewapp.R

/**
 * Versão minimal: dois ícones (ordenar + filtro) sem fundo/sem texto,
 * cada um a abrir o respetivo DropdownMenu.
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

        // ---------- Ordenar ----------
        Box {
            IconButton(onClick = { sortOpen = true }) {
                Icon(
                    imageVector = Icons.Filled.Sort,
                    contentDescription = null, // sem texto/desc para manter minimal
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
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
                        sortOpen = false
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
                        sortOpen = false
                    }
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        // ---------- Filtros ----------
        Box {
            IconButton(onClick = { filterOpen = true }) {
                Icon(
                    imageVector = Icons.Filled.FilterAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            DropdownMenu(expanded = filterOpen, onDismissRequest = { filterOpen = false }) {

                // Só com foto
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reviews_filter_with_photo)) },
                    leadingIcon = {
                        Checkbox(
                            checked = state.withPhotoOnly,
                            onCheckedChange = null
                        )
                    },
                    onClick = {
                        onChange(state.copy(withPhotoOnly = !state.withPhotoOnly))
                        // mantém o menu aberto para permitir múltiplos toggles
                    }
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
                    onClick = {
                        onChange(state.copy(onlyMine = !state.onlyMine))
                    }
                )

                androidx.compose.material3.Divider()

                // Cabeçalho (desabilitado)
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
                    onClick = {
                        onChange(state.copy(minStars = null))
                        // mantém aberto (consistente com o FilterBar original)
                    }
                )

                // 5..1 estrelas
                for (s in 5 downTo 1) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.reviews_filter_min_stars_value, s)) },
                        leadingIcon = {
                            RadioButton(
                                selected = state.minStars == s,
                                onClick = null
                            )
                        },
                        onClick = {
                            onChange(state.copy(minStars = s))
                        }
                    )
                }
            }
        }
    }
}
