package com.example.reviewapp.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.ui.components.ReviewCard
import com.example.reviewapp.ui.components.StarRating
import com.example.reviewapp.viewmodels.DetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    placeId: String,
    vm: DetailsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onReview: (String) -> Unit = {}
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(placeId) { vm.load(placeId) }

    state.place?.let { place ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(place.name) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(onClick = { onReview(place.id) }) {
                    Text("Avaliar")
                }
            }
        ) { padding ->
            LazyColumn(Modifier.padding(padding)) {
                item {
                    Text(
                        place.address.orEmpty(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    place.phone?.let { phone ->
                        TextButton(onClick = { vm.call(phone) }) { Text("Telefonar: $phone") }
                    }
                    StarRating(rating = place.avgRating)
                    Divider()
                }
                items(state.latestReviews) { r ->
                    ReviewCard(review = r)
                    Divider()
                }
            }
        }
    }
}
