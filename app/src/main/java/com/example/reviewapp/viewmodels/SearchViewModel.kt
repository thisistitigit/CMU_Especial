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
        // 1) permissões
        val fineGranted = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return@launch

        try {
            // 2) localização atual
            val token = CancellationTokenSource().token
            val loc = locationProvider
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token)
                .await() ?: return@launch

            // 3) usa o alias nearby do repositório
            val list = placeRepo.nearby(loc.latitude, loc.longitude, radiusMeters)
            _state.update {
                it.copy(cameraLatLng = LatLng(loc.latitude, loc.longitude), places = list)
            }
        } catch (_: SecurityException) {
            // permissões revogadas em runtime — ignora ou expõe estado de erro se precisares
        }
    }

    /** Pesquisa perto de um ponto arbitrário (ex.: centro do mapa) */
    fun refreshAt(center: LatLng, radiusMeters: Int = 250) = viewModelScope.launch {
        val list = placeRepo.nearby(center.latitude, center.longitude, radiusMeters)
        _state.update { it.copy(cameraLatLng = center, places = list) }
    }
    private val _searchState = MutableStateFlow<SearchState>(SearchState.Empty)
    val searchState: StateFlow<SearchState> = _searchState

    fun search(query: String) {
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            try {
                // Sua lógica de busca aqui...
                // Exemplo: usar locationProvider para obter localização
                _searchState.value = SearchState.Success(emptyList())
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    sealed class SearchState {
        object Empty : SearchState()
        object Loading : SearchState()
        data class Success(val results: List<Any>) : SearchState()
        data class Error(val message: String) : SearchState()
    }
}
