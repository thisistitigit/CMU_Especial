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
        // mapa / pesquisa
        val cameraLatLng: LatLng = LatLng(38.7223, -9.1393), // Lisboa por defeito
        val searchCenter: LatLng = LatLng(38.7223, -9.1393),
        val places: List<Place> = emptyList(),          // resultados de PESQUISA (centro = searchCenter)

        // “perto de si” (GPS)
        val nearMeCenter: LatLng? = null,               // centro real do utilizador (GPS)
        val nearMePlaces: List<Place> = emptyList(),    // resultados fixos ao GPS

        // controlos comuns
        val radiusMeters: Int = 3000,                   // raio desejado no UI
        val fetchedRadiusMeters: Int = 0,               // último raio usado para PESQUISA
        val nearMeFetchedRadiusMeters: Int = 0,         // último raio usado para NEAR ME
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState(isLoading = true))
    val state: StateFlow<UiState> = _state

    // caches brutas independentes
    private var rawSearchPlaces: List<Place> = emptyList()
    private var rawNearMePlaces: List<Place> = emptyList()

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

    // ---------- FETCHES SEPARADOS ----------

    private suspend fun doFetchSearch(center: LatLng, radiusMeters: Int) {
        _state.update { it.copy(isLoading = true, error = null) }

        suspend fun fetch(r: Int): List<Place> =
            runCatching { placeRepo.nearby(center.latitude, center.longitude, r) }
                .onFailure { logE("VM.fetch SEARCH r=$r: ${it.message}", it) }
                .getOrElse { emptyList() }

        var usedRadius = radiusMeters
        var list = fetch(usedRadius)
        if (list.isEmpty() && usedRadius < 6000) {
            usedRadius = 6000
            list = fetch(usedRadius)
            logD("VM.fetch SEARCH fallback radius=$usedRadius -> ${list.size} places")
        }

        rawSearchPlaces = list
        _state.update {
            it.copy(
                cameraLatLng = center,
                searchCenter = center,
                fetchedRadiusMeters = usedRadius,
                places = applyLocalRadiusFilter(center, it.radiusMeters.coerceAtMost(usedRadius), rawSearchPlaces),
                isLoading = false,
                error = null
            )
        }
    }

    private suspend fun doFetchNearMe(center: LatLng, radiusMeters: Int) {
        // não bloquear o ecrã todo por causa do near me
        suspend fun fetch(r: Int): List<Place> =
            runCatching { placeRepo.nearby(center.latitude, center.longitude, r) }
                .onFailure { logE("VM.fetch NEARME r=$r: ${it.message}", it) }
                .getOrElse { emptyList() }

        var usedRadius = radiusMeters
        var list = fetch(usedRadius)
        if (list.isEmpty() && usedRadius < 6000) {
            usedRadius = 6000
            list = fetch(usedRadius)
            logD("VM.fetch NEARME fallback radius=$usedRadius -> ${list.size} places")
        }

        rawNearMePlaces = list
        _state.update {
            it.copy(
                nearMeCenter = center,
                nearMeFetchedRadiusMeters = usedRadius,
                nearMePlaces = applyLocalRadiusFilter(center, it.radiusMeters.coerceAtMost(usedRadius), rawNearMePlaces)
            )
        }
    }

    // ---------- BOOTSTRAP ----------

    private suspend fun preload() {
        logD("VM.preload - start")
        val fineGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val defaultCenter = _state.value.cameraLatLng

        // 1) tentar GPS para NEAR ME
        if (fineGranted || coarseGranted) {
            try {
                val token = CancellationTokenSource().token
                val loc = try { locationProvider.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token).await() } catch (_: Throwable) { null }
                val best = loc ?: try { locationProvider.lastLocation.await() } catch (_: Throwable) { null }
                if (best != null) {
                    val me = LatLng(best.latitude, best.longitude)
                    // fetch near-me em paralelo com a pesquisa default
                    doFetchNearMe(me, _state.value.radiusMeters)
                    // também coloca pesquisa por defeito a partir do mesmo sítio (opcional)
                    doFetchSearch(me, _state.value.radiusMeters)
                    return
                }
            } catch (t: Throwable) {
                logE("VM.preload location fail: ${t.message}", t)
            }
        }

        // 2) fallback: Lisboa
        doFetchNearMe(defaultCenter, _state.value.radiusMeters)
        doFetchSearch(defaultCenter, _state.value.radiusMeters)
    }

    // ---------- API PÚBLICA ----------

    /** Atualiza o RAIO. Aplica aos dois conjuntos:
     *  - se novo raio excede o último fetch respetivo, volta a ir buscar; senão, filtra localmente. */
    fun setRadiusMeters(newRadius: Int) = viewModelScope.launch {
        val st = _state.value
        _state.update { it.copy(radiusMeters = newRadius) }

        // SEARCH
        if (newRadius > st.fetchedRadiusMeters) {
            doFetchSearch(st.searchCenter, newRadius)
        } else {
            _state.update {
                it.copy(places = applyLocalRadiusFilter(it.searchCenter, newRadius, rawSearchPlaces))
            }
        }

        // NEAR ME
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

    /** Força refresh “perto de mim” via GPS (requer permissão). */
    fun refreshNearMe(radiusMeters: Int = _state.value.radiusMeters) = viewModelScope.launch {
        val fineGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return@launch

        val token = CancellationTokenSource().token
        val loc = runCatching { locationProvider.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token).await() }.getOrNull()
        val best = loc ?: runCatching { locationProvider.lastLocation.await() }.getOrNull() ?: return@launch

        doFetchNearMe(LatLng(best.latitude, best.longitude), radiusMeters)
    }

    /** Pesquisa numa área específica (geocoding ou mapa). NÃO mexe no “perto de si”. */
    fun refreshAt(center: LatLng, radiusMeters: Int = _state.value.radiusMeters) = viewModelScope.launch {
        logD("VM.refreshAt(center=${center.latitude},${center.longitude}, r=$radiusMeters)")
        doFetchSearch(center, radiusMeters)
    }
}
