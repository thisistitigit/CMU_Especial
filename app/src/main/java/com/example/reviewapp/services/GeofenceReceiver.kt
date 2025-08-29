package com.example.reviewapp.services

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.time.LocalTime
class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val hour = LocalTime.now()
        val withinWindow = hour >= LocalTime.of(16, 0) && hour <= LocalTime.of(18, 0)

        if (withinWindow && event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // construir e emitir Notification
            notifyNearby(
                context,
                title = "Doces por perto",
                text = "Há uma pastelaria com reviews a menos de 50m 🍰"
            )
        }
    }
}
