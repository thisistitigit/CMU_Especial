package com.example.cmu_especial.ui.screens.leaderboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onOpenDetails: (String) -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load(limit = 50)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Leaderboard") }) }
    ) { padding ->
        if (ui.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (ui.error != null) {
            Text(
                "Erro: ${ui.error}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(ui.entries) { index, entry ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onOpenDetails(entry.establishment.id) }
                    ) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("${index + 1}.", fontWeight = FontWeight.SemiBold)
                            Column(Modifier.weight(1f)) {
                                Text(entry.establishment.name, fontWeight = FontWeight.SemiBold)
                                Text(entry.establishment.address ?: "", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                Text("Média: %.1f".format(entry.avgRating))
                                Text("${entry.count} avaliações", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
