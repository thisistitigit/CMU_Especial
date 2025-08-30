package com.example.reviewapp.viewmodels
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reviewapp.data.models.Place
import com.example.reviewapp.data.models.Review
import com.example.reviewapp.data.repository.PlaceRepository
import com.example.reviewapp.data.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val placeRepo: PlaceRepository,
    private val reviewRepo: ReviewRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class UiState(
        val place: Place? = null,
        val latestReviews: List<Review> = emptyList(),
        val isLoading: Boolean = false,
        val error: Throwable? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun load(placeId: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }

        val p = runCatching { placeRepo.getDetails(placeId) }
            .onFailure { e -> _state.update { it.copy(error = e) } }
            .getOrNull()

        _state.update { it.copy(place = p) }

        val reviews = runCatching { reviewRepo.latestReviews(placeId) }
            .getOrDefault(emptyList())
        _state.update { it.copy(latestReviews = reviews.take(10), isLoading = false) }
    }

    fun call(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
    }

    fun openOnMap(lat: Double, lng: Double, label: String?) {
        // geo:lat,lng?q=lat,lng(label)
        val uri = if (label.isNullOrBlank())
            Uri.parse("geo:$lat,$lng?q=$lat,$lng")
        else
            Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
    }

    fun getDirections(lat: Double, lng: Double) {
        // Google Maps directions
        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
    }
}
