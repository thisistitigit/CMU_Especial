package com.example.cmu_especial.ui.screens.details

import com.example.cmu_especial.domain.model.Establishment
import com.example.cmu_especial.domain.model.Review

data class DetailsUiState(
    val isLoading: Boolean = true,
    val establishment: Establishment? = null,
    val recent: List<Review> = emptyList(),
    val error: String? = null
)
