package com.example.reviewapp.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reviewapp.data.dao.PlaceDao
import com.example.reviewapp.data.models.Place
import com.example.reviewapp.data.repository.PlaceRepository
import com.example.reviewapp.data.repository.ReviewRepository
import com.example.reviewapp.network.mappers.toModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val placeDao: PlaceDao,
    private val placeRepo: PlaceRepository,
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
        viewModelScope.launch {
            combine(
                placeDao.flowById(placeId),
                reviewRepo.streamPlaceMetaFromReviews(placeId),
                reviewRepo.streamPlaceReviews(placeId)
            ) { roomPlace, metaFromReviews, reviews ->
                val local = roomPlace?.toModel()
                val merged = mergePlaceFieldWise(local, metaFromReviews)

                val avg = reviews.takeIf { it.isNotEmpty() }?.map { it.stars }?.average() ?: 0.0
                val count = reviews.size

                Triple(merged, Pair(avg, count), reviews)
            }.onStart {
                _state.update { it.copy(isLoading = true, error = null) }
                // 1) garantir que temos detalhes (se não houver em Room, vai à API e cacheia)
                runCatching { placeRepo.getDetails(placeId) }
                    .onSuccess { Log.d(TAG, "getDetails ok para $placeId") }
                    .onFailure { Log.w(TAG, "getDetails falhou (Room/API): ${it.message}") }
                // 2) garantir reviews atuais
                reviewRepo.refreshPlaceReviews(placeId)
            }.catch { e ->
                _state.update { it.copy(isLoading = false, error = e.message) }
            }.collect { (place, metrics, latest) ->
                val (avg, count) = metrics
                _state.update {
                    it.copy(
                        isLoading = false,
                        place = place,
                        internalAvg = avg,
                        internalCount = count,
                        latestReviews = latest
                    )
                }
            }
        }
    }

    private fun String.isPlaceholderName() =
        isBlank() || equals("Estabelecimento", true) || startsWith("Local ")

    private fun preferNonBlank(primary: String?, fallback: String?) =
        primary?.takeIf { it.isNotBlank() } ?: fallback?.takeIf { it.isNotBlank() }

    private fun mergePlaceFieldWise(local: Place?, meta: Place?): Place? {
        if (local == null && meta == null) return null
        val localName = local?.name
        val metaName  = meta?.name
        val bestName =
            if (localName.isNullOrBlank() || localName.isPlaceholderName())
                preferNonBlank(metaName, localName)
            else localName
        val bestAddr = preferNonBlank(local?.address, meta?.address) ?: meta?.address
        val base = local ?: meta!!
        return base.copy(
            name = bestName ?: base.name,
            address = bestAddr ?: base.address
        )
    }

    fun call(number: String) = runCatching { /* ACTION_DIAL na UI */ }
    fun openOnMap(lat: Double, lng: Double, label: String) = Unit
    fun getDirections(lat: Double, lng: Double) = Unit
}
