package com.example.reviewapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reviewapp.data.dao.ReviewDao
import com.example.reviewapp.data.locals.ReviewEntity
import com.example.reviewapp.data.repository.PlaceRepository
import com.example.reviewapp.data.repository.ReviewRepository
import com.example.reviewapp.network.mappers.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * **VM** do Leaderboard (estabelecimentos e doçarias).
 *
 * Estratégia:
 * - Observa `streamAllReviews()` (limitado no repo) → projeta para entidades Room;
 * - Agrega **por local** e **por doçaria** (média e contagem);
 * - Enriquecer nomes/moradas via `PlaceRepository.enrichIds`;
 * - Oferece `refresh()` para *full sync* (paginado) e recálculo.
 */
@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val reviewDao: ReviewDao,
    private val reviewRepo: ReviewRepository,
    private val placeRepo: PlaceRepository
) : ViewModel() {

    /** Aba ativa da UI. */
    enum class Tab { ESTABLISHMENTS, PASTRIES }

    /** Linha agregada por **place**. */
    data class PlaceRow(
        val placeId: String,
        val name: String,
        val address: String?,
        val avg: Double,
        val count: Int
    )

    /** Linha agregada por **doçaria**. */
    data class PastryRow(
        val pastryName: String,
        val avg: Double,
        val count: Int
    )

    /** Estado completo do ecrã de leaderboard. */
    data class UiState(
        val isLoading: Boolean = true,
        val tab: Tab = Tab.ESTABLISHMENTS,
        val establishments: List<PlaceRow> = emptyList(),
        val pastries: List<PastryRow> = emptyList(),
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init { observeLeaderboard() }

    /** Dispara sincronização paginada e recalcula agregados. */
    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            runCatching {
                reviewRepo.refreshAllReviews(maxToFetch = 2000, pageSize = 500)
                val reviews = reviewDao.listAll()
                val rows = aggregateEstablishmentsFromReviews(reviews)
                val enriched = enrichRowsWithPlaces(rows)
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    establishments = enriched,
                    pastries = aggregatePastries(reviews)
                )
            }.onFailure { e ->
                _ui.value = _ui.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    /** Seleciona a aba ativa. */
    fun onSelectTab(tab: Tab) { _ui.value = _ui.value.copy(tab = tab) }

    /** Observa fluxo global e projeta para [UiState]. */
    private fun observeLeaderboard() = viewModelScope.launch {
        _ui.value = _ui.value.copy(isLoading = true, error = null)

        reviewRepo.streamAllReviews()
            .map { reviews ->
                val entities = reviews.map { it.toEntity() }
                val rows = aggregateEstablishmentsFromReviews(entities)
                val enriched = enrichRowsWithPlaces(rows)
                UiState(
                    isLoading = false,
                    tab = _ui.value.tab,
                    establishments = enriched,
                    pastries = aggregatePastries(entities)
                )
            }
            .catch { e -> _ui.value = _ui.value.copy(isLoading = false, error = e.message) }
            .collect { state -> _ui.value = state }
    }

    /** Agregação por estabelecimento com ordenação: média DESC, contagem DESC, nome ASC. */
    private fun aggregateEstablishmentsFromReviews(
        reviews: List<ReviewEntity>
    ): List<PlaceRow> {
        val byPlace = reviews.groupBy { it.placeId.trim() }

        fun latestNonEmptyName(rs: List<ReviewEntity>): String? =
            rs.asSequence().sortedByDescending { it.createdAt }
                .mapNotNull { it.placeName?.trim() }
                .firstOrNull { it.isNotEmpty() }

        fun latestAddress(rs: List<ReviewEntity>): String? =
            rs.asSequence().sortedByDescending { it.createdAt }
                .mapNotNull { it.placeAddress?.trim() }
                .firstOrNull { it.isNotEmpty() }

        val rows = byPlace.map { (placeId, rs) ->
            val count = rs.size
            val avg = rs.map { it.stars }.average()
            val name = latestNonEmptyName(rs) ?: latestAddress(rs) ?: "—"
            val address = latestAddress(rs)
            PlaceRow(placeId, name, address, avg, count)
        }

        return rows.sortedWith(
            compareByDescending<PlaceRow> { it.avg }
                .thenByDescending { it.count }
                .thenBy { it.name.lowercase() }
        )
    }

    /** Agregação por doçaria (ignora vazios), ordenada por média/contagem/nome. */
    private fun aggregatePastries(reviews: List<ReviewEntity>): List<PastryRow> {
        val byPastry = reviews.groupBy { it.pastryName.trim() }
        val rows = byPastry.mapNotNull { (pastry, list) ->
            if (pastry.isBlank()) null
            else PastryRow(
                pastryName = pastry,
                avg = list.map { it.stars }.average(),
                count = list.size
            )
        }
        return rows.sortedWith(
            compareByDescending<PastryRow> { it.avg }
                .thenByDescending { it.count }
                .thenBy { it.pastryName.lowercase() }
        )
    }

    /** Enriquecimento com dados de Places persistidos/API. */
    private suspend fun enrichRowsWithPlaces(rows: List<PlaceRow>): List<PlaceRow> {
        if (rows.isEmpty()) return rows
        val ids = rows.map { it.placeId }.distinct()
        val map = placeRepo.enrichIds(ids)
        return rows.map { r ->
            val p = map[r.placeId]
            r.copy(
                name = when {
                    r.name == "—" || r.name.isBlank() -> p?.name ?: r.name
                    else -> r.name
                },
                address = r.address ?: p?.address
            )
        }
    }
}
