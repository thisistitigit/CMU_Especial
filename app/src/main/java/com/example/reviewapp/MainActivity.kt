package com.example.reviewapp

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.reviewapp.navigation.AppNavGraph
import com.example.reviewapp.ui.theme.ReviewAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import com.google.android.gms.location.LocationServices
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log
import com.example.reviewapp.geofence.LocationUpdateReceiver
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    @Inject
    lateinit var registrar: com.example.reviewapp.geofence.GeofenceRegistrar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReviewAppTheme { AppNavGraph(rememberNavController()) } }

        // 1) Semeadura inicial: regista geofences com a lastLocation (se houver)
        quickSeedGeofences()

        // 2) Passa a manter geofences atualizadas automaticamente por movimento (~500 m)
        startLocationUpdatesForGeofences()
    }

    /** Regista geofences uma vez com a última localização conhecida (seed inicial). */
    private fun quickSeedGeofences() {
        if (!com.example.reviewapp.utils.PermissionUtils.hasLocationPermission(this)) return
        if (!com.example.reviewapp.utils.PermissionUtils.hasBackgroundLocationPermission(this)) return

        val fused = LocationServices.getFusedLocationProviderClient(this)
        try {
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    lifecycleScope.launch {
                        try {
                            registrar.refreshGeofences(
                                this@MainActivity,
                                loc.latitude,
                                loc.longitude
                            )
                        } catch (t: Throwable) {
                            Log.e("Geofence", "Seed geofences falhou: ${t.message}")
                        }
                    }
                }
            }
        } catch (_: SecurityException) { /* sem permissões */ }
    }

    /** Pede updates de localização low-power; refresca geofences quando nos movimentamos ~500 m. */
    private fun startLocationUpdatesForGeofences() {
        if (!com.example.reviewapp.utils.PermissionUtils.hasLocationPermission(this)) return
        if (!com.example.reviewapp.utils.PermissionUtils.hasBackgroundLocationPermission(this)) return

        val fused = LocationServices.getFusedLocationProviderClient(this)
        val req = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, 30 * 60 * 1000L // 30 min sugerido
        ).setMinUpdateDistanceMeters(500f) // refresca após ~500 m
            .build()

        val intent = Intent(this, LocationUpdateReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        try {
            fused.requestLocationUpdates(req, pi)
        } catch (_: SecurityException) { /* pedir permissão antes */ }
    }
}
