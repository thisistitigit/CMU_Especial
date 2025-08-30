package com.example.reviewapp.ui.screens

import android.Manifest
import android.util.Log
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.data.models.Place
import com.example.reviewapp.ui.components.PlaceListItem
import com.example.reviewapp.viewmodels.SearchViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

private enum class HomeMode { List, Map }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: SearchViewModel = hiltViewModel(),
    onOpenDetails: (String) -> Unit,
    onOpenReview: (String) -> Unit,
    onOpenProfile: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val ui by vm.state.collectAsState()
    // Sempre que a lista mudar, despeja no Logcat
    LaunchedEffect(ui.places) {
        Log.d("HomeScreen", "places.size=${ui.places.size}")
        ui.places.take(5).forEachIndexed { i, p ->
            Log.d(
                "HomeScreen",
                "[$i] id=${p.id}, name=${p.name}, lat=${p.lat}, lng=${p.lng}, rating=${p.avgRating}"
            )
        }
    }
    // --- Permissões ---
    var hasLocationPermission by remember { mutableStateOf(false) }
    fun checkGranted(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                coarse == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        hasLocationPermission = res[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                res[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) vm.refresh()
    }
    val showRationale = !hasLocationPermission &&
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)

    // primeira carga
    LaunchedEffect(Unit) {
        hasLocationPermission = checkGranted()
        if (hasLocationPermission) vm.refresh() else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }


    var mode by remember { mutableStateOf(HomeMode.List) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pastelarias & Doçaria") },
                actions = {
                    IconButton(onClick = { mode = HomeMode.List }) {
                        Icon(Icons.Filled.List, contentDescription = "Lista")
                    }
                    IconButton(onClick = { mode = HomeMode.Map }) {
                        Icon(Icons.Filled.Map, contentDescription = "Mapa")
                    }
                }
            )
        },
        floatingActionButton = {
            if (mode == HomeMode.Map) {
                FloatingActionButton(onClick = { vm.refresh() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Minha localização")
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {

            if (!hasLocationPermission) {
                PermissionBanner(
                    showRationale = showRationale,
                    onRequest = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onOpenSettings = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                )
            }

            when (mode) {
                HomeMode.List -> PlaceList(
                    places = ui.places,
                    onOpenDetails = onOpenDetails,
                    onOpenReview = onOpenReview
                )
                HomeMode.Map -> PlaceMap(
                    center = ui.cameraLatLng,
                    places = ui.places,
                    onMarkerTap = onOpenDetails,
                    myLocationEnabled = hasLocationPermission
                )
            }
        }
    }
}

@Composable
private fun PlaceList(
    places: List<Place>,
    onOpenDetails: (String) -> Unit,
    onOpenReview: (String) -> Unit
) {
    if (places.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        items(places.sortedByDescending { it.avgRating }) { p ->
            Column {
                PlaceListItem(place = p, onClick = { onOpenDetails(p.id) })
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    TextButton(onClick = { onOpenDetails(p.id) }) { Text("Detalhes") }
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { onOpenReview(p.id) }) { Text("Avaliar") }
                }
                Divider()
            }
        }
    }
}

@Composable
private fun PlaceMap(
    center: LatLng,
    places: List<Place>,
    onMarkerTap: (String) -> Unit,
    myLocationEnabled: Boolean
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 15f)
    }
    val mapProps = remember(myLocationEnabled) {
        MapProperties(isMyLocationEnabled = myLocationEnabled)
    }
    val uiSettings = remember {
        MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false)
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.45f), // mapa “hero” no topo
        cameraPositionState = cameraPositionState,
        properties = mapProps,
        uiSettings = uiSettings
    ) {
        places.forEach { p ->
            Marker(
                state = MarkerState(position = LatLng(p.lat, p.lng)),
                title = p.name,
                onClick = {
                    onMarkerTap(p.id)
                    true
                }
            )
        }
    }

    // abaixo do mapa, lista resumida (top 10) para rápido acesso
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
          //  .weight(1f, fill = true)
            .padding(top = 8.dp)
    ) {
        items(places.sortedByDescending { it.avgRating }.take(10)) { p ->
            ListItem(
                headlineContent = { Text(p.name) },
                supportingContent = { Text(p.address ?: "") },
                modifier = Modifier.fillMaxWidth(),
                trailingContent = {
                    AssistChip(
                        onClick = { /* no-op */ },
                        label = { Text("${"%.1f".format(p.avgRating ?: 0.0)}★") }
                    )
                }
            )
            Divider()
        }
    }
}

@Composable
private fun PermissionBanner(
    showRationale: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (showRationale)
                    "Precisamos da tua localização para centrar o mapa perto de ti."
                else
                    "Permissão de localização desativada. Concede acesso nas definições."
            )
            Spacer(Modifier.height(8.dp))
            Row {
                if (showRationale) {
                    Button(onClick = onRequest) { Text("Permitir localização") }
                } else {
                    OutlinedButton(onClick = onOpenSettings) { Text("Abrir definições") }
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = onRequest) { Text("Tentar novamente") }
                }
            }
        }
    }
}
