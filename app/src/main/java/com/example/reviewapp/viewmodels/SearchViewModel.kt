package com.example.reviewapp.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reviewapp.data.models.Place
import com.example.reviewapp.data.repository.PlaceRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.*

/**
 * **VM** de pesquisa/descoberta de locais (mapa + lista).
 *
 * Fluxos:
 * - *Preload* tenta obter localização atual (se permitido) e pesquisa `nearby`;
 * - Mantém duas listas: resultados **no centro de pesquisa** e **perto de mim**;
 * - Aplica **filtro por raio local** em memória (Haversine) e ordena por rating;
 * - Enriquecimento assíncrono com detalhes (nome/telefone/rating) via `enrichPlaces`.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val placeRepo: PlaceRepository,
    private val locationProvider: FusedLocationProviderClient,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    /** Estado observável do ecrã de pesquisa. */
    data class UiState(
        val cameraLatLng: LatLng = LatLng(38.7223, -9.1393),
        val searchCenter: LatLng = LatLng(38.7223, -9.1393),
        val places: List<Place> = emptyList(),

        val nearMeCenter: LatLng? = null,
        val nearMePlaces: List<Place> = emptyList(),

        val radiusMeters: Int = 3000,
        val fetchedRadiusMeters: Int = 0,
        val nearMeFetchedRadiusMeters: Int = 0,
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState(isLoading = true))
    val state: StateFlow<UiState> = _state

    /** Cache bruta para re-aplicar filtros locais sem re-fetch. */
    private var rawSearchPlaces: List<Place> = emptyList()
    private var rawNearMePlaces: List<Place> = emptyList()

    init {
        viewModelScope.launch { preload() }
    }

    /** Distância Haversine (m). */
    private fun haversineMeters(a: LatLng, b: LatLng): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val la1 = Math.toRadians(a.latitude)
        val la2 = Math.toRadians(b.latitude)
        val h = sin(dLat/2).pow(2) + cos(la1) * cos(la2) * sin(dLng/2).pow(2)
        return 2 * R * asin(min(1.0, sqrt(h)))
    }

    /** Filtro local por raio + ordenação por rating (desc). */
    private fun applyLocalRadiusFilter(center: LatLng, radius: Int, list: List<Place>): List<Place> {
        return list.filter { p ->
            haversineMeters(center, LatLng(p.lat, p.lng)) <= radius
        }.sortedByDescending { it.avgRating }
    }

    /** Fetch principal para o centro de pesquisa, com *fallback* de raio. */
    private suspend fun doFetchSearch(center: LatLng, radiusMeters: Int) {
        _state.update { it.copy(isLoading = true, error = null) }

        suspend fun fetch(r: Int) =
            runCatching { placeRepo.nearby(center.latitude, center.longitude, r) }
                .getOrElse { emptyList() }

        var usedRadius = radiusMeters
        var list = fetch(usedRadius)
        if (list.isEmpty() && usedRadius < 6000) { usedRadius = 6000; list = fetch(usedRadius) }

        rawSearchPlaces = list
        _state.update {
            it.copy(
                cameraLatLng = center,
                searchCenter = center,
                fetchedRadiusMeters = usedRadius,
                places = applyLocalRadiusFilter(center, it.radiusMeters.coerceAtMost(usedRadius), rawSearchPlaces),
                isLoading = false, error = null
            )
        }

        // Enriquecimento assíncrono (detalhes/telefone/ratingCount)
        viewModelScope.launch {
            val enriched = runCatching { placeRepo.enrichPlaces(rawSearchPlaces) }.getOrNull() ?: return@launch
            if (enriched !== rawSearchPlaces) {
                rawSearchPlaces = enriched
                _state.update {
                    it.copy(places = applyLocalRadiusFilter(center, it.radiusMeters.coerceAtMost(usedRadius), rawSearchPlaces))
                }
            }
        }
    }

    /** Fetch para "Perto de mim" (idêntico ao de pesquisa). */
    private suspend fun doFetchNearMe(center: LatLng, radiusMeters: Int) {
        suspend fun fetch(r: Int) =
            runCatching { placeRepo.nearby(center.latitude, center.longitude, r) }
                .getOrElse { emptyList() }

        var usedRadius = radiusMeters
        var list = fetch(usedRadius)
        if (list.isEmpty() && usedRadius < 6000) { usedRadius = 6000; list = fetch(usedRadius) }

        rawNearMePlaces = list
        _state.update {
            it.copy(
                nearMeCenter = center,
                nearMeFetchedRadiusMeters = usedRadius,
                nearMePlaces = applyLocalRadiusFilter(center, it.radiusMeters.coerceAtMost(usedRadius), rawNearMePlaces)
            )
        }

        viewModelScope.launch {
            val enriched = runCatching { placeRepo.enrichPlaces(rawNearMePlaces) }.getOrNull() ?: return@launch
            if (enriched !== rawNearMePlaces) {
                rawNearMePlaces = enriched
                _state.update {
                    it.copy(nearMePlaces = applyLocalRadiusFilter(center, it.radiusMeters.coerceAtMost(usedRadius), rawNearMePlaces))
                }
            }
        }
    }

    /**
     * Pré-carregamento inicial: tenta usar localização real se permitido.
     * Em falta, usa o centro por omissão (Lisboa) para ambos os *feeds*.
     */
    private suspend fun preload() {
        val fineGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val defaultCenter = _state.value.cameraLatLng

        if (fineGranted || coarseGranted) {
            try {
                val token = CancellationTokenSource().token
                val loc = try { locationProvider.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token).await() } catch (_: Throwable) { null }
                val best = loc ?: try { locationProvider.lastLocation.await() } catch (_: Throwable) { null }
                if (best != null) {
                    val me = LatLng(best.latitude, best.longitude)
                    doFetchNearMe(me, _state.value.radiusMeters)
                    doFetchSearch(me, _state.value.radiusMeters)
                    return
                }
            } catch (_: Throwable) {
                // Silenciar falhas de localização no arranque
            }
        }

        doFetchNearMe(defaultCenter, _state.value.radiusMeters)
        doFetchSearch(defaultCenter, _state.value.radiusMeters)
    }

    /**
     * Atualiza o raio de pesquisa e decide se é necessário novo fetch
     * (quando o novo raio excede o `fetchedRadiusMeters`).
     */
    fun setRadiusMeters(newRadius: Int) = viewModelScope.launch {
        val st = _state.value
        _state.update { it.copy(radiusMeters = newRadius) }

        if (newRadius > st.fetchedRadiusMeters) {
            doFetchSearch(st.searchCenter, newRadius)
        } else {
            _state.update {
                it.copy(places = applyLocalRadiusFilter(it.searchCenter, newRadius, rawSearchPlaces))
            }
        }

        val meCenter = _state.value.nearMeCenter
        if (meCenter != null) {
            if (newRadius > st.nearMeFetchedRadiusMeters) {
                doFetchNearMe(meCenter, newRadius)
            } else {
                _state.update {
                    it.copy(nearMePlaces = applyLocalRadiusFilter(meCenter, newRadius, rawNearMePlaces))
                }
            }
        }
    }

    /** Recalcula "Perto de mim" com localização atual (se permitido). */
    fun refreshNearMe(radiusMeters: Int = _state.value.radiusMeters) = viewModelScope.launch {
        val fineGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return@launch

        val token = CancellationTokenSource().token
        val loc = runCatching { locationProvider.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token).await() }.getOrNull()
        val best = loc ?: runCatching { locationProvider.lastLocation.await() }.getOrNull() ?: return@launch

        doFetchNearMe(LatLng(best.latitude, best.longitude), radiusMeters)
    }

    /** Recarrega resultados para um novo centro de pesquisa. */
    fun refreshAt(center: LatLng, radiusMeters: Int = _state.value.radiusMeters) = viewModelScope.launch {
        doFetchSearch(center, radiusMeters)
    }
}
