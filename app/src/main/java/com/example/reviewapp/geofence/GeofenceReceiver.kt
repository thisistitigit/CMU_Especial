// app/src/main/java/com/example/reviewapp/geofence/GeofenceReceiver.kt
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
import com.example.reviewapp.utils.TimeUtils

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
      //  if (!TimeUtils.isWithinPromoWindow()) return

        // ⚡ obter dependências via EntryPoint (sem @AndroidEntryPoint)
        val appCtx = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appCtx, GeofenceReceiverEntryPoint::class.java
        )
        val placeDao = entryPoint.placeDao()

        val ids = event.triggeringGeofences?.mapNotNull { it.requestId } ?: return
        CoroutineScope(Dispatchers.IO).launch {
            ids.forEach { placeId ->
                val place = placeDao.get(placeId)
                val title = place?.name ?: "Estabelecimento por perto"
                val addr  = place?.address ?: "A menos de 50 m"
                notifyNearby(appCtx, "Estás perto: $title", addr)
            }
        }
    }
}
