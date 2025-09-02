package com.example.reviewapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reviewapp.data.dao.PlaceDao
import com.example.reviewapp.data.dao.ReviewDao
import com.example.reviewapp.data.locals.PlaceEntity
import com.example.reviewapp.data.locals.ReviewEntity
import com.example.reviewapp.data.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val placeDao: PlaceDao,
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

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            try {
                // 1) puxa do Firestore e cacheia no Room
                runCatching { reviewRepo.refreshAllReviews(maxToFetch = 2000, pageSize = 500) }

                // 2) lê do Room (offline-first)
                val places  = placeDao.listAll()
                val reviews = reviewDao.listAll()

                // 3) agrega
                val est = aggregateEstablishmentsFromReviews(places, reviews)
                val pas = aggregatePastries(reviews)

                _ui.value = _ui.value.copy(
                    isLoading = false,
                    establishments = est,
                    pastries = pas
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(isLoading = false, error = e.message)
            }
        }
    }


    fun onSelectTab(tab: Tab) {
        _ui.value = _ui.value.copy(tab = tab)
    }
    private fun aggregateEstablishmentsFromReviews(
        places: List<PlaceEntity>,
        reviews: List<ReviewEntity>
    ): List<PlaceRow> {
        val placeById = places.associateBy { it.id }
        val byPlace = reviews.groupBy { it.placeId }

        val rows = byPlace.map { (placeId, r) ->
            val count = r.size
            val avg = r.map { it.stars }.average()
            val p = placeById[placeId]
            PlaceRow(
                placeId = placeId,
                name = p?.name ?: "Local $placeId",
                address = p?.address,
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
