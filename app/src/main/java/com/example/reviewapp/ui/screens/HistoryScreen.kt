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
import com.example.reviewapp.ui.components.HistoryItem
import com.example.reviewapp.ui.components.OfflineBanner
import com.example.reviewapp.viewmodels.HistoryViewModel

/**
 * Ecrã de **Histórico do Utilizador**.
 *
 * Apresenta a lista de reviews do utilizador autenticado, com:
 * - *Card* por review (foto, nome do estabelecimento, doçaria, estrelas, data),
 * - Mensagens de estado (**vazio**, **erro**, **loading**),
 * - *Banner* offline.
 *
 * ### Notas
 * - A lista usa `key` estável `placeId_createdAt` para melhor diffing.
 *
 * @param viewModel ViewModel que expõe o *stream* de histórico.
 * @param onOpenPlaceDetails Navega para o detalhe de um estabelecimento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onOpenPlaceDetails: (placeId: String) -> Unit
) {
    val state by viewModel.ui.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppHeader(title = stringResource(R.string.history_title)) }
    ) { inner ->
        when {
            state.isLoading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.error != null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.history_error_prefix, state.error ?: ""),
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
            ) {
                OfflineBanner()
                Spacer(Modifier.height(8.dp))

                if (state.items.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.history_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    return@Column
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.items,
                        key = { "${it.placeId}_${it.createdAt}" }
                    ) { row ->
                        HistoryItem(
                            row = row,
                            onOpen = { onOpenPlaceDetails(row.placeId) }
                        )
                    }
                }
            }
        }
    }
}
