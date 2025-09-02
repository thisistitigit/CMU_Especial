package com.example.reviewapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.R
import com.example.reviewapp.viewmodels.LeaderboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = hiltViewModel(),
    onPlaceClick: (placeId: String) -> Unit = {}
) {
    val state by viewModel.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.leaderboard_title)) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            // Tabs
            TabRow(selectedTabIndex = state.tab.ordinal) {
                Tab(
                    selected = state.tab == LeaderboardViewModel.Tab.ESTABLISHMENTS,
                    onClick = { viewModel.onSelectTab(LeaderboardViewModel.Tab.ESTABLISHMENTS) },
                    text = { Text(stringResource(R.string.leaderboard_tab_establishments)) }
                )
                Tab(
                    selected = state.tab == LeaderboardViewModel.Tab.PASTRIES,
                    onClick = { viewModel.onSelectTab(LeaderboardViewModel.Tab.PASTRIES) },
                    text = { Text(stringResource(R.string.leaderboard_tab_pastries)) }
                )
            }

            // Conteúdo
            when {
                state.isLoading -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = state.error!!, color = MaterialTheme.colorScheme.error)
                    }
                }

                else -> {
                    when (state.tab) {
                        LeaderboardViewModel.Tab.ESTABLISHMENTS ->
                            EstablishmentsList(
                                items = state.establishments,
                                onPlaceClick = onPlaceClick
                            )

                        LeaderboardViewModel.Tab.PASTRIES ->
                            PastriesList(items = state.pastries)
                    }
                }
            }
        }
    }
}


@Composable
private fun EstablishmentsList(
    items: List<LeaderboardViewModel.PlaceRow>,
    onPlaceClick: (placeId: String) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState(text = stringResource(R.string.leaderboard_empty_establishments))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        itemsIndexed(items, key = { _, it -> it.placeId }) { index, row ->
            LeaderboardCard(
                rank = index + 1,
                title = row.name,
                subtitle = row.address ?: "",
                metrics = "${"%.2f".format(row.avg)} ★ · ${row.count} ${stringResource(R.string.reviews_short)}",
                onClick = { onPlaceClick(row.placeId) }
            )
            Divider()
        }
    }
}

@Composable
private fun PastriesList(items: List<LeaderboardViewModel.PastryRow>) {
    if (items.isEmpty()) {
        EmptyState(text = stringResource(R.string.leaderboard_empty_pastries))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        itemsIndexed(items, key = { _, it -> it.pastryName }) { index, row ->
            LeaderboardCard(
                rank = index + 1,
                title = row.pastryName,
                subtitle = stringResource(R.string.pastry_label),
                metrics = "${"%.2f".format(row.avg)} ★ · ${row.count} ${stringResource(R.string.reviews_short)}"
            )
            Divider()
        }
    }
}

@Composable
private fun LeaderboardCard(
    rank: Int,
    title: String,
    subtitle: String,
    metrics: String,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.widthIn(min = 40.dp)
                )
                Column(Modifier.padding(start = 8.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        supportingContent = { Text(metrics) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .let { if (onClick != null) it.clickable { onClick() } else it }
    )
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
