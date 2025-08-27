package com.example.cmu_especial.ui.screens.leaderboard

import com.example.cmu_especial.domain.model.LeaderboardEntry

data class LeaderboardUiState(
    val isLoading: Boolean = false,
    val entries: List<LeaderboardEntry> = emptyList(),
    val error: String? = null
)
