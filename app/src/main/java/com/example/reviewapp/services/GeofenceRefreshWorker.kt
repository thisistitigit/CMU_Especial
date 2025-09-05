package com.example.reviewapp.services

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.example.reviewapp.geofence.GeofenceRegistrar
import com.example.reviewapp.utils.PermissionUtils
import kotlinx.coroutines.tasks.await
import android.util.Log

@HiltWorker
class GeofenceRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val registrar: GeofenceRegistrar
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!PermissionUtils.hasLocationPermission(ctx)) return Result.retry()

        return try {
            val fused = LocationServices.getFusedLocationProviderClient(ctx)
            val loc = fused.lastLocation.await() ?: return Result.retry()
            registrar.refreshGeofences(ctx, loc.latitude, loc.longitude)
            Result.success()
        } catch (se: SecurityException) {
            Log.e("Geofence", "Permissão negada: ${se.message}")
            Result.failure()
        } catch (t: Throwable) {
            Log.e("Geofence", "Erro refresh: ${t.message}")
            Result.retry()
        }
    }
}
