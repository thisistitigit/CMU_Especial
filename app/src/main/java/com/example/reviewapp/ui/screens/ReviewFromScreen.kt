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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.reviewapp.R
import com.example.reviewapp.ui.components.OfflineBanner
import com.example.reviewapp.ui.components.StarSelector
import com.example.reviewapp.utils.MediaStoreUtils
import com.example.reviewapp.utils.PermissionUtils
import com.example.reviewapp.utils.ReviewRules
import com.example.reviewapp.viewmodels.AuthViewModel
import com.example.reviewapp.viewmodels.ReviewFormViewModel
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uid = authVm.auth.currentUser?.uid
    var username by remember { mutableStateOf<String?>(null) }
    var loadingUser by remember { mutableStateOf(true) }

    LaunchedEffect(uid, placeId, navPlaceLat, navPlaceLng) {
        if (uid == null) { loadingUser = false; return@LaunchedEffect }
        try {
            val snap = authVm.db.collection("users").document(uid).get().await()
            val fetched = snap.getString("username")
                ?: authVm.auth.currentUser?.displayName ?: context.getString(R.string.user_generic)
            username = fetched
            vm.init(placeId = placeId, userId = uid, userName = fetched)
            vm.warmupRules(distanceMeters = null)

            val hasPerm = PermissionUtils.hasLocationPermission(context)
            vm.setLocationPermission(hasPerm)
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            vm.setLocationEnabled(isEnabled)

            val pLatLng: Pair<Double, Double>? = when {
                navPlaceLat != null && navPlaceLng != null -> navPlaceLat to navPlaceLng
                else -> {
                    val doc = authVm.db.collection("places").document(placeId).get().await()
                    doc.getGeoPoint("geo")?.let { it.latitude to it.longitude }
                        ?: doc.getGeoPoint("location")?.let { it.latitude to it.longitude }
                        ?: run {
                            val a = doc.getDouble("lat"); val b = doc.getDouble("lng")
                            if (a != null && b != null) a to b else null
                        }
                        ?: run {
                            val geom = doc.get("geometry") as? Map<*, *>
                            val loc = geom?.get("location") as? Map<*, *>
                            val a = (loc?.get("lat") as? Number)?.toDouble()
                            val b = (loc?.get("lng") as? Number)?.toDouble()
                            if (a != null && b != null) a to b else null
                        }
                }
            }

            if (hasPerm && isEnabled && pLatLng != null) {
                vm.setLocLoading(true)
                val loc = getOneShotLocation(context, Priority.PRIORITY_BALANCED_POWER_ACCURACY, 8000)
                vm.setLocLoading(false)

                val (pLat, pLng) = pLatLng
                if (loc != null) {
                    vm.setUserLocation(loc.latitude, loc.longitude)
                    val r = FloatArray(1)
                    Location.distanceBetween(loc.latitude, loc.longitude, pLat, pLng, r)
                    vm.setDistanceMeters(r[0].toDouble())
                } else {
                    vm.setUserLocation(pLat, pLng) // fallback: não bloqueia
                    vm.setDistanceMeters(0.0)
                }
            } else {
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
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCaptureUri?.let { vm.setPhotoLocalPath(it.toString()) }
        pendingCaptureUri = null
    }
    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) pendingCaptureUri?.let { takePicture.launch(it) } }
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
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
                uid == null -> {
                    Text(stringResource(R.string.error_not_authenticated), color = MaterialTheme.colorScheme.error)
                    return@Column
                }
                username == null -> {
                    Text(stringResource(R.string.error_generic), color = MaterialTheme.colorScheme.error)
                    return@Column
                }
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

            Button(
                enabled = state.canSubmit && state.rulesOk && !state.isSubmitting,
                onClick = { scope.launch { if (vm.submit()) onDone() } }
            ) { Text(stringResource(R.string.action_submit)) }

            if (!state.rulesOk || state.ruleMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (state.ruleMessage) {
                        "hint_grant_location_permission" -> stringResource(R.string.hint_grant_location_permission)
                        "hint_enable_gps" -> stringResource(R.string.hint_enable_gps)
                        "hint_fetching_location" -> stringResource(R.string.hint_fetching_location)
                        "error_too_far" -> stringResource(R.string.error_too_far, ReviewRules.MIN_DISTANCE_METERS.toInt())
                        "error_too_far_live" -> stringResource(R.string.error_too_far_live,
                            (state.distanceMeters ?: 0.0).toInt(), ReviewRules.MIN_DISTANCE_METERS.toInt())
                        "error_too_soon" -> stringResource(R.string.error_too_soon, ReviewRules.MIN_INTERVAL_MINUTES)
                        "error_too_soon_live" -> stringResource(R.string.error_too_soon_live, ReviewRules.MIN_INTERVAL_MINUTES)
                        "error_enable_location" -> stringResource(R.string.error_enable_location)
                        "error_fill_fields" -> stringResource(R.string.error_fill_fields)
                        "error_submit_generic" -> stringResource(R.string.error_submit_generic)
                        "error_not_authenticated" -> stringResource(R.string.error_not_authenticated)
                        else -> stringResource(R.string.rules_not_met)
                    },
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

    val current = withTimeoutOrNull(timeoutMs / 2) {
        try { fused.getCurrentLocation(priority, null).await() } catch (_: Exception) { null }
    }
    if (current != null) return current

    val last = try { fused.lastLocation.await() } catch (_: Exception) { null }
    if (last != null) return last

    return withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine<Location?> { cont ->
            val req = LocationRequest.Builder(priority, 0L).setMinUpdateIntervalMillis(0).setMaxUpdates(1).build()
            val cb = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(res: com.google.android.gms.location.LocationResult) {
                    if (!cont.isCompleted) cont.resume(res.lastLocation, onCancellation = null)
                    fused.removeLocationUpdates(this)
                }
            }
            try { fused.requestLocationUpdates(req, cb, Looper.getMainLooper()) } catch (_: SecurityException) {
                if (!cont.isCompleted) cont.resume(null, onCancellation = null)
            }
            cont.invokeOnCancellation { fused.removeLocationUpdates(cb) }
        }
    }
}
