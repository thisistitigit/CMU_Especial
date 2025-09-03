package com.example.reviewapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reviewapp.core.ext.requireSignedInUid
import com.example.reviewapp.data.dao.PlaceDao
import com.example.reviewapp.data.repository.PlaceRepository
import com.example.reviewapp.data.repository.ReviewRepository
import com.example.reviewapp.network.mappers.toEntity
import com.example.reviewapp.utils.TimeUtils
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val reviewRepo: ReviewRepository,
    private val placeRepo: PlaceRepository,
    private val placeDao: PlaceDao,
    private val auth: FirebaseAuth
) : ViewModel() {

    data class Row(
        val reviewId: String,
        val placeId: String,
        val placeName: String,
        val pastryName: String,
        val stars: Int,
        val createdAt: Long,
        val photoLocalPath: String?,
        val photoCloudUrl: String?
    )

    data class UiState(
        val isLoading: Boolean = true,
        val items: List<Row> = emptyList(),
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init { observe() }

    private val startedStreams = mutableSetOf<String>()

    init { observe() }

    private fun observe() = viewModelScope.launch {
        val uid = auth.requireSignedInUid()

        reviewRepo.streamUserHistory(uid)
            .combine(placeDao.flowAll()) { reviews, places ->
                val placeById = places.associateBy { it.id }

                // IDs de locais ainda não presentes no Room
                val missing = reviews.map { it.placeId }.toSet() - placeById.keys
                if (missing.isNotEmpty()) {
                    missing.forEach { id ->
                        if (startedStreams.add(id)) {
                            // ► ouvir metadados do local A PARTIR DAS REVIEWS
                            reviewRepo.streamPlaceMetaFromReviews(id)
                                .onEach { place ->
                                    if (place != null) {
                                        placeDao.upsert(place.toEntity(TimeUtils.now()))
                                    }
                                }
                                .launchIn(viewModelScope)
                        }
                    }
                }

                // linhas (fallback até nome chegar)
                reviews.map { r ->
                    Row(
                        reviewId = r.id,
                        placeId = r.placeId,
                        placeName = placeById[r.placeId]?.name ?: "Estabelecimento",
                        pastryName = r.pastryName,
                        stars = r.stars,
                        createdAt = r.createdAt,
                        photoLocalPath = r.photoLocalPath,
                        photoCloudUrl = r.photoCloudUrl
                    )
                }
            }
            .onStart { _ui.update { it.copy(isLoading = true, error = null) } }
            .catch { e -> _ui.update { it.copy(isLoading = false, error = e.message) } }
            .collect { rows -> _ui.update { it.copy(isLoading = false, items = rows) } }
    }
}