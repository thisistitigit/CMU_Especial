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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val placeRepo: PlaceRepository,
    private val reviewRepo: ReviewRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class UiState(
        val place: Place? = null,
        val latestReviews: List<Review> = emptyList(),
        val internalAvg: Double = 0.0,
        val internalCount: Int = 0,
        val isLoading: Boolean = false,
        val error: Throwable? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state


    fun load(placeId: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }

        // detalhes do local
        _state.update { it.copy(place = runCatching { placeRepo.getDetails(placeId) }.getOrNull()) }

        // stream em tempo-real das reviews do local (Cloud→Room→UI)
        reviewRepo.streamPlaceReviews(placeId)
            .onEach { all ->
                val latest = all.take(10)
                val count = all.size
                val avg = if (count > 0) all.map { it.stars }.average() else 0.0
                _state.update {
                    it.copy(
                        latestReviews = latest,
                        internalAvg = avg,
                        internalCount = count,
                        isLoading = false
                    )
                }
            }
            .catch { e -> _state.update { it.copy(isLoading = false, error = e) } }
            .launchIn(this)
    }


    fun call(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
    }

    fun openOnMap(lat: Double, lng: Double, label: String?) {
        // geo:lat,lng?q=lat,lng(label)
        val uri = if (label.isNullOrBlank())
            "geo:$lat,$lng?q=$lat,$lng".toUri()
        else
            "geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})".toUri()

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
    }

    fun getDirections(lat: Double, lng: Double) {
        // Google Maps directions
        val uri = "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
    }
}
