package com.example.reviewapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reviewapp.data.models.Review
import com.example.reviewapp.data.repository.ReviewRepository
import com.example.reviewapp.utils.ReviewRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ReviewFormViewModel @Inject constructor(
    private val reviewRepo: ReviewRepository
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

        val userLat: Double? = null,
        val userLng: Double? = null,
        val distanceMeters: Double? = null,
        val lastReviewAt: Long? = null,
        val rulesOk: Boolean = false,
        val ruleMessage: String? = null,

        val canSubmit: Boolean = false,
        val isSubmitting: Boolean = false,
        val isLocLoading: Boolean = false,
        val hasLocationPermission: Boolean = false,
        val isLocationEnabled: Boolean = true,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun init(placeId: String, userId: String, userName: String) {
        _state.update { it.copy(placeId = placeId, userId = userId, userName = userName) }
        recompute()
    }
    sealed interface Event { data object Submitted : Event }
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()
    fun onPastryChanged(value: String) { _state.update { it.copy(pastryName = value) }; recompute() }
    fun onStarsChanged(value: Int) { _state.update { it.copy(stars = value.coerceIn(0, 5)) }; recompute() }
    fun onCommentChanged(value: String) { _state.update { it.copy(comment = value) }; recompute() }

    fun setUserLocation(lat: Double?, lng: Double?) { _state.update { it.copy(userLat = lat, userLng = lng) }; recompute() }
    fun setDistanceMeters(value: Double?) { _state.update { it.copy(distanceMeters = value) }; recompute() }

    fun warmupRules(distanceMeters: Double?) = viewModelScope.launch {
        val uid = _state.value.userId
        val last = if (uid.isNotBlank()) reviewRepo.lastReviewAtByUser(uid) else null
        _state.update { it.copy(distanceMeters = distanceMeters, lastReviewAt = last) }
        recompute()
    }

    fun setPhotoLocalPath(path: String?) { _state.update { it.copy(photoLocalPath = path) } }
    fun setLocLoading(loading: Boolean) { _state.update { it.copy(isLocLoading = loading) }; recompute() }
    fun setLocationPermission(has: Boolean) { _state.update { it.copy(hasLocationPermission = has) }; recompute() }
    fun setLocationEnabled(enabled: Boolean) { _state.update { it.copy(isLocationEnabled = enabled) }; recompute() }


    suspend fun submit(): Boolean {
        val s = _state.value
        val uid = s.userId
        val lat = s.userLat
        val lng = s.userLng
        val now = System.currentTimeMillis()

        if (uid.isBlank()) { _state.update { it.copy(ruleMessage = "error_not_authenticated") }; return false }
        if (lat == null || lng == null) { _state.update { it.copy(ruleMessage = "error_enable_location") }; return false }
        if (!s.canSubmit) { _state.update { it.copy(ruleMessage = "error_fill_fields") }; return false }
        if (!s.rulesOk) return false

        _state.update { it.copy(isSubmitting = true, ruleMessage = null) }

        val review = Review(
            id = java.util.UUID.randomUUID().toString(),
            placeId = s.placeId,
            userId = uid,
            userName = s.userName,
            pastryName = s.pastryName,
            stars = s.stars,
            comment = s.comment,
            photoLocalPath = s.photoLocalPath,
            photoCloudUrl = s.photoCloudUrl,
            createdAt = now
        )

        return try {
            reviewRepo.addReview(review, userLat = lat, userLng = lng, now = now)

            // emite o evento de sucesso (antes ou depois de limpar o formulário)
            _events.tryEmit(Event.Submitted)

            _state.update {
                it.copy(
                    pastryName = "", stars = 0, comment = "",
                    photoLocalPath = null, photoCloudUrl = null,
                    isSubmitting = false, ruleMessage = null
                )
            }
            recompute()
            true
        } catch (e: Exception) {
            val msgKey = when (e.message) {
                "TOO_FAR" -> "error_too_far"
                "TOO_SOON" -> "error_too_soon"
                else -> "error_submit_generic"
            }
            _state.update { it.copy(isSubmitting = false, ruleMessage = msgKey, rulesOk = false) }
            false
        }
    }
    private fun recompute() {
        val s = _state.value
        val now = System.currentTimeMillis()
        val dist = s.distanceMeters

        val (ok, messageKey) = when {
            !s.hasLocationPermission -> false to "hint_grant_location_permission"
            !s.isLocationEnabled -> false to "hint_enable_gps"
            s.isLocLoading -> false to "hint_fetching_location"
            dist == null -> false to "hint_fetching_location"
            dist > ReviewRules.MIN_DISTANCE_METERS ->
                false to "error_too_far_live"
            (s.lastReviewAt != null) && (now - s.lastReviewAt < ReviewRules.MIN_INTERVAL_MINUTES * 60_000) ->
                false to "error_too_soon_live"
            else -> true to null
        }

        val canSubmit = s.pastryName.isNotBlank() && s.stars in 1..5 && s.comment.isNotBlank()
        _state.update { it.copy(rulesOk = ok, canSubmit = canSubmit, ruleMessage = messageKey) }
    }
}
