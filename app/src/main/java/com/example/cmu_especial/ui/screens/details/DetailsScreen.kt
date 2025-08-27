package com.example.cmu_especial.ui.screens.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cmu_especial.ui.shared.RatingBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    establishmentId: String,
    viewModel: DetailsViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()

    LaunchedEffect(establishmentId) { viewModel.load(establishmentId) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Detalhes") }) }
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
            val est = ui.establishment
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(est?.name ?: "", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(est?.address ?: "", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                    Text("Pontuação: %.1f  •  ${est?.ratingsCount ?: 0} avaliações"
                        .format(est?.avgRating ?: 0.0))
                }

                item {
                    Divider()
                    Text("Últimas 10 avaliações", style = MaterialTheme.typography.titleMedium)
                }

                items(ui.recent) { r ->
                    ElevatedCard {
                        Column(Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text("Por: ${r.userName}", fontWeight = FontWeight.SemiBold)
                                    RatingBar(r.rating)
                                }
                            }
                            r.photoCloudUrl?.let { url ->
                                Spacer(Modifier.height(8.dp))
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Foto do doce",
                                    modifier = Modifier.fillMaxWidth().height(180.dp)
                                )
                            }
                            r.comment?.let {
                                Spacer(Modifier.height(6.dp))
                                Text(it)
                            }
                            val df = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
                            Text(
                                df.format(Date(r.createdAt)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
