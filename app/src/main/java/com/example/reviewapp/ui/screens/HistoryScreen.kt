package com.example.reviewapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // ← IMPORT ESSENCIAL
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.R
import com.example.reviewapp.ui.components.HistoryItem
import com.example.reviewapp.viewmodels.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onOpenPlaceDetails: (placeId: String) -> Unit
) {
    val state by viewModel.ui.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }) }
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
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Usa o overload de lista + key estável sem depender de reviewId
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
