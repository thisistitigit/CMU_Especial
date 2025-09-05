package com.example.reviewapp.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reviewapp.data.dao.PlaceDao
import com.example.reviewapp.data.models.Place
import com.example.reviewapp.data.repository.ReviewRepository
import com.example.reviewapp.network.mappers.toModel // PlaceEntity -> Place
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val placeDao: PlaceDao,
    private val reviewRepo: ReviewRepository
) : ViewModel() {

    private val TAG = "DetailsVM"

    data class UiState(
        val isLoading: Boolean = true,
        val place: Place? = null,
        val internalAvg: Double = 0.0,
        val internalCount: Int = 0,
        val latestReviews: List<com.example.reviewapp.data.models.Review> = emptyList(),
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun load(placeIdRaw: String) {
        val placeId = placeIdRaw.trim()
        Log.d(TAG, "load(placeId='$placeId')")
        viewModelScope.launch {
            combine(
                // 1) Place do Room (pode ser null)
                placeDao.flowById(placeId),
                // 2) Meta do Place derivada das reviews (nome/morada); já tens isto no repo
                reviewRepo.streamPlaceMetaFromReviews(placeId),
                // 3) Reviews do local (para métricas e lista)
                reviewRepo.streamPlaceReviews(placeId)
            ) { roomPlace, metaFromReviews, reviews ->
                val local = roomPlace?.toModel()
                val merged = mergePlace(local, metaFromReviews)

                val avg = if (reviews.isNotEmpty()) reviews.map { it.stars }.average() else 0.0
                val count = reviews.size

                Triple(merged, Pair(avg, count), reviews)
            }
                .onStart {
                    _state.update { it.copy(isLoading = true, error = null) }
                    // puxa remoto para garantir dados atualizados
                    reviewRepo.refreshPlaceReviews(placeId)
                    Log.d(TAG, "refreshPlaceReviews($placeId) dispatched")
                }
                .catch { e ->
                    Log.e(TAG, "load error", e)
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { (place, metrics, latest) ->
                    val (avg, count) = metrics
                    Log.d(TAG, "state update: place='${place?.name}' addr='${place?.address}' avg=$avg count=$count latest=${latest.size}")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            place = place,
                            internalAvg = avg,
                            internalCount = count,
                            latestReviews = latest,
                            error = null
                        )
                    }
                }
        }
    }

    /** Prefere nome/morada vindos das reviews quando o local do Room tem placeholders/vazios. */
    private fun mergePlace(local: Place?, meta: Place?): Place? {
        if (local == null && meta == null) return null
        val base = local ?: meta!!

        fun firstNonBlank(vararg s: String?): String? =
            s.firstOrNull { !it.isNullOrBlank() }

        val mergedName = firstNonBlank(meta?.name, local?.name)
        val mergedAddr = firstNonBlank(meta?.address, local?.address)

        return base.copy(
            name = mergedName ?: base.name,
            address = mergedAddr ?: base.address
        )
    }

    // Ações auxiliares usadas pelo UI (já tens no teu DetailsScreen)
    fun call(number: String) = runCatching { /* start ACTION_DIAL no layer de UI */ }
    fun openOnMap(lat: Double, lng: Double, label: String) = Unit
    fun getDirections(lat: Double, lng: Double) = Unit
}
