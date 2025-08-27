package com.example.cmu_especial.ui.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cmu_especial.domain.usecase.GetLeaderboard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Carrega o leaderboard de estabelecimentos por média de avaliações.
 * Usa o caso de uso GetLeaderboard (repo calcula/obtém média e contagem).
 */
@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val getLeaderboard: GetLeaderboard
) : ViewModel() {

    val ui = MutableStateFlow(LeaderboardUiState())

    fun load(limit: Int = 50) {
        viewModelScope.launch {
            ui.update { it.copy(isLoading = true, error = null) }
            runCatching { getLeaderboard(limit) }
                .onSuccess { list -> ui.update { it.copy(isLoading = false, entries = list) } }
                .onFailure { e -> ui.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
