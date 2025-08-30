// com/example/reviewapp/ui/screens/SearchScreen.kt
// com/example/reviewapp/ui/screens/SearchScreen.kt
package com.example.reviewapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.R
import com.example.reviewapp.ui.components.PermissionBanner
import com.example.reviewapp.ui.components.PlaceListItem
import com.example.reviewapp.utils.rememberLocationPermissionState
import com.example.reviewapp.viewmodels.SearchViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    vm: SearchViewModel = hiltViewModel(),
    onOpenDetails: (String) -> Unit,
    onOpenReview: (String) -> Unit,
    onOpenProfile: () -> Unit
) {
    val state by vm.state.collectAsState()

    // ⟵ encapsulado num helper reutilizável
    val perm = rememberLocationPermissionState(onGranted = { vm.refresh() })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = stringResource(R.string.search_profile_cd))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (perm.isGranted) vm.refresh() else perm.ask()
            }) {
                Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.action_my_location))
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {

            if (!perm.isGranted) {
                PermissionBanner(
                    showRationale = perm.showRationale,
                    onRequest = perm.ask,
                    onOpenSettings = perm.openSettings
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.radius_label), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(12.dp))
                RadiusChip(label = stringResource(R.string.radius_250m), value = 250,  current = state.radiusMeters) { vm.setRadiusMeters(it) }
                Spacer(Modifier.width(8.dp))
                RadiusChip(label = stringResource(R.string.radius_1km),  value = 1000, current = state.radiusMeters) { vm.setRadiusMeters(it) }
                Spacer(Modifier.width(8.dp))
                RadiusChip(label = stringResource(R.string.radius_3km),  value = 3000, current = state.radiusMeters) { vm.setRadiusMeters(it) }
                Spacer(Modifier.width(8.dp))
                RadiusChip(label = stringResource(R.string.radius_5km),  value = 5000, current = state.radiusMeters) { vm.setRadiusMeters(it) }
            }
            // ===== Raio (chips) — igual ao que já tinhas =====
            // ... RadiusChip row aqui ...

            // ===== Mapa =====
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(state.cameraLatLng, 15f)
            }
            LaunchedEffect(state.cameraLatLng) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(state.cameraLatLng, 15f))
            }
            val mapProps = remember(perm.isGranted) {
                MapProperties(isMyLocationEnabled = perm.isGranted)
            }
            val uiSettings = remember {
                MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false)
            }

            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(260.dp),
                cameraPositionState = cameraPositionState,
                properties = mapProps,
                uiSettings = uiSettings
            ) {
                state.places.forEach { p ->
                    Marker(
                        state = MarkerState(position = LatLng(p.lat, p.lng)),
                        title = p.name,
                        onClick = { onOpenDetails(p.id); true }
                    )
                }
            }

            // Pesquisar nesta zona — usa o raio atual do estado
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = {
                    val center = cameraPositionState.position.target
                    vm.refreshAt(center, radiusMeters = state.radiusMeters)
                }) { Text(stringResource(R.string.search_in_this_area)) }
            }

            // Lista (ordenada no VM)
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.places) { p ->
                    Column(Modifier.fillMaxWidth()) {
                        PlaceListItem(place = p, onClick = { onOpenDetails(p.id) })
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            TextButton(onClick = { onOpenDetails(p.id) }) { Text(stringResource(R.string.action_details)) }
                            Spacer(Modifier.width(12.dp))
                            TextButton(onClick = { onOpenReview(p.id) }) { Text(stringResource(R.string.action_review)) }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadiusChip(label: String, value: Int, current: Int, onSelect: (Int) -> Unit) {
    FilterChip(
        selected = current == value,
        onClick = { onSelect(value) },
        label = { Text(label) },
        leadingIcon = null,
        colors = FilterChipDefaults.filterChipColors()
    )
}

