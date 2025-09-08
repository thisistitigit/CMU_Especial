package com.example.reviewapp.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.R
import com.example.reviewapp.ui.components.AppHeader
import com.example.reviewapp.ui.components.OfflineBanner
import com.example.reviewapp.ui.components.RatingRow
import com.example.reviewapp.viewmodels.LeaderboardViewModel

private const val TAG_CLICK = "LeaderboardClick"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = hiltViewModel(),
    onPlaceClick: (placeId: String) -> Unit = {}
) {
    val state by viewModel.ui.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppHeader(
                title = stringResource(R.string.leaderboard_title),
                actions = {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            OfflineBanner()

            val tabs = listOf(
                R.string.leaderboard_tab_establishments to LeaderboardViewModel.Tab.ESTABLISHMENTS,
                R.string.leaderboard_tab_pastries       to LeaderboardViewModel.Tab.PASTRIES
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, (labelRes, tab) ->
                    SegmentedButton(
                        selected = state.tab == tab,
                        onClick = { viewModel.onSelectTab(tab) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor   = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor     = MaterialTheme.colorScheme.onPrimaryContainer,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor   = MaterialTheme.colorScheme.onSurface,
                            disabledActiveContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            disabledActiveContentColor   = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(stringResource(labelRes), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            when {
                state.isLoading -> Box(
                    Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                state.error != null -> Box(
                    Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) { Text(text = state.error!!, color = MaterialTheme.colorScheme.error) }

                else -> when (state.tab) {
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items,
            key = { _, it -> it.placeId.trim() }
        ) { index, row ->
            LeaderboardCardModern(
                rank = index + 1,
                title = row.name,
                subtitle = row.address.orEmpty(),
                avg = row.avg,
                count = row.count,
                onClick = {
                    val id = row.placeId.trim()
                    Log.d(
                        TAG_CLICK,
                        "clicked rank=${index + 1}, placeId='$id', name='${row.name}', reviews=${row.count}"
                    )
                    onPlaceClick(id)
                }
            )
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(items, key = { _, it -> it.pastryName }) { index, row ->
            LeaderboardCardModern(
                rank = index + 1,
                title = row.pastryName,
                subtitle = stringResource(R.string.pastry_label),
                avg = row.avg,
                count = row.count
            )
        }
    }
}


@Composable
private fun LeaderboardCardModern(
    rank: Int,
    title: String,
    subtitle: String,
    avg: Double,
    count: Int,
    onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor   = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                RatingRow(rating = avg, count = count)
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun EmptyState(text: String) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
