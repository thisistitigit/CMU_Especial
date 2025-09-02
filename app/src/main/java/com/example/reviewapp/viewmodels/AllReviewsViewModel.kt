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

        // 1) tenta remoto e faz cache
        runCatching { reviewRepo.refreshPlaceReviews(placeId) }
            .onFailure { e -> _state.update { s -> s.copy(error = e) } }

        // 2) lê de Room (offline-first)
        val list = runCatching { reviewRepo.allReviews(placeId) }
            .getOrDefault(emptyList())

        _state.update { it.copy(reviews = list, isLoading = false) }
    }
}
