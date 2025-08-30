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

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val placeRepo: PlaceRepository,
    private val locationProvider: FusedLocationProviderClient,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    data class UiState(
        val cameraLatLng: LatLng = LatLng(38.7223, -9.1393), // Lisboa por defeito
        val places: List<Place> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState(isLoading = true))
    val state: StateFlow<UiState> = _state

    init {
        // Primeiro carregamento: tenta localização atual, senão faz fallback para o centro por defeito
        viewModelScope.launch {
            preload()
        }
    }

    private suspend fun preload(radiusMeters: Int = 250) {
        logD("VM.preload - start")
        _state.update { it.copy(isLoading = true, error = null) }

        val fineGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            // Temos permissão — tenta posição atual
            try {
                // tenta localização atual; se vier null, tenta lastLocation
                val token = CancellationTokenSource().token
                val loc = try {
                    locationProvider.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token).await()
                } catch (_: Throwable) { null }

                val best = loc ?: try { locationProvider.lastLocation.await() } catch (_: Throwable) { null }

                if (best != null) {
                    val lat = best.latitude
                    val lng = best.longitude
                    val list = placeRepo.nearby(lat, lng, radiusMeters)
                    _state.update {
                        it.copy(
                            cameraLatLng = LatLng(lat, lng),
                            places = list,
                            isLoading = false,
                            error = null
                        )
                    }
                    logD("VM - using ${if (loc!=null) "currentLocation" else "lastLocation"} ($lat,$lng) -> ${list.size} places")
                    return
                }
            } catch (t: Throwable) {
                logE("VM.preload - using current location failed: ${t.message}", t)
            }
        }

        // Sem permissão ou falhou localização → fallback para o centro por defeito
        try {
            val center = _state.value.cameraLatLng
            val list = placeRepo.nearby(center.latitude, center.longitude, radiusMeters)
            _state.update { it.copy(places = list, isLoading = false, error = null) }
            logD("VM.preload - fallback with default center (${center.latitude},${center.longitude}) -> ${list.size} places")
        } catch (t: Throwable) {
            logE("VM.preload - fallback failed: ${t.message}", t)
            _state.update { it.copy(isLoading = false, error = t.message ?: "unknown") }
        }
    }

    /** Pesquisa perto da localização atual do dispositivo (requer permissão). */
    fun refresh(radiusMeters: Int = 250) = viewModelScope.launch {
        logD("VM.refresh(r=$radiusMeters) - checking permissions")
        val fineGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            logD("VM.refresh - NO PERMISSION, abort")
            return@launch
        }

        _state.update { it.copy(isLoading = true, error = null) }
        try {
            val token = CancellationTokenSource().token
            val loc = locationProvider.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token).await()
            if (loc == null) {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            val list = placeRepo.nearby(loc.latitude, loc.longitude, radiusMeters)
            _state.update {
                it.copy(
                    cameraLatLng = LatLng(loc.latitude, loc.longitude),
                    places = list,
                    isLoading = false,
                    error = null
                )
            }
            logD("VM.refresh - repo returned ${list.size} places")
        } catch (se: SecurityException) {
            logE("VM.refresh - SecurityException ${se.message}", se)
            _state.update { it.copy(isLoading = false, error = se.message) }
        } catch (t: Throwable) {
            logE("VM.refresh - FAILED: ${t.message}", t)
            _state.update { it.copy(isLoading = false, error = t.message) }
        }
    }

    fun refreshAt(center: LatLng, radiusMeters: Int = 250) = viewModelScope.launch {
        logD("VM.refreshAt(center=${center.latitude},${center.longitude}, r=$radiusMeters)")
        _state.update { it.copy(isLoading = true, error = null) }
        try {
            val list = placeRepo.nearby(center.latitude, center.longitude, radiusMeters)
            _state.update { it.copy(cameraLatLng = center, places = list, isLoading = false, error = null) }
            logD("VM.refreshAt - repo returned ${list.size} places")
        } catch (t: Throwable) {
            logE("VM.refreshAt - FAILED: ${t.message}", t)
            _state.update { it.copy(isLoading = false, error = t.message) }
        }
    }
}
