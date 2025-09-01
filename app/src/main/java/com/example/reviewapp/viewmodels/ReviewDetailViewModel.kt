package com.example.reviewapp.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reviewapp.data.models.Review
import com.example.reviewapp.data.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ReviewDetailViewModel @Inject constructor(
    private val reviewRepo: ReviewRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class UiState(
        val review: Review? = null,
        val isLoading: Boolean = true,
        val error: Throwable? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun load(reviewId: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        val rev = runCatching { reviewRepo.getReview(reviewId) }
            .onFailure { e -> _state.update { it.copy(error = e) } }
            .getOrNull()
        _state.update { it.copy(review = rev, isLoading = false) }
    }

    fun openPhotoExternally(photoUri: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(photoUri), "image/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { appContext.startActivity(intent) }
    }
}
