// com/example/reviewapp/ui/screens/HomeScreen.kt
package com.example.reviewapp.ui.screens

import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.R
import com.example.reviewapp.viewmodels.SearchViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.reviewapp.ui.components.PlaceHorizontalSection

private enum class HomeSort { Rating }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: SearchViewModel = hiltViewModel(),
    onOpenDetails: (String) -> Unit,
    onOpenReview: (String) -> Unit, // mantido caso uses noutro lado
    onOpenProfile: () -> Unit
) {
    val ui by vm.state.collectAsState()
    var sort by remember { mutableStateOf(HomeSort.Rating) }

    var query by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.search_location_hint)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (query.isBlank()) return@KeyboardActions
                                scope.launch {
                                    isSearching = true
                                    val latLng = geocodeFirstLatLng(ctx, query)
                                    isSearching = false
                                    latLng?.let { vm.refreshAt(it) }
                                }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {

            // header / ordenação
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.home_sort_label), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(12.dp))
                AssistChip(
                    onClick = { sort = HomeSort.Rating },
                    label = { Text(stringResource(R.string.home_sort_rating)) }
                )
                Spacer(Modifier.weight(1f))
                if (isSearching || ui.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            when {
                ui.isLoading && ui.places.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                ui.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.home_error))
                    }
                }
                ui.places.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.home_empty))
                    }
                }
                else -> {
                    val ordered = when (sort) {
                        HomeSort.Rating -> ui.places.sortedByDescending { it.avgRating }
                    }

                    val showingSuggestions = query.isBlank() && !isSearching
                    val title = if (showingSuggestions)
                        stringResource(R.string.home_suggestions_title)
                    else
                        stringResource(R.string.home_search_results_title)

                    PlaceHorizontalSection(
                        title = title,
                        places = ordered.take(30), // limita para não pesar a UI
                        onPlaceClick = onOpenDetails,
                        showSeeMore = showingSuggestions,
                        onSeeMoreClick = { /* TODO: navegar para ecrã Top/Lista completa se quiseres */ },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private suspend fun geocodeFirstLatLng(
    context: Context,
    query: String
): LatLng? = withContext(Dispatchers.IO) {
    @Suppress("DEPRECATION")
    runCatching {
        Geocoder(context).getFromLocationName(query, 1)
            ?.firstOrNull()
            ?.let { LatLng(it.latitude, it.longitude) }
    }.getOrNull()
}
