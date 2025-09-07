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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onOpenPlaceDetails: (placeId: String) -> Unit
) {
    val state by viewModel.ui.collectAsState()

    Scaffold(
        topBar = { AppHeader(title = stringResource(R.string.history_title)) }
    ) { inner ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(inner)) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            state.error != null -> Box(Modifier.fillMaxSize().padding(inner)) {
                Text(
                    text = stringResource(R.string.history_error_prefix, state.error ?: ""),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
            ) {
                OfflineBanner()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = state.items,
                        key = { "${it.placeId}_${it.createdAt}" }
                    ) { row ->
                        HistoryItem(row, onOpen = { onOpenPlaceDetails(row.placeId) })
                    }
                }
            }
        }
    }
}
