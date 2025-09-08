package com.example.reviewapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.reviewapp.R
import com.example.reviewapp.ui.components.ReviewDetails
import com.example.reviewapp.viewmodels.ReviewDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDetailScreen(
    reviewId: String,
    vm: ReviewDetailViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(reviewId) { vm.load(reviewId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.review_details_title)) },
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
            state.review == null -> Box(Modifier.padding(padding).fillMaxSize()) {
                Text(stringResource(R.string.state_error), modifier = Modifier.padding(16.dp))
            }
            else -> {
                val r = state.review
                Column(
                    Modifier.padding(padding).padding(16.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val photo = ReviewDetails.bestPhoto(r!!)
                    if (photo != null) {
                        AsyncImage(
                            model = photo,
                            contentDescription = stringResource(R.string.review_photo),
                            modifier = Modifier.fillMaxWidth().height(300.dp)
                        )
                        TextButton(onClick = { vm.openPhotoExternally(photo) }) {
                            Text(stringResource(R.string.review_open_external))
                        }
                    } else {
                        Text(stringResource(R.string.review_no_photo))
                    }

                    Text(r.userName, style = MaterialTheme.typography.titleMedium)
                    Text(ReviewDetails.relativeDate(LocalContext.current, r.createdAt))
                    Text(stringResource(R.string.field_pastry) + ": " + r.pastryName)
                    Text(stringResource(R.string.field_comment) + ": " + r.comment)
                }
            }
        }
    }
}
