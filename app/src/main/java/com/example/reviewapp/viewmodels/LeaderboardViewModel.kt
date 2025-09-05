package com.example.reviewapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reviewapp.data.dao.ReviewDao
import com.example.reviewapp.data.locals.ReviewEntity
import com.example.reviewapp.data.repository.ReviewRepository
import com.example.reviewapp.network.mappers.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val reviewDao: ReviewDao,
    private val reviewRepo: ReviewRepository
) : ViewModel() {

    enum class Tab { ESTABLISHMENTS, PASTRIES }

    data class PlaceRow(
        val placeId: String,
        val name: String,
        val address: String?,
        val avg: Double,
        val count: Int
    )

    data class PastryRow(
        val pastryName: String,
        val avg: Double,
        val count: Int
    )

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

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            runCatching {
                // puxa do Firestore e cacheia no Room
                reviewRepo.refreshAllReviews(maxToFetch = 2000, pageSize = 500)

                // lê do Room e agrega
                val reviews = reviewDao.listAll()
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    establishments = aggregateEstablishmentsFromReviews(reviews),
                    pastries = aggregatePastries(reviews)
                )
            }.onFailure { e ->
                _ui.value = _ui.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun onSelectTab(tab: Tab) { _ui.value = _ui.value.copy(tab = tab) }

    private fun observeLeaderboard() = viewModelScope.launch {
        _ui.value = _ui.value.copy(isLoading = true, error = null)

        reviewRepo.streamAllReviews()
            .map { reviews ->
                val entities = reviews.map { it.toEntity() }
                UiState(
                    isLoading = false,
                    tab = _ui.value.tab,
                    establishments = aggregateEstablishmentsFromReviews(entities),
                    pastries = aggregatePastries(entities)
                )
            }
            .catch { e -> _ui.value = _ui.value.copy(isLoading = false, error = e.message) }
            .collect { state -> _ui.value = state }
    }

    // ---- Aggregations (só com reviews) ----

    private fun aggregateEstablishmentsFromReviews(
        reviews: List<ReviewEntity>
    ): List<PlaceRow> {
        val byPlace = reviews.groupBy { it.placeId.trim() }

        fun latestNonEmptyName(rs: List<ReviewEntity>): String? =
            rs.asSequence()
                .sortedByDescending { it.createdAt }
                .mapNotNull { it.placeName?.trim() }
                .firstOrNull { it.isNotEmpty() }

        fun latestAddress(rs: List<ReviewEntity>): String? =
            rs.asSequence()
                .sortedByDescending { it.createdAt }
                .mapNotNull { it.placeAddress?.trim() }
                .firstOrNull { it.isNotEmpty() }

        val rows = byPlace.map { (placeId, rs) ->
            val count = rs.size
            val avg = rs.map { it.stars }.average()

            val name = latestNonEmptyName(rs) ?: latestAddress(rs) ?: "—"
            val address = latestAddress(rs)

            PlaceRow(
                placeId = placeId,
                name = name,
                address = address,
                avg = avg,
                count = count
            )
        }

        return rows.sortedWith(
            compareByDescending<PlaceRow> { it.avg }
                .thenByDescending { it.count }
                .thenBy { it.name.lowercase() }
        )
    }

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
}
