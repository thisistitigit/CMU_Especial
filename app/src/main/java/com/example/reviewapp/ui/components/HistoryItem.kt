package com.example.reviewapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.reviewapp.R
import com.example.reviewapp.viewmodels.HistoryViewModel

/**
 * Cartão compacto para um item do **histórico de reviews**.
 *
 * Layout:
 * - *Thumbnail* 64dp (foto local/cloud) ou *placeholder* com iniciais.
 * - Coluna com `placeName`, `pastryName` e _row_ de estrelas + data.
 * - *Chevron* à direita indicando ação de detalhe.
 *
 * Perfomance:
 * - Usa `AsyncImage` (Coil) com `ContentScale.Crop` para miniaturas.
 *
 * @param row linha de dados preparada pelo [HistoryViewModel].
 * @param onOpen ação ao tocar no cartão (abre detalhes da review).
 */
@Composable
fun HistoryItem(
    row: HistoryViewModel.Row,
    onOpen: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor   = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            Modifier
                .padding(14.dp)
                .heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val model = row.photoLocalPath?.takeIf { it.isNotBlank() } ?: row.photoCloudUrl
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = stringResource(R.string.cd_review_photo),
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder com iniciais do local (melhora reconhecimento imediato)
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initialsFrom(row.placeName),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    row.placeName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (row.pastryName.isNotBlank()) {
                    Text(
                        row.pastryName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(row.stars.coerceIn(0, 5)) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = stringResource(R.string.cd_star_icon),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        row.createdAt.asDateTimeLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.cd_open_details),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Extrai duas iniciais de um nome para _placeholders_ de avatar.
 * - String vazia → `"?"`
 * - Uma palavra → 2 primeiras letras
 * - Várias palavras → primeira letra da 1.ª + primeira da última
 */
private fun initialsFrom(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> (parts.first().first().toString() + parts.last().first()).uppercase()
    }
}

/**
 * Formata `epochMillis` para `dd/MM/yyyy HH:mm` no fuso do dispositivo.
 *
 * @receiver epoch millis UTC.
 * @return string formatada para UI legível.
 */
fun Long.asDateTimeLabel(): String =
    java.time.Instant.ofEpochMilli(this)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
