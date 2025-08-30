// com/example/reviewapp/ui/screens/HomeScreen.kt
package com.example.reviewapp.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.R
import com.example.reviewapp.data.models.Place
import com.example.reviewapp.ui.components.PlaceListItem
import com.example.reviewapp.viewmodels.SearchViewModel

private enum class HomeSort { Rating /*, Distance*/ }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: SearchViewModel = hiltViewModel(),
    onOpenDetails: (String) -> Unit,
    onOpenReview: (String) -> Unit,
    onOpenProfile: () -> Unit
) {
    val ui by vm.state.collectAsState()

    LaunchedEffect(ui.places) {
        Log.d("HomeScreen", "places.size=${ui.places.size}")
    }

    var sort by remember { mutableStateOf(HomeSort.Rating) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.home_title)) })
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_sort_label),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.width(12.dp))
                AssistChip(
                    onClick = { sort = HomeSort.Rating },
                    label = { Text(stringResource(R.string.home_sort_rating)) }
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { /* trocar para tab Explorar via bottom bar */ }) {
                    Text(stringResource(R.string.home_explore_map))
                }
            }

            when {
                ui.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                ui.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.home_error))
                    }
                }
                ui.places.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.home_empty))
                    }
                }
                else -> {
                    val ordered = when (sort) {
                        HomeSort.Rating -> ui.places.sortedByDescending { it.avgRating }
                    }
                    PlaceList(
                        places = ordered,
                        onOpenDetails = onOpenDetails,
                        onOpenReview = onOpenReview
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceList(
    places: List<Place>,
    onOpenDetails: (String) -> Unit,
    onOpenReview: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        items(places) { p ->
            Column {
                PlaceListItem(place = p, onClick = { onOpenDetails(p.id) })
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    TextButton(onClick = { onOpenDetails(p.id) }) {
                        Text(stringResource(R.string.action_details))
                    }
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { onOpenReview(p.id) }) {
                        Text(stringResource(R.string.action_review))
                    }
                }
                Divider()
            }
        }
    }
}
