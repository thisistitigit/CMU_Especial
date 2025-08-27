package com.example.cmu_especial.ui.screens.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cmu_especial.domain.model.GeoPoint
import com.example.cmu_especial.domain.model.Review
import com.example.cmu_especial.domain.usecase.AddReview
import com.example.cmu_especial.domain.usecase.CanUserReviewNow
import com.example.cmu_especial.domain.usecase.GetEstablishmentDetails
import com.example.cmu_especial.domain.repository.UserRepository
import com.example.cmu_especial.services.LocationTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewUiState(
    val isLoading: Boolean = true,
    val userId: String = "",
    val userName: String = "",
    val establishmentId: String = "",
    val establishmentLocation: GeoPoint? = null,
    val currentLocation: GeoPoint? = null,
    val error: String? = null
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val canReviewNow: CanUserReviewNow,
    private val addReview: AddReview,
    private val getDetails: GetEstablishmentDetails,
    private val userRepo: UserRepository,
    private val locationTracker: LocationTracker
) : ViewModel() {

    val ui = MutableStateFlow(ReviewUiState())

    fun load(establishmentId: String) {
        viewModelScope.launch {
            ui.update {
                it.copy(
                    isLoading = true,
                    userId = userRepo.currentUserId(),
                    userName = userRepo.currentUserName(),
                    establishmentId = establishmentId,
                    currentLocation = locationTracker.current.value
                )
            }
            runCatching { getDetails(establishmentId) }
                .onSuccess { est ->
                    ui.update {
                        it.copy(
                            isLoading = false,
                            establishmentLocation = est.location,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    ui.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    suspend fun submit(review: Review): Boolean {
        val cur = ui.value.currentLocation
        val est = ui.value.establishmentLocation
        if (cur == null || est == null) return false
        if (!canReviewNow(review.userId, review.establishmentId, cur, est)) return false
        addReview(review)
        return true
    }
}
