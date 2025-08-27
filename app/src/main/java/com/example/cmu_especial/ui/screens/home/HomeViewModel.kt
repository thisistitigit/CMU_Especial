package com.example.cmu_especial.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cmu_especial.domain.model.GeoPoint
import com.example.cmu_especial.domain.usecase.GetLeaderboard
import com.example.cmu_especial.domain.usecase.SearchEstablishments
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val search: SearchEstablishments,
    private val getLeaderboard: GetLeaderboard
) : ViewModel() {

    val state = MutableStateFlow(HomeUiState())

    fun onSearch(center: GeoPoint, radius: Int = 250) { // mínimo 250 m.
        viewModelScope.launch {
            state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                search(center, radius)
            }.onSuccess { list ->
                state.update { it.copy(isLoading = false, results = list) }
            }.onFailure { e ->
                state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadLeaderboard(limit: Int = 10) {
        viewModelScope.launch {
            runCatching { getLeaderboard(limit) }
                .onSuccess { entries ->
                    state.update { it.copy(leaderboard = entries) }
                }
                .onFailure { e ->
                    state.update { it.copy(error = e.message) }
                }
        }
    }
}
