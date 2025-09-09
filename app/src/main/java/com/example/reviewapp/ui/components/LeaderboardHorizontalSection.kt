package com.example.reviewapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.reviewapp.viewmodels.LeaderboardViewModel

/**
 * Secção horizontal reutilizável para **leaderboards** de locais.
 *
 * Mostra um título e uma `LazyRow` de cartões compactos com nome, morada e
 * resumo de rating (média/contagem).
 *
 * @param title cabeçalho da secção (ex.: "Mais bem avaliados").
 * @param rows linhas pré-formatadas pelo [LeaderboardViewModel].
 * @param onPlaceClick callback ao tocar num cartão (recebe `placeId`).
 * @param modifier modificador externo (margens, largura, etc.).
 */
@Composable
fun LeaderboardHorizontalSection(
    title: String,
    rows: List<LeaderboardViewModel.PlaceRow>,
    onPlaceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(top = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(rows, key = { it.placeId }) { row ->
                LeaderboardPlaceCard(row = row, onClick = { onPlaceClick(row.placeId) })
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Cartão compacto de leaderboard.
 *
 * @param row dados agregados do local (nome, morada, média/contagem).
 * @param onClick ação ao selecionar o cartão.
 */
@Composable
private fun LeaderboardPlaceCard(
    row: LeaderboardViewModel.PlaceRow,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .width(240.dp)
            .heightIn(min = 108.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = row.name.ifBlank { "—" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            row.address?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            RatingRow(rating = row.avg, count = row.count)
        }
    }
}
