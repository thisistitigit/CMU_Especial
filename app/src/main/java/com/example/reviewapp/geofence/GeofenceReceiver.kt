package com.example.reviewapp.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dagger.hilt.android.EntryPointAccessors
import com.example.reviewapp.di.GeofenceReceiverEntryPoint
import com.example.reviewapp.services.notifyNearby

/**
 * Receiver de transições de **Geofence** (ENTER/DWELL).
 *
 * Ao disparar, carrega o `PlaceDao` via [GeofenceReceiverEntryPoint],
 * obtém os locais pelos `requestId`s e emite uma notificação contextual.
 *
 * @since 1.0
 */
class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.e("Geofence", "Erro evento: ${event.errorCode}")
            return
        }

        val transition = event.geofenceTransition
        val isEnterOrDwell =
            transition == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER ||
                    transition == com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_DWELL
        if (!isEnterOrDwell) return

        val appCtx = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appCtx, GeofenceReceiverEntryPoint::class.java
        )
        val placeDao = entryPoint.placeDao()

        val ids = event.triggeringGeofences?.mapNotNull { it.requestId } ?: return
        CoroutineScope(Dispatchers.IO).launch {
            ids.forEach { placeId ->
                val place = placeDao.get(placeId)
                notifyNearby(appCtx, place?.name)
            }
        }
    }
}
