// com/example/reviewapp/ui/screens/DetailsScreen.kt
package com.example.reviewapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.R
import com.example.reviewapp.ui.components.AppHeader
import com.example.reviewapp.ui.components.OfflineBanner
import com.example.reviewapp.ui.components.RatingRow
import com.example.reviewapp.ui.components.ReviewCard
import com.example.reviewapp.ui.components.ReviewFilterState
import com.example.reviewapp.ui.components.ReviewFiltersMinimal
import com.example.reviewapp.ui.components.ReviewSort
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

    // Filtros visuais para a lista
    var filters by remember { mutableStateOf(ReviewFilterState(sort = ReviewSort.OLDEST_FIRST)) }

    val filteredAll = remember(state.latestReviews, filters, uid) {
        applyReviewFilters(
            all = state.latestReviews,
            f = filters,
            currentUserId = uid
        )
    }
    val filteredTop10 = remember(filteredAll) { filteredAll.take(10) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppHeader(
                title = state.place?.name ?: stringResource(R.string.details_title),
                onBack = onBack
            )
        },
        floatingActionButton = {
            state.place?.let { place ->
                ExtendedFloatingActionButton(
                    onClick = { onReview(place.id, place.lat, place.lng) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(stringResource(R.string.details_review_cta))
                }
            }
        }
    ) { padding ->

        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.error != null -> Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text(text = stringResource(R.string.state_error), color = MaterialTheme.colorScheme.error) }

            else -> {
                state.place?.let { place ->
                    LazyColumn(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        /* ===== HERO / INFO ===== */
                        item {
                            ElevatedCard(
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor   = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    OfflineBanner()

                                    // Categoria e morada
                                    place.category?.takeIf { it.isNotBlank() }?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    place.address?.takeIf { it.isNotBlank() }?.let {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Contacto
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (place.phone.isNullOrBlank()) {
                                            Text(
                                                text = stringResource(R.string.place_phone_none),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            FilledTonalButton(
                                                onClick = { vm.call(place.phone) },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Filled.Phone, contentDescription = null)
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    stringResource(
                                                        R.string.place_call_action_with_number,
                                                        place.phone
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    // Coordenadas (chipzinha)
                                    Spacer(Modifier.height(10.dp))
                                    AssistChip(
                                        onClick = {},
                                        label = {
                                            Text(
                                                stringResource(R.string.place_lat_lng, place.lat, place.lng),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.Place,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        shape = RoundedCornerShape(16.dp)
                                    )

                                    // Ações
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { vm.openOnMap(place.lat, place.lng, place.name) },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.Map, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.place_view_on_map))
                                        }
                                        FilledTonalButton(
                                            onClick = { vm.getDirections(place.lat, place.lng) },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.Directions, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.place_get_directions))
                                        }
                                    }
                                }
                            }
                        }

                        /* ===== RATINGS ===== */
                        item {
                            ElevatedCard(
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor   = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(16.dp)) {

                                    Text(
                                        text = stringResource(R.string.ratings_google_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    if (place.ratingsCount > 0) {
                                        RatingRow(
                                            rating = place.avgRating,
                                            count  = place.ratingsCount
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.ratings_not_available),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier
                                            .padding(vertical = 12.dp),
                                        thickness = DividerDefaults.Thickness,
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    Text(
                                        text = stringResource(R.string.ratings_internal_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    if (state.internalCount > 0) {
                                        RatingRow(
                                            rating = state.internalAvg,
                                            count  = state.internalCount
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.ratings_not_available),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        /* ===== REVIEWS: header ===== */
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.reviews_section_title_top10),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (state.internalCount > state.latestReviews.size) {
                                    TextButton(onClick = { onOpenAllReviews(place.id) }) {
                                        Text(stringResource(R.string.reviews_view_all))
                                    }
                                }
                            }
                        }

                        /* ===== REVIEWS: filtros + contagem ===== */
                        item {
                            ReviewFiltersMinimal(
                                state = filters,
                                onChange = { filters = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = stringResource(R.string.reviews_count_filtered, filteredAll.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }


                        /* ===== REVIEWS: lista (Top 10 após filtros) ===== */
                        if (filteredAll.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.reviews_empty),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            items(filteredTop10, key = { it.id }) { r ->
                                ReviewCard(
                                    review = r,
                                    onClick = { onOpenReviewDetails(r.id) }
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
