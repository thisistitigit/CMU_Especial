package com.example.reviewapp.ui.screens

import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.R
import com.example.reviewapp.ui.components.AppHeader
import com.example.reviewapp.ui.components.LeaderboardHorizontalSection
import com.example.reviewapp.ui.components.LocationPermissionGate
import com.example.reviewapp.ui.components.OfflineBanner
import com.example.reviewapp.ui.components.PlaceHorizontalSection
import com.example.reviewapp.viewmodels.LeaderboardViewModel
import com.example.reviewapp.viewmodels.SearchViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * **Home**: ponto de entrada com:
 * - *Search box* (geocoding textual para coordenadas),
 * - Secção “Perto de mim” (se permissão localização) e resultados de pesquisa,
 * - Destaques de **leaderboard** (melhor classificados).
 *
 * ### Interações
 * - Ao conceder permissões, `LocationPermissionGate` chama `vm.refreshNearMe()`.
 * - `TextField` com ação IME *Search* faz `geocodeFirstLatLng(...)` e atualiza o *viewport*.
 *
 * ### Estados visuais
 * - Mostra *spinner* compacto quando `isSearching` ou `vm.state.isLoading` ou `lb.isLoading`.
 *
 * @param vm ViewModel de busca/descoberta.
 * @param leaderboardVm ViewModel do leaderboard (top estabelecimentos).
 * @param onOpenDetails Navegar para detalhe do estabelecimento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: SearchViewModel = hiltViewModel(),
    leaderboardVm: LeaderboardViewModel = hiltViewModel(),
    onOpenDetails: (String) -> Unit,
) {
    val ui by vm.state.collectAsState()
    val lb by leaderboardVm.ui.collectAsState()

    var query by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var showingSearch by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    LocationPermissionGate(
        autoShowIfNeeded = true,
        onAllGranted = { vm.refreshNearMe() }
    )

    LaunchedEffect(query, isSearching) {
        if (query.isBlank() && !isSearching) showingSearch = false
    }

    Scaffold(
        topBar = { AppHeader(title = stringResource(R.string.home_title)) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.search_location_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = ""; showingSearch = false }) {
                                Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (query.isBlank()) return@KeyboardActions
                            scope.launch {
                                isSearching = true
                                val latLng = geocodeFirstLatLng(ctx, query)
                                isSearching = false
                                if (latLng != null) {
                                    vm.refreshAt(latLng)
                                    showingSearch = true
                                }
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            OfflineBanner()
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                val searchingOrLoading = isSearching || ui.isLoading || lb.isLoading
                if (searchingOrLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            if (ui.error != null && ui.places.isEmpty() && ui.nearMePlaces.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.home_error))
                }
                return@Column
            }

            val nearOrdered = ui.nearMePlaces.sortedByDescending { it.avgRating }
            val searchOrdered = ui.places.sortedByDescending { it.avgRating }

            if (showingSearch) {
                if (searchOrdered.isNotEmpty()) {
                    PlaceHorizontalSection(
                        title = stringResource(R.string.home_search_results_title),
                        places = searchOrdered.take(30),
                        onPlaceClick = onOpenDetails,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.home_empty))
                    }
                }
            } else {
                if (nearOrdered.isNotEmpty()) {
                    PlaceHorizontalSection(
                        title = stringResource(R.string.home_suggestions_title),
                        places = nearOrdered.take(30),
                        onPlaceClick = onOpenDetails,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.home_empty))
                    }
                }

                val topRated = lb.establishments.take(12)
                if (topRated.isNotEmpty()) {
                    LeaderboardHorizontalSection(
                        title = stringResource(R.string.home_best_rated_title),
                        rows = topRated,
                        onPlaceClick = onOpenDetails
                    )
                }
            }
        }
    }
}

/**
 * Geocodifica uma string (ex.: "Porto", "Av. da Liberdade, Lisboa") para a **primeira** coordenada disponível.
 *
 * ### Observações
 * - Usa `Geocoder` (bloqueante) num `Dispatcher.IO` para não bloquear a *main thread*.
 * - Em dispositivos sem *backends* de geocodificação ou sem rede pode devolver `null`.
 *
 * @param context Contexto Android.
 * @param query Texto a geocodificar.
 * @return A primeira [LatLng] encontrada ou `null`.
 */
private suspend fun geocodeFirstLatLng(
    context: Context,
    query: String
): LatLng? = withContext(Dispatchers.IO) {
    @Suppress("DEPRECATION")
    runCatching {
        Geocoder(context).getFromLocationName(query, 1)
            ?.firstOrNull()
            ?.let { LatLng(it.latitude, it.longitude) }
    }.getOrNull()
}
