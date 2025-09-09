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

/**
 * **VM** da página de detalhes de um *place*.
 *
 * Combina 3 fontes reativas:
 * - Room: `placeDao.flowById(placeId)` (dados persistidos/cached);
 * - Firestore: `streamPlaceMetaFromReviews(placeId)` (nome/morada derivada de reviews);
 * - Firestore/Room: `streamPlaceReviews(placeId)` (últimas reviews para média interna).
 *
 * Também dispara `placeRepo.getDetails(placeId)` **onStart** para enriquecer Room via API.
 */
@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val placeDao: PlaceDao,
    private val placeRepo: PlaceRepository,
    private val reviewRepo: ReviewRepository
) : ViewModel() {

    private val TAG = "DetailsVM"

    /** Estado enriquecido para o ecrã de detalhes. */
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

    /**
     * Observa e mescla fontes para o [placeIdRaw].
     *
     * - Prefere **nome/morada** não-vazios (evita placeholders).
     * - Calcula média interna a partir das reviews locais.
     */
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
                runCatching { placeRepo.getDetails(placeId) }
                    .onSuccess { Log.d(TAG, "getDetails ok para $placeId") }
                    .onFailure { Log.w(TAG, "getDetails falhou (Room/API): ${it.message}") }
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

    /** Heurística para detetar placeholders típicos. */
    private fun String.isPlaceholderName() =
        isBlank() || equals("Estabelecimento", true) || startsWith("Local ")

    private fun preferNonBlank(primary: String?, fallback: String?) =
        primary?.takeIf { it.isNotBlank() } ?: fallback?.takeIf { it.isNotBlank() }

    /** Merge campo-a-campo de [Place] (Room vs. meta-reviews). */
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
        return base.copy(name = bestName ?: base.name, address = bestAddr ?: base.address)
    }

    /** _Stubs_ acionados pela UI (navegação externa). */
    fun call(number: String) = runCatching { }
}
