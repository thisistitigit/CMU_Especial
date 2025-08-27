package com.example.cmu_especial.ui.screens.home

import com.example.cmu_especial.domain.model.Establishment
import com.example.cmu_especial.domain.model.LeaderboardEntry

data class HomeUiState(
    val isLoading: Boolean = false,
    val results: List<Establishment> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val error: String? = null
)
