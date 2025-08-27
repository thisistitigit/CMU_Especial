package com.example.cmu_especial.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.GeofencingEvent
import timber.log.Timber

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val transition = event.geofenceTransition
        val ids = event.triggeringGeofences?.map { it.requestId } ?: emptyList()
        Timber.d("Geofence transition=$transition ids=$ids")
        // TODO: filtrar janela 16–18h e notificar canal "proximity"
    }
}
