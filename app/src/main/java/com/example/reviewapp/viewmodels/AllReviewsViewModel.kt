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

/**
 * **VM** responsável por carregar e expor todas as reviews de um *place*.
 *
 * Estratégia:
 * 1. Força `refreshPlaceReviews(placeId)` (sincroniza Room ⇄ Firestore);
 * 2. Lê `allReviews(placeId)` do Room para UI rápida e determinística;
 * 3. Mantém estado reativo com `isLoading` e `error`.
 */
@HiltViewModel
class AllReviewsViewModel @Inject constructor(
    private val reviewRepo: ReviewRepository
) : ViewModel() {

    /** Estado imutável consumido pela UI de "Todas as Reviews". */
    data class UiState(
        val reviews: List<Review> = emptyList(),
        val isLoading: Boolean = true,
        val error: Throwable? = null
    )

    private val _state = MutableStateFlow(UiState())
    /** Fluxo frio; a UI faz `collectAsState()` para renderizar. */
    val state: StateFlow<UiState> = _state

    /**
     * Carrega as reviews do local.
     *
     * @param placeId identificador do estabelecimento.
     */
    fun load(placeId: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }

        // Tenta sincronizar com remotos (não bloqueia a listagem local)
        runCatching { reviewRepo.refreshPlaceReviews(placeId) }
            .onFailure { e -> _state.update { s -> s.copy(error = e) } }

        // Leitura local final (Room)
        val list = runCatching { reviewRepo.allReviews(placeId) }
            .getOrDefault(emptyList())

        _state.update { it.copy(reviews = list, isLoading = false) }
    }
}
