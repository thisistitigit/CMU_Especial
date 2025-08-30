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
        val places: List<Place> = emptyList()
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    /** Pesquisa perto da localização atual do dispositivo */
    fun refresh(radiusMeters: Int = 250) = viewModelScope.launch {
        logD("VM.refresh(r=$radiusMeters) - checking permissions")
        val fineGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        logD("VM.refresh - perms fine=$fineGranted coarse=$coarseGranted")
        if (!fineGranted && !coarseGranted) {
            logD("VM.refresh - NO PERMISSION, abort")
            return@launch
        }

        try {
            val token = CancellationTokenSource().token
            logD("VM.refresh - requesting currentLocation")
            val loc = locationProvider.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token).await()
            if (loc == null) {
                logD("VM.refresh - location null, abort")
                return@launch
            }
            logD("VM.refresh - got location: ${loc.latitude}, ${loc.longitude}")

            val list = placeRepo.nearby(loc.latitude, loc.longitude, radiusMeters)
            logD("VM.refresh - repo returned ${list.size} places")
            _state.update {
                it.copy(cameraLatLng = LatLng(loc.latitude, loc.longitude), places = list)
            }
        } catch (se: SecurityException) {
            logE("VM.refresh - SecurityException (perms were revoked?) ${se.message}", se)
        } catch (t: Throwable) {
            logE("VM.refresh - FAILED: ${t.message}", t)
        }
    }

    fun refreshAt(center: LatLng, radiusMeters: Int = 250) = viewModelScope.launch {
        logD("VM.refreshAt(center=${center.latitude},${center.longitude}, r=$radiusMeters)")
        try {
            val list = placeRepo.nearby(center.latitude, center.longitude, radiusMeters)
            logD("VM.refreshAt - repo returned ${list.size} places")
            _state.update { it.copy(cameraLatLng = center, places = list) }
        } catch (t: Throwable) {
            logE("VM.refreshAt - FAILED: ${t.message}", t)
        }
    }
    sealed class SearchState {
        object Empty : SearchState()
        object Loading : SearchState()
        data class Success(val results: List<Any>) : SearchState()
        data class Error(val message: String) : SearchState()
    }
}
