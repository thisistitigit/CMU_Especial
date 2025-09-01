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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.R
import com.example.reviewapp.ui.components.ReviewCard
import com.example.reviewapp.ui.components.StarRating
import com.example.reviewapp.viewmodels.DetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    placeId: String,
    vm: DetailsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onReview: (String) -> Unit = {},
    onOpenReviewDetails: (String) -> Unit = {},
    onOpenAllReviews: (String) -> Unit = {}
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(placeId) { vm.load(placeId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.place?.name ?: stringResource(R.string.details_title)
                    )
                },
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
                ExtendedFloatingActionButton(onClick = { onReview(place.id) }) {
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
                ) {
                    Text(text = stringResource(R.string.state_loading))
                }
            }

            state.error != null -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Text(text = stringResource(R.string.state_error))
                }
            }

            else -> {
                state.place?.let { place ->
                    LazyColumn(Modifier.padding(padding)) {

                        // ===== Cabeçalho / Identificação =====
                        item {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                // Categoria / “Doçaria avaliada”
                                place.category?.takeIf { it.isNotBlank() }?.let { cat ->
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                // Endereço
                                place.address?.takeIf { it.isNotBlank() }?.let { addr ->
                                    Text(
                                        text = addr,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                // Telefone
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

                                // Coordenadas
                                Text(
                                    text = stringResource(
                                        R.string.place_lat_lng,
                                        place.lat,
                                        place.lng
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )

                                // Botões de mapa
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(onClick = {
                                        vm.openOnMap(place.lat, place.lng, place.name)
                                    }) {
                                        Text(stringResource(R.string.place_view_on_map))
                                    }
                                    Button(onClick = {
                                        vm.getDirections(place.lat, place.lng)
                                    }) {
                                        Text(stringResource(R.string.place_get_directions))
                                    }
                                }
                            }

                            Divider()
                        }

                        // ===== Ratings: Google + Interno =====
                        item {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                // Google
                                place.avgRating?.let { gAvg ->
                                    val gCount = place.ratingsCount ?: 0
                                    Text(
                                        text = stringResource(R.string.ratings_google_title),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
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

                                // Interno
                                Text(
                                    text = stringResource(R.string.ratings_internal_title),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (place.internalRatingsCount > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StarRating(rating = place.internalAvgRating)
                                        val countStr = pluralStringResource(
                                            R.plurals.ratings_count,
                                            place.internalRatingsCount,
                                            place.internalRatingsCount
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.format_score_with_count,
                                                place.avgRating,
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
                        item {
                            Text(
                                text = stringResource(R.string.reviews_section_title),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        if (state.latestReviews.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.reviews_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        } else {
                            items(state.latestReviews) { r ->
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
