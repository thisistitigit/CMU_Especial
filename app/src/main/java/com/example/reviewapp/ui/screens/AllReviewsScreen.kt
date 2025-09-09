package com.example.reviewapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.R
import com.example.reviewapp.ui.components.AppHeader
import com.example.reviewapp.ui.components.OfflineBanner
import com.example.reviewapp.ui.components.ReviewCard
import com.example.reviewapp.ui.components.ReviewFilterState
import com.example.reviewapp.ui.components.ReviewFiltersMinimal
import com.example.reviewapp.ui.components.ReviewSort
import com.example.reviewapp.ui.components.applyReviewFilters
import com.example.reviewapp.viewmodels.AllReviewsViewModel
import com.example.reviewapp.viewmodels.AuthViewModel

/**
 * Ecrã que lista **todas as reviews** de um determinado estabelecimento.
 *
 * ### Responsabilidades
 * - Carrega e observa o estado de `AllReviewsViewModel`.
 * - Aplica filtros/ordenação reativa via [ReviewFilterState].
 * - Mostra estados de **loading**, **empty** e **erro**.
 * - Integra o *banner* [OfflineBanner] para consciência de conectividade.
 *
 * ### Performance & Recomposição
 * - `filtered` é memoizado com `remember(...)` para reduzir recomposições
 *   quando filtros/UID/reviews não mudam.
 * - `LazyColumn` usa `key = { it.id }` para diffs estáveis.
 *
 * @param placeId ID do estabelecimento alvo.
 * @param vm ViewModel que expõe as reviews e ciclo de carregamento.
 * @param authVm ViewModel para obter o UID atual (filtrar "só as minhas").
 * @param onBack Ação para voltar atrás (navegação).
 * @param onOpenReviewDetails Ação para abrir detalhes de uma review.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllReviewsScreen(
    placeId: String,
    vm: AllReviewsViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onOpenReviewDetails: (String) -> Unit = {}
) {
    val state by vm.state.collectAsState()
    val uid by authVm.currentUserId.collectAsState()

    LaunchedEffect(placeId) { vm.load(placeId) }

    Scaffold(
        topBar = {
            AppHeader(
                title = stringResource(R.string.reviews_all_title),
                onBack = onBack
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.reviews.isEmpty() -> Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text(text = stringResource(R.string.reviews_empty)) }

            else -> {
                var filters by remember { mutableStateOf(ReviewFilterState(sort = ReviewSort.OLDEST_FIRST)) }
                val filtered = remember(state.reviews, filters, uid) {
                    applyReviewFilters(state.reviews, filters, currentUserId = uid)
                }

                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    ReviewFiltersMinimal(
                        state = filters,
                        onChange = { filters = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    OfflineBanner()

                    Text(
                        text = stringResource(R.string.reviews_count_filtered, filtered.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = stringResource(R.string.reviews_empty))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(filtered, key = { it.id }) { r ->
                                ReviewCard(
                                    review = r,
                                    onClick = { onOpenReviewDetails(r.id) }
                                )
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }
}
