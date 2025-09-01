package com.example.reviewapp.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reviewapp.data.models.Review
import com.example.reviewapp.data.repository.ReviewRepository
import com.example.reviewapp.utils.ReviewRules
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ReviewFormViewModel @Inject constructor(
    private val reviewRepo: ReviewRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class UiState(
        val placeId: String = "",
        val userId: String = "",
        val userName: String = "",
        val pastryName: String = "",
        val stars: Int = 0,
        val comment: String = "",
        val photoLocalPath: String? = null,
        val photoCloudUrl: String? = null,
        val distanceMeters: Double? = null,
        val lastReviewAt: Long? = null,
        val rulesOk: Boolean = false,
        val canSubmit: Boolean = false,
        val isSubmitting: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    /* -------- setters básicos usados pelo ecrã -------- */

    fun init(placeId: String, userId: String, userName: String) {
        _state.update { it.copy(placeId = placeId, userId = userId, userName = userName) }
        recompute()
    }

    fun onPastryChanged(value: String) {
        _state.update { it.copy(pastryName = value) }
        recompute()
    }

    fun onStarsChanged(value: Int) {
        _state.update { it.copy(stars = value.coerceIn(0, 5)) }
        recompute()
    }

    fun onCommentChanged(value: String) {
        _state.update { it.copy(comment = value) }
        recompute()
    }

    fun setDistanceMeters(value: Double?) {
        _state.update { it.copy(distanceMeters = value) }
        recompute()
    }

    fun setLastReviewAt(value: Long?) {
        _state.update { it.copy(lastReviewAt = value) }
        recompute()
    }

    /* -------- ações (no-ops seguros para já) -------- */

    fun capturePhoto() {
        // Normalmente isto dispara um evento para a UI abrir CameraX.
        // Mantemos no-op para não rebentar compilação.
    }

    fun pickPhoto() {
        // Normalmente isto dispara um picker na UI.
    }

    fun setPhotoLocalPath(path: String?) {
        _state.update { it.copy(photoLocalPath = path) }
    }

    fun submit() = viewModelScope.launch {
        val s = _state.value
        if (!s.rulesOk || !s.canSubmit) return@launch

        _state.update { it.copy(isSubmitting = true) }

        val review = Review(
            placeId = s.placeId,
            userId = s.userId,
            userName = s.userName,
            pastryName = s.pastryName,
            stars = s.stars,
            comment = s.comment,
            photoLocalPath = s.photoLocalPath,
            photoCloudUrl = s.photoCloudUrl,
            createdAt = System.currentTimeMillis(),
            id = java.util.UUID.randomUUID().toString()
        )

        reviewRepo.addReview(review)

        _state.update {
            it.copy(
                pastryName = "",
                stars = 0,
                comment = "",
                photoLocalPath = null,
                photoCloudUrl = null,
                isSubmitting = false
            )
        }
        recompute()
    }

    fun warmupRules(distanceMeters: Double?) = viewModelScope.launch {
        val uid = _state.value.userId
        val last = if (uid.isNotBlank()) reviewRepo.history(uid).firstOrNull()?.createdAt else null
        _state.update { it.copy(distanceMeters = distanceMeters, lastReviewAt = last) }
        recompute()
    }

    /* -------- lógica de regras e validação -------- */

    private fun recompute() {
        val s = _state.value
        val now = System.currentTimeMillis()
        val dist = s.distanceMeters
        val rulesOk = if (dist == null) {
            false
        } else {
            ReviewRules.canReview(dist, s.lastReviewAt, now)
        }
        val canSubmit = s.pastryName.isNotBlank() && s.stars in 1..5 && s.comment.isNotBlank()
        _state.update { it.copy(rulesOk = rulesOk, canSubmit = canSubmit) }
    }
}
