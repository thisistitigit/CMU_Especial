// app/src/main/java/com/example/reviewapp/geofence/GeofenceRegistrar.kt
package com.example.reviewapp.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.example.reviewapp.data.enums.PlaceType
import com.example.reviewapp.data.repository.PlaceRepository
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException

@Singleton
class GeofenceRegistrar @Inject constructor(
    private val repo: PlaceRepository
) {
    private fun pendingIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, GeofenceReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(ctx, 0, intent, flags)
    }

    suspend fun refreshGeofences(
        ctx: Context,
        lat: Double,
        lng: Double,
        searchRadiusMeters: Int = 400
    ) {
        if (!com.example.reviewapp.utils.PermissionUtils.hasBackgroundLocationPermission(ctx)) {
            Log.e("Geofence", "Sem ACCESS_BACKGROUND_LOCATION — não registo geofences.")
            return
        }

        val client = LocationServices.getGeofencingClient(ctx)

        // 1) limpa os antigos ligados a este PI
        runCatching { client.removeGeofences(pendingIntent(ctx)).await() }

        // 2) top 10 restaurantes perto
        val places = repo.nearby(
            lat, lng, searchRadiusMeters, setOf(PlaceType.RESTAURANT)
        ).take(10).filter { it.lat != 0.0 || it.lng != 0.0 }

        if (places.isEmpty()) return

        val geofences = places.map {
            Geofence.Builder()
                .setRequestId(it.id)
                .setCircularRegion(it.lat, it.lng, 1000f) // 50 m
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL
                )
                .setLoiteringDelay(2 * 60 * 1000) // 2 min dwell para evitar spam
                .build()
        }

        val req = GeofencingRequest.Builder()
            .setInitialTrigger(
                GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL
            )
            .addGeofences(geofences)
            .build()

        if (!ensureLocationSettings(ctx)) {
            Log.e("Geofence", "Location settings não satisfazem requisitos — abortar registo.")
            return
        }

        val pi = pendingIntent(ctx)
        runCatching { client.addGeofences(req, pi).await() }
            .onSuccess { Log.d("Geofence", "Registadas ${geofences.size}") }
            .onFailure { err ->
                val msg = if (err is ApiException)
                    "${err.statusCode} - ${GeofenceStatusCodes.getStatusCodeString(err.statusCode)}"
                else err.message
                Log.e("Geofence", "Falha a registar: $msg", err)
            }
    }

    private suspend fun ensureLocationSettings(ctx: Context): Boolean {
        val req = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000).build()
        val settingsReq = LocationSettingsRequest.Builder().addLocationRequest(req).build()
        val client: SettingsClient = LocationServices.getSettingsClient(ctx)
        return try {
            client.checkLocationSettings(settingsReq).await()
            true
        } catch (e: ResolvableApiException) {
            // Aqui poderias lançar um intent de resolução se estivesse num Activity
            false
        } catch (_: Throwable) {
            false
        }
    }
}
