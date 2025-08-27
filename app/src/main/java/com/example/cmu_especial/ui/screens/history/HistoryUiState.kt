package com.example.cmu_especial.ui.screens.history

import com.example.cmu_especial.domain.model.Review

data class HistoryUiState(
    val isLoading: Boolean = false,
    val reviews: List<Review> = emptyList(),
    val error: String? = null
)
