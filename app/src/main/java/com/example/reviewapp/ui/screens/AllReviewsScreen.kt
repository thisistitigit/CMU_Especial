package com.example.reviewapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.R
import com.example.reviewapp.ui.components.ReviewCard
import com.example.reviewapp.ui.components.ReviewFilterBar
import com.example.reviewapp.ui.components.ReviewFilterState
import com.example.reviewapp.ui.components.ReviewSort
import com.example.reviewapp.ui.components.applyReviewFilters
import com.example.reviewapp.viewmodels.AllReviewsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllReviewsScreen(
    placeId: String,
    vm: AllReviewsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onOpenReviewDetails: (String) -> Unit = {}
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(placeId) { vm.load(placeId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reviews_all_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_back))
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.padding(padding).fillMaxSize()) {
                CircularProgressIndicator(Modifier.padding(24.dp))
            }
            state.reviews.isEmpty() -> Box(Modifier.padding(padding).fillMaxSize()) {
                Text(
                    text = stringResource(R.string.reviews_empty),
                    modifier = Modifier.padding(16.dp)
                )
            }
            else -> {

                // no conteúdo do Scaffold, ramo "else"
                var filters by remember { mutableStateOf(ReviewFilterState(sort = ReviewSort.OLDEST_FIRST)) }

                Column {
                    ReviewFilterBar(
                        state = filters,
                        onChange = { filters = it }
                    )

                    val filtered = applyReviewFilters(
                        state.reviews,
                        filters,
                        currentUserId = null
                    )

                    LazyColumn(
                        modifier = Modifier,
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(filtered, key = { it.id }) { r ->
                            ReviewCard(review = r, onClick = { onOpenReviewDetails(r.id) })
                            Divider()
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.reviews, key = { it.id }) { r ->
                        ReviewCard(review = r, onClick = { onOpenReviewDetails(r.id) })
                        Divider()
                    }
                }
            }
        }
    }
}