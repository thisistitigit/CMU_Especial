package com.example.cmu_especial.services

import android.annotation.SuppressLint
import android.content.Context
import com.example.cmu_especial.domain.model.GeoPoint
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationTracker @Inject constructor(
    context: Context
) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private val req = LocationRequest.Builder(
        Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L
    ).setMinUpdateIntervalMillis(5_000L).build()

    private val _current = MutableStateFlow<GeoPoint?>(null)
    val current = _current.asStateFlow()

    private val cb = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let {
                val gp = GeoPoint(it.latitude, it.longitude)
                _current.value = gp
                Timber.d("Loc: $gp")
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start() { client.requestLocationUpdates(req, cb, null) }
    fun stop() { client.removeLocationUpdates(cb) }
}
