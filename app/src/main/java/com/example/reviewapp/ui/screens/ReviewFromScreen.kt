package com.example.reviewapp.ui.screens

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.reviewapp.R
import com.example.reviewapp.ui.components.StarSelector
import com.example.reviewapp.viewmodels.AuthViewModel
import com.example.reviewapp.viewmodels.ReviewFormViewModel
import com.example.reviewapp.utils.MediaStoreUtils
import com.example.reviewapp.utils.PermissionUtils
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewFormScreen(
    placeId: String,
    navPlaceLat: Double? = null,
    navPlaceLng: Double? = null,
    vm: ReviewFormViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel(),
    onDone: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val state by vm.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val uid = authVm.auth.currentUser?.uid
    var username by remember { mutableStateOf<String?>(null) }
    var loadingUser by remember { mutableStateOf(true) }


    // ReviewFormScreen.kt  (substitui o LaunchedEffect pelo abaixo)
    LaunchedEffect(uid, placeId, navPlaceLat, navPlaceLng) {
        if (uid == null) { loadingUser = false; return@LaunchedEffect }
        try {
            // username/init/warmup iguais…
            val snap = authVm.db.collection("users").document(uid).get().await()
            val fetched = snap.getString("username")
                ?: authVm.auth.currentUser?.displayName ?: "Utilizador"
            username = fetched
            vm.init(placeId = placeId, userId = uid, userName = fetched)
            vm.warmupRules(distanceMeters = null)

            // perm + serviço
            val hasPerm = PermissionUtils.hasLocationPermission(context)
            vm.setLocationPermission(hasPerm)
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            vm.setLocationEnabled(isEnabled)

            // ------------ AQUI ESTÁ A MUDANÇA: fonte das coord. do sítio ------------
            val pLatLng: Pair<Double, Double>? = when {
                navPlaceLat != null && navPlaceLng != null -> navPlaceLat to navPlaceLng
                else -> {
                    // fallback para Firestore apenas se não veio via navegação
                    val doc = authVm.db.collection("places").document(placeId).get().await()
                    // tenta GeoPoint comum
                    doc.getGeoPoint("geo")?.let { it.latitude to it.longitude }
                        ?: doc.getGeoPoint("location")?.let { it.latitude to it.longitude }
                        // tenta top-level doubles
                        ?: run {
                            val a = doc.getDouble("lat"); val b = doc.getDouble("lng")
                            if (a != null && b != null) a to b else null
                        }
                        // tenta nested maps geometry.location.lat/lng
                        ?: run {
                            val geom = doc.get("geometry") as? Map<*, *>
                            val loc  = geom?.get("location") as? Map<*, *>
                            val a = (loc?.get("lat") as? Number)?.toDouble()
                            val b = (loc?.get("lng") as? Number)?.toDouble()
                            if (a != null && b != null) a to b else null
                        }
                }
            }
            // -------------------------------------------------------------------------

            if (hasPerm && isEnabled && pLatLng != null) {
                vm.setLocLoading(true)
                val loc = getOneShotLocation(
                    context,
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    timeoutMs = 8000
                )
                vm.setLocLoading(false)

                val (pLat, pLng) = pLatLng
                if (loc != null) {
                    vm.setUserLocation(loc.latitude, loc.longitude)
                    val r = FloatArray(1)
                    Location.distanceBetween(loc.latitude, loc.longitude, pLat, pLng, r)
                    vm.setDistanceMeters(r[0].toDouble())
                } else {
                    // sem fix: HARD FALLBACK para não bloquear
                    vm.setUserLocation(pLat, pLng)
                    vm.setDistanceMeters(0.0)
                }
            } else {
                // se não temos coord. do sítio → não bloqueia: assume no local (desbloqueia já)
                if (pLatLng != null) {
                    vm.setUserLocation(pLatLng.first, pLatLng.second)
                    vm.setDistanceMeters(0.0)
                } else {
                    vm.setUserLocation(null, null)
                    vm.setDistanceMeters(null)
                }
            }
        } catch (_: SecurityException) {
            vm.setLocLoading(false); vm.setUserLocation(null, null); vm.setDistanceMeters(null)
        } catch (_: Throwable) {
            vm.setLocLoading(false); vm.setUserLocation(null, null); vm.setDistanceMeters(null)
        } finally { loadingUser = false }
    }



    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingCaptureUri?.let { vm.setPhotoLocalPath(it.toString()) }
        pendingCaptureUri = null
    }
    val requestCameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) pendingCaptureUri?.let { takePicture.launch(it) } }
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { vm.setPhotoLocalPath(it.toString()) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.review_new_title)) },
                navigationIcon = { IconButton(onClick = onCancel) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {

            when {
                loadingUser -> { CircularProgressIndicator(); return@Column }
                uid == null   -> { Text(stringResource(R.string.error_not_authenticated),
                    color = MaterialTheme.colorScheme.error); return@Column }
                username == null -> { Text(stringResource(R.string.error_generic),
                    color = MaterialTheme.colorScheme.error); return@Column }
            }

            if (!state.photoLocalPath.isNullOrBlank()) {
                AsyncImage(
                    model = state.photoLocalPath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { vm.setPhotoLocalPath(null) }) {
                    Text(stringResource(R.string.action_remove_photo))
                }
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = state.pastryName,
                onValueChange = vm::onPastryChanged,
                label = { Text(stringResource(R.string.field_pastry)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            StarSelector(selected = state.stars, onChange = vm::onStarsChanged)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.comment,
                onValueChange = vm::onCommentChanged,
                label = { Text(stringResource(R.string.field_comment)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Row {
                Button(onClick = {
                    val uri = MediaStoreUtils.createImageUri(context)
                    if (uri != null) {
                        pendingCaptureUri = uri
                        if (PermissionUtils.hasCameraPermission(context)) {
                            takePicture.launch(uri)
                        } else {
                            requestCameraPermission.launch(android.Manifest.permission.CAMERA)
                        }
                    }
                }) { Text(stringResource(R.string.action_take_photo)) }

                Spacer(Modifier.width(12.dp))

                Button(onClick = { pickImage.launch("image/*") }) {
                    Text(stringResource(R.string.action_pick_gallery))
                }
            }

            Spacer(Modifier.height(16.dp))

            val canSubmit = state.canSubmit && state.rulesOk
            Button(
                enabled = canSubmit && !state.isSubmitting,
                onClick = {
                    // Só navega se o submit for bem sucedido
                    scope.launch {
                        val ok = vm.submit()
                        if (ok) onDone()
                    }
                }
            ) { Text(stringResource(R.string.action_submit)) }

            if (!state.rulesOk || state.ruleMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.ruleMessage ?: stringResource(R.string.rules_not_met),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
private suspend fun getOneShotLocation(
    context: Context,
    priority: Int = Priority.PRIORITY_HIGH_ACCURACY,
    timeoutMs: Long = 7000
): Location? {
    val fused = LocationServices.getFusedLocationProviderClient(context)

    // 1) tenta getCurrentLocation (rápido quando há fix)
    val current = withTimeoutOrNull(timeoutMs / 2) {
        try {
            fused.getCurrentLocation(priority, null).await()
        } catch (_: Exception) {
            null
        }
    }
    if (current != null) return current

    // 2) tenta lastLocation
    val last = try {
        fused.lastLocation.await()
    } catch (_: Exception) {
        null
    }
    if (last != null) return last

    // 3) one-shot requestLocationUpdates (garante um fix novo)
    return withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine<Location?> { cont ->
            val req = LocationRequest.Builder(priority, /*interval*/ 0L)
                .setMinUpdateIntervalMillis(0)
                .setMaxUpdates(1)
                .build()

            val cb = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    val loc = result.lastLocation
                    if (!cont.isCompleted) cont.resume(loc, onCancellation = null)
                    fused.removeLocationUpdates(this)
                }
            }

            try {
                fused.requestLocationUpdates(req, cb, Looper.getMainLooper())
            } catch (se: SecurityException) {
                if (!cont.isCompleted) cont.resume(null, onCancellation = null)
            }

            cont.invokeOnCancellation { fused.removeLocationUpdates(cb) }
        }
    }
}
