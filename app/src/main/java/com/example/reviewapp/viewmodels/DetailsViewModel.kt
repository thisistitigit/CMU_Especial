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
        val latestReviews: List<Review> = emptyList()
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun load(placeId: String) = viewModelScope.launch {
        // 1) buscar detalhes locais (ou remoto se implementado no repo)
        val p = runCatching { placeRepo.getDetails(placeId) }.getOrNull()
        _state.update { it.copy(place = p) }

        // 2) últimas 10 reviews
        val reviews = runCatching { reviewRepo.latestReviews(placeId) }.getOrDefault(emptyList())
        _state.update { it.copy(latestReviews = reviews) }
    }

    fun call(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
    }
}
