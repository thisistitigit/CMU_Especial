// com/example/reviewapp/ui/screens/DetailsScreen.kt
package com.example.reviewapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.R
import com.example.reviewapp.ui.components.ReviewCard
import com.example.reviewapp.ui.components.ReviewFilterBar
import com.example.reviewapp.ui.components.ReviewFilterState
import com.example.reviewapp.ui.components.ReviewSort
import com.example.reviewapp.ui.components.StarRating
import com.example.reviewapp.ui.components.applyReviewFilters
import com.example.reviewapp.viewmodels.AuthViewModel
import com.example.reviewapp.viewmodels.DetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    placeId: String,
    vm: DetailsViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onReview: (String, Double?, Double?) -> Unit = { _,_,_ -> },
    onOpenReviewDetails: (String) -> Unit = {},
    onOpenAllReviews: (String) -> Unit = {}
) {
    val state by vm.state.collectAsState()
    val uid by authVm.currentUserId.collectAsState()

    LaunchedEffect(placeId) { vm.load(placeId) }

    // --- Estado de filtros partilhado na screen ---
    var filters by remember { mutableStateOf(ReviewFilterState(sort = ReviewSort.OLDEST_FIRST)) }

    val filteredAll = remember(state.latestReviews, filters, uid) {
        applyReviewFilters(
            all = state.latestReviews,
            f = filters,
            currentUserId = uid           // ← AGORA PASSAMOS O UID
        )
    }
    val filteredTop10 = remember(filteredAll) { filteredAll.take(10) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.place?.name ?: stringResource(R.string.details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            state.place?.let { place ->
                ExtendedFloatingActionButton(onClick = {
                    onReview(place.id, place.lat, place.lng)   // envia lat/lng
                }) {
                    Text(stringResource(R.string.details_review_cta))
                }
            }
        }
    ) { padding ->

        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                ) { Text(text = stringResource(R.string.state_loading)) }
            }

            state.error != null -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                ) { Text(text = stringResource(R.string.state_error)) }
            }

            else -> {
                state.place?.let { place ->
                    LazyColumn(Modifier.padding(padding)) {

                        // ===== Cabeçalho / Identificação =====
                        item {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                place.category?.takeIf { it.isNotBlank() }?.let { cat ->
                                    Text(text = cat, style = MaterialTheme.typography.bodyMedium)
                                }
                                place.address?.takeIf { it.isNotBlank() }?.let { addr ->
                                    Text(text = addr, style = MaterialTheme.typography.bodyMedium)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    if (place.phone.isNullOrBlank()) {
                                        Text(
                                            text = stringResource(R.string.place_phone_none),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    } else {
                                        TextButton(onClick = { vm.call(place.phone!!) }) {
                                            Text(
                                                text = stringResource(
                                                    R.string.place_call_action_with_number,
                                                    place.phone!!
                                                )
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = stringResource(
                                        R.string.place_lat_lng,
                                        place.lat,
                                        place.lng
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(onClick = {
                                        vm.openOnMap(place.lat, place.lng, place.name)
                                    }) { Text(stringResource(R.string.place_view_on_map)) }
                                    Button(onClick = {
                                        vm.getDirections(place.lat, place.lng)
                                    }) { Text(stringResource(R.string.place_get_directions)) }
                                }
                            }
                            Divider()
                        }

                        // ===== Ratings: Google + Interno =====
                        item {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                if (place.ratingsCount > 0) {
                                    Text(
                                        text = stringResource(R.string.ratings_google_title),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val gAvg = place.avgRating
                                        val gCount = place.ratingsCount
                                        StarRating(rating = gAvg)
                                        val countStr = pluralStringResource(
                                            R.plurals.ratings_count, gCount, gCount
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.format_score_with_count,
                                                gAvg,
                                                countStr
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.padding(top = 8.dp))
                                }

                                Text(
                                    text = stringResource(R.string.ratings_internal_title),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (state.internalCount > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StarRating(rating = state.internalAvg)
                                        val countStr = pluralStringResource(
                                            R.plurals.ratings_count,
                                            state.internalCount,
                                            state.internalCount
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.format_score_with_count,
                                                state.internalAvg,
                                                countStr
                                            )
                                        )
                                    }
                                } else {
                                    Text(
                                        text = stringResource(R.string.ratings_not_available),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            Divider()
                        }

                        // ===== Secção Reviews =====
                        // Cabeçalho (Top 10 + Ver todos)
                        item {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.reviews_section_title_top10),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                if (state.internalCount > state.latestReviews.size) {
                                    TextButton(onClick = { onOpenAllReviews(place.id) }) {
                                        Text(stringResource(R.string.reviews_view_all))
                                    }
                                }
                            }
                        }

                        // Barra de filtros + contagem (aplica a toda a lista, mas a renderização mantém Top 10)
                        item {
                            ReviewFilterBar(
                                state = filters,
                                onChange = { filters = it }
                            )
                            Text(
                                text = stringResource(R.string.reviews_count_filtered, filteredAll.size),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        // Lista (Top 10 após filtros)
                        if (filteredAll.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.reviews_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        } else {
                            items(filteredTop10, key = { it.id }) { r ->
                                ReviewCard(review = r, onClick = { onOpenReviewDetails(r.id) })
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }
}
