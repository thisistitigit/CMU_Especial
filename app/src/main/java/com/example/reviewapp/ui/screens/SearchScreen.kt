package com.example.reviewapp.ui.screens

import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.R
import com.example.reviewapp.ui.components.AppHeader
import com.example.reviewapp.ui.components.OfflineBanner
import com.example.reviewapp.ui.components.PlaceListItem
import com.example.reviewapp.ui.components.PlaceSort
import com.example.reviewapp.ui.components.PlaceSortButton
import com.example.reviewapp.ui.components.PlaceSortState
import com.example.reviewapp.ui.components.applyPlaceSort
import com.example.reviewapp.utils.rememberLocationPermissionState
import com.example.reviewapp.viewmodels.SearchViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    vm: SearchViewModel = hiltViewModel(),
    onOpenDetails: (String) -> Unit,
    onOpenProfile: () -> Unit
) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    val perm = rememberLocationPermissionState(onGranted = { /* no-op */ })

    var searchActive by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showFilter by remember { mutableStateOf(false) }
    var geocodingInFlight by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var sortState by remember { mutableStateOf(PlaceSortState(selected = PlaceSort.GOOGLE_RATING)) }
    val sortedPlaces = remember(state.places, sortState, state.searchCenter) {
        applyPlaceSort(state.places, sortState, state.searchCenter)
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = stringResource(R.string.search_title),
                actions = {
                    IconButton(onClick = { searchActive = !searchActive }) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_location_hint))
                    }

                    Box {
                        IconButton(onClick = { showFilter = true }) {
                            Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.action_filter))
                        }
                        DropdownMenu(expanded = showFilter, onDismissRequest = { showFilter = false }) {
                            Text(
                                text = stringResource(R.string.filter_radius_title),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                            DropdownMenuItem(text = { Text(stringResource(R.string.radius_250m)) },
                                onClick = { vm.setRadiusMeters(250); showFilter = false })
                            DropdownMenuItem(text = { Text(stringResource(R.string.radius_1km)) },
                                onClick = { vm.setRadiusMeters(1000); showFilter = false })
                            DropdownMenuItem(text = { Text(stringResource(R.string.radius_3km)) },
                                onClick = { vm.setRadiusMeters(3000); showFilter = false })
                            DropdownMenuItem(text = { Text(stringResource(R.string.radius_5km)) },
                                onClick = { vm.setRadiusMeters(5000); showFilter = false })
                        }
                    }

                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = stringResource(R.string.search_profile_cd))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (perm.isGranted) vm.refreshNearMe() else perm.ask() }
            ) { Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.action_my_location)) }
        }
    ) { padding ->

        Column(Modifier.padding(padding)) {

            if (searchActive) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.search_location_hint)) },
                        trailingIcon = {
                            Row {
                                if (query.isNotBlank()) {
                                    IconButton(onClick = { query = "" }, enabled = !geocodingInFlight) {
                                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear))
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        if (query.isBlank() || geocodingInFlight) return@IconButton
                                        scope.launch {
                                            geocodingInFlight = true
                                            val latLng = geocodeFirstLatLng(ctx, query)
                                            geocodingInFlight = false
                                            latLng?.let { vm.refreshAt(it, radiusMeters = state.radiusMeters) }
                                        }
                                    },
                                    enabled = query.isNotBlank() && !geocodingInFlight
                                ) {
                                    Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_location_hint))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    if (geocodingInFlight) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }
                }
            }

            OfflineBanner()

            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(state.cameraLatLng, 15f)
            }
            LaunchedEffect(state.cameraLatLng) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(state.cameraLatLng, 15f))
            }

            var pendingCenter by remember { mutableStateOf<LatLng?>(null) }
            LaunchedEffect(cameraPositionState) {
                snapshotFlow { cameraPositionState.isMoving }
                    .collectLatest { moving ->
                        if (!moving) pendingCenter = cameraPositionState.position.target
                    }
            }

            val mapProps = remember(perm.isGranted) { MapProperties(isMyLocationEnabled = perm.isGranted) }
            val uiSettings = remember { MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.matchParentSize(),
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

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.extraSmall
                        )
                )

                if (pendingCenter != null) {
                    ElevatedAssistChip(
                        onClick = {
                            val center = pendingCenter!!
                            pendingCenter = null
                            vm.refreshAt(center, radiusMeters = state.radiusMeters)
                        },
                        label = { Text(stringResource(R.string.search_here)) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    )
                }
            }

            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        PlaceSortButton(state = sortState, onChange = { sortState = it })
                    }
                }

                items(sortedPlaces) { p ->
                    Column(Modifier.fillMaxWidth()) {
                        PlaceListItem(place = p, onClick = { onOpenDetails(p.id) })
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            TextButton(onClick = { onOpenDetails(p.id) }) {
                                Text(stringResource(R.string.action_details))
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }
}

/** Geocoding simples em background. */
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
