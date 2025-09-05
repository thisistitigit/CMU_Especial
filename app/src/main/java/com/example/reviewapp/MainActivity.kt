// app/src/main/java/com/example/reviewapp/MainActivity.kt
package com.example.reviewapp

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

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    @Inject lateinit var registrar: com.example.reviewapp.geofence.GeofenceRegistrar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReviewAppTheme { AppNavGraph(rememberNavController()) } }
        quickRefresh()
    }

    private fun quickRefresh() {
        if (!com.example.reviewapp.utils.PermissionUtils.hasLocationPermission(this)) return
        val fused = LocationServices.getFusedLocationProviderClient(this)
        try {
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    lifecycleScope.launch {
                        try {
                            registrar.refreshGeofences(this@MainActivity, loc.latitude, loc.longitude)
                        } catch (t: Throwable) { Log.e("Geofence", "Quick refresh falhou: ${t.message}") }
                    }
                }
            }
        } catch (_: SecurityException) { /* ignorar */ }
    }
}
