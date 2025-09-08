package com.example.reviewapp.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.EntryPointAccessors
import com.example.reviewapp.di.GeofenceReceiverEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val res = LocationResult.extractResult(intent) ?: return
        val loc = res.lastLocation ?: return

        val app = context.applicationContext
        val entry = EntryPointAccessors.fromApplication(app, GeofenceReceiverEntryPoint::class.java)
        val registrar = entry.geofenceRegistrar()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                registrar.refreshGeofences(app, loc.latitude, loc.longitude)
                Log.d("Geofence", "Refresh por movimento @ ${loc.latitude},${loc.longitude}")
            } catch (t: Throwable) {
                Log.e("Geofence", "Refresh por movimento falhou: ${t.message}")
            }
        }
    }
}
