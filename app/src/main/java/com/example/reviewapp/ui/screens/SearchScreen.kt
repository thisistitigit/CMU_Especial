package com.example.reviewapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.ui.components.PlaceListItem
import com.example.reviewapp.viewmodels.SearchViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    vm: SearchViewModel = hiltViewModel(),
    onOpenDetails: (String) -> Unit,
    onOpenReview: (String) -> Unit,
    onOpenProfile: () -> Unit
) {
    val state by vm.state.collectAsState()

    // primeira carga: perto da localização atual
    LaunchedEffect(Unit) { vm.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pastelarias & Doçaria") },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Perfil")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {

            // Mapa
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(state.cameraLatLng, 15f)
            }
            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(260.dp),
                cameraPositionState = cameraPositionState
            ) {
                state.places.forEach { p ->
                    Marker(
                        state = MarkerState(position = LatLng(p.lat, p.lng)),
                        title = p.name,
                        onClick = {
                            onOpenDetails(p.id)
                            true
                        }
                    )
                }
            }

            // Botão para pesquisar no centro atual do mapa (usa nearby via refreshAt)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = {
                        val center = cameraPositionState.position.target
                        vm.refreshAt(center, radiusMeters = 250)
                    }
                ) { Text("Pesquisar nesta zona") }
            }

            // Lista ordenada por pontuação
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.places.sortedByDescending { it.avgRating }) { p ->
                    Column(Modifier.fillMaxWidth()) {
                        PlaceListItem(place = p, onClick = { onOpenDetails(p.id) })
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            TextButton(onClick = { onOpenDetails(p.id) }) {
                                Text("Detalhes")
                            }
                            Spacer(Modifier.width(12.dp))
                            TextButton(onClick = { onOpenReview(p.id) }) {
                                Icon(Icons.Filled.Star, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Avaliar")
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}
