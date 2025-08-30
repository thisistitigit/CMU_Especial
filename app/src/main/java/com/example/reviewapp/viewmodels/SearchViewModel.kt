// com/example/reviewapp/viewmodels/SearchViewModel.kt
package com.example.reviewapp.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reviewapp.data.models.Place
import com.example.reviewapp.data.repository.PlaceRepository
import com.example.reviewapp.utils.logD
import com.example.reviewapp.utils.logE
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

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val placeRepo: PlaceRepository,
    private val locationProvider: FusedLocationProviderClient,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class UiState(
        val cameraLatLng: LatLng = LatLng(38.7223, -9.1393), // Lisboa por defeito
        val searchCenter: LatLng = LatLng(38.7223, -9.1393),
        val radiusMeters: Int = 3000,           // RAIO ATUAL (UI)
        val fetchedRadiusMeters: Int = 0,       // último raio usado em fetch
        val places: List<Place> = emptyList(),  // filtrado por radiusMeters
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState(isLoading = true))
    val state: StateFlow<UiState> = _state

    // Mantemos o "resultado bruto" do último fetch para poder encolher o raio sem novo request
    private var rawPlaces: List<Place> = emptyList()

    init {
        viewModelScope.launch { preload() }
    }

    private fun haversineMeters(a: LatLng, b: LatLng): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val la1 = Math.toRadians(a.latitude)
        val la2 = Math.toRadians(b.latitude)
        val h = sin(dLat/2).pow(2) + cos(la1) * cos(la2) * sin(dLng/2).pow(2)
        return 2 * R * asin(min(1.0, sqrt(h)))
    }

    private fun applyLocalRadiusFilter(center: LatLng, radius: Int, list: List<Place>): List<Place> {
        return list.filter { p ->
            haversineMeters(center, LatLng(p.lat, p.lng)) <= radius
        }.sortedByDescending { it.avgRating }
    }

    private suspend fun doFetch(center: LatLng, radiusMeters: Int) {
        _state.update { it.copy(isLoading = true, error = null) }

        suspend fun fetch(r: Int): List<Place> =
            runCatching { placeRepo.nearby(center.latitude, center.longitude, r) }
                .onFailure { logE("VM.fetch fail r=$r: ${it.message}", it) }
                .getOrElse { emptyList() }

        // 1ª tentativa com o raio pedido
        var usedRadius = radiusMeters
        var list = fetch(usedRadius)

        // Fallback: se vazio, sobe o raio para 6000m (ou mantém maior)
        if (list.isEmpty() && usedRadius < 6000) {
            usedRadius = 6000
            list = fetch(usedRadius)
            logD("VM.fetch fallback to radius=$usedRadius -> ${list.size} places")
        }

        rawPlaces = list
        _state.update {
            it.copy(
                cameraLatLng = center,
                searchCenter = center,
                fetchedRadiusMeters = usedRadius,
                // filtra localmente com o raio visível (se for menor que o usado no fetch)
                places = applyLocalRadiusFilter(center, it.radiusMeters.coerceAtMost(usedRadius), rawPlaces),
                isLoading = false,
                error = null
            )
        }
        logD("VM.fetch OK center=$center rUsed=$usedRadius raw=${rawPlaces.size} shown=${_state.value.places.size}")
    }


    private suspend fun preload() {
        logD("VM.preload - start")
        val fineGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            try {
                val token = CancellationTokenSource().token
                val loc = try { locationProvider.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token).await() } catch (_: Throwable) { null }
                val best = loc ?: try { locationProvider.lastLocation.await() } catch (_: Throwable) { null }
                if (best != null) {
                    val center = LatLng(best.latitude, best.longitude)
                    return doFetch(center, _state.value.radiusMeters)
                }
            } catch (t: Throwable) {
                logE("VM.preload location fail: ${t.message}", t)
            }
        }

        // fallback default center
        val center = _state.value.cameraLatLng
        doFetch(center, _state.value.radiusMeters)
    }

    /** Atualiza o RAIO. Se aumentar para além do último fetch, faz novo fetch; se diminuir, filtra localmente. */
    fun setRadiusMeters(newRadius: Int) = viewModelScope.launch {
        val st = _state.value
        _state.update { it.copy(radiusMeters = newRadius) }

        if (newRadius > st.fetchedRadiusMeters) {
            // precisamos de expandir a área → fetch ao repositório
            doFetch(st.searchCenter, newRadius)
        } else {
            // apenas encolher → filtra localmente
            _state.update {
                it.copy(places = applyLocalRadiusFilter(it.searchCenter, newRadius, rawPlaces))
            }
        }
    }

    /** Pesquisa perto da localização atual do dispositivo (requer permissão). */
    fun refresh(radiusMeters: Int = _state.value.radiusMeters) = viewModelScope.launch {
        logD("VM.refresh(r=$radiusMeters)")
        val fineGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return@launch

        val token = CancellationTokenSource().token
        val loc = runCatching { locationProvider.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token).await() }.getOrNull()
        val best = loc ?: runCatching { locationProvider.lastLocation.await() }.getOrNull()
        if (best == null) return@launch

        doFetch(LatLng(best.latitude, best.longitude), radiusMeters)
    }

    /** Pesquisa na área do mapa (centro atual). */
    fun refreshAt(center: LatLng, radiusMeters: Int = _state.value.radiusMeters) = viewModelScope.launch {
        logD("VM.refreshAt(center=${center.latitude},${center.longitude}, r=$radiusMeters)")
        doFetch(center, radiusMeters)
    }
}
