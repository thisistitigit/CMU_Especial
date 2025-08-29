package com.example.reviewapp.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.reviewapp.ui.components.PlaceListItem
import com.example.reviewapp.viewmodels.SearchViewModel
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
    val context = LocalContext.current
    val activity = context as Activity
    val state by vm.state.collectAsState()

    // --- estado da permissão em runtime ---
    var hasLocationPermission by remember { mutableStateOf(false) }
    fun checkGranted(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    // launcher para pedir permissões
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasLocationPermission = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            vm.refresh() // arranca logo a pesquisa perto da posição atual
        }
    }

    // primeira composição: se já tem permissão → refresh; senão → pedir
    LaunchedEffect(Unit) {
        hasLocationPermission = checkGranted()
        if (hasLocationPermission) {
            vm.refresh()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // opção para re-pedir caso o utilizador negue uma vez
    val showRationale =
        !hasLocationPermission && ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.ACCESS_FINE_LOCATION
        )

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

            // banner de rationale / atalho para definições se foi negado "para sempre"
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

            // Mapa
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(state.cameraLatLng, 15f)
            }

            // Ativa o "ponto azul" + botão "minha localização"
            val mapProperties = remember(hasLocationPermission) {
                MapProperties(isMyLocationEnabled = hasLocationPermission)
            }
            val uiSettings = remember {
                MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false)
            }

            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(260.dp),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = uiSettings
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

            // Botão "Pesquisar nesta zona"
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
                    },
                    enabled = true // pode pesquisar mesmo sem permissão, usando o centro do mapa
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
                    "A app precisa da tua localização para centrar o mapa perto de ti."
                else
                    "Permissão de localização desativada. Concede acesso para centrar o mapa na tua posição."
            )
            Spacer(Modifier.height(8.dp))
            Row {
                if (showRationale) {
                    Button(onClick = onRequest) { Text("Permitir localização") }
                } else {
                    // Negado “para sempre”: abrir Definições
                    OutlinedButton(onClick = onOpenSettings) { Text("Abrir definições") }
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = onRequest) { Text("Tentar novamente") }
                }
            }
        }
    }
}
