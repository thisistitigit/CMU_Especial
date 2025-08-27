package com.example.cmu_especial.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cmu_especial.domain.model.GeoPoint
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDetails: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    // Para já passamos a localização por parâmetro. Integra com LocationService depois.
    currentLocation: GeoPoint = GeoPoint(41.14961, -8.61099) // Porto (exemplo)
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadLeaderboard()
        viewModel.onSearch(currentLocation, 250)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Doçaria — Home") }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Barra de ações
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Raio: 250m", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    scope.launch { viewModel.onSearch(currentLocation, 250) }
                }) { Text("Pesquisar") }
            }

            // Leaderboard (top 10)
            if (state.leaderboard.isNotEmpty()) {
                Text(
                    "Top Avaliados",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.leaderboard) { entry ->
                        ElevatedCard(
                            modifier = Modifier.width(220.dp).height(110.dp)
                                .clickable { onOpenDetails(entry.establishment.id) }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(entry.establishment.name, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(6.dp))
                                Text("Média: %.1f".format(entry.avgRating))
                                Text("Avaliações: ${entry.count}")
                            }
                        }
                    }
                }
            }

            // Resultados da pesquisa (lista)
            Text(
                "Resultados",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.error != null) {
                Text(
                    "Erro: ${state.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.results) { est ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onOpenDetails(est.id) }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(est.name, style = MaterialTheme.typography.titleMedium)
                                est.address?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                Text("Pontuação: %.1f  •  ${est.ratingsCount} avaliações"
                                    .format(est.avgRating),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
