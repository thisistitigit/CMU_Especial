package com.example.reviewapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reviewapp.data.models.Review
import com.example.reviewapp.data.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AllReviewsViewModel @Inject constructor(
    private val reviewRepo: ReviewRepository
) : ViewModel() {

    data class UiState(
        val reviews: List<Review> = emptyList(),
        val isLoading: Boolean = true,
        val error: Throwable? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun load(placeId: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        val result = runCatching { reviewRepo.allReviews(placeId) }
        _state.update {
            it.copy(
                reviews = result.getOrDefault(emptyList()),
                isLoading = false,
                error = result.exceptionOrNull()
            )
        }
    }
}
