package com.example.cmu_especial.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cmu_especial.domain.usecase.GetMyHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Carrega o histórico de avaliações do utilizador (mais recentes primeiro).
 * Usa GetMyHistory, que por omissão vai buscar o currentUserId() ao UserRepository.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getMyHistory: GetMyHistory
) : ViewModel() {

    val ui = MutableStateFlow(HistoryUiState())

    /**
     * @param userId opcional — se null, usa o utilizador atual.
     */
    fun load(userId: String? = null) {
        viewModelScope.launch {
            ui.update { it.copy(isLoading = true, error = null) }
            runCatching { getMyHistory(userId) }
                .onSuccess { list -> ui.update { it.copy(isLoading = false, reviews = list) } }
                .onFailure { e -> ui.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
