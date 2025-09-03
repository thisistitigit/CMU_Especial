package com.example.reviewapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.reviewapp.R
import com.example.reviewapp.viewmodels.HistoryViewModel

@Composable
fun HistoryItem(
    row: HistoryViewModel.Row,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

            val model = row.photoLocalPath?.takeIf { it.isNotBlank() } ?: row.photoCloudUrl
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = stringResource(R.string.cd_review_photo),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(Modifier.weight(1f)) {
                Text(row.placeName, style = MaterialTheme.typography.titleMedium)
                if (row.pastryName.isNotBlank()) {
                    Text(row.pastryName, style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(row.stars.coerceIn(0, 5)) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = stringResource(R.string.cd_star_icon),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(row.createdAt.asDateTimeLabel(), style = MaterialTheme.typography.bodySmall)
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.cd_open_details)
            )
        }
    }
}

fun Long.asDateTimeLabel(): String =
    java.time.Instant.ofEpochMilli(this)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

