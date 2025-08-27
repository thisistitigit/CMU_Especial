package com.example.cmu_especial.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cmu_especial.ui.shared.RatingBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenDetails: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("O meu histórico") }) }
    ) { padding ->
        when {
            ui.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            ui.error != null -> Text(
                "Erro: ${ui.error}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ui.reviews) { r ->
                        ElevatedCard(
                            onClick = { onOpenDetails(r.establishmentId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Estabelecimento: ${r.establishmentId}") // podes trocar pelo nome se o juntares no Review
                                Spacer(Modifier.height(6.dp))
                                RatingBar(r.rating)
                                r.comment?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                val df = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
                                Text(df.format(Date(r.createdAt)), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
