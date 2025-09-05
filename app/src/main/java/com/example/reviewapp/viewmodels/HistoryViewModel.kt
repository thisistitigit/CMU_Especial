package com.example.reviewapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reviewapp.core.ext.requireSignedInUid
import com.example.reviewapp.data.repository.ReviewRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val reviewRepo: ReviewRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    data class Row(
        val reviewId: String,
        val placeId: String,
        val placeName: String,     // vem da própria review
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

    private fun observe() = viewModelScope.launch {
        val uid = auth.requireSignedInUid()

        reviewRepo.streamUserHistory(uid)
            .onStart { _ui.update { it.copy(isLoading = true, error = null) } }
            .catch { e -> _ui.update { it.copy(isLoading = false, error = e.message) } }
            .collect { reviews ->
                // ordenar mais recentes primeiro e usar name/address diretamente da review
                val rows = reviews
                    .sortedByDescending { it.createdAt }
                    .map { r ->
                        Row(
                            reviewId = r.id,
                            placeId = r.placeId.trim(),
                            placeName = (r.placeName?.trim())
                                ?: (r.placeAddress?.trim() ?: "—"),
                            pastryName = r.pastryName,
                            stars = r.stars,
                            createdAt = r.createdAt,
                            photoLocalPath = r.photoLocalPath,
                            photoCloudUrl = r.photoCloudUrl
                        )
                    }

                _ui.update { it.copy(isLoading = false, items = rows) }
            }
    }
}
