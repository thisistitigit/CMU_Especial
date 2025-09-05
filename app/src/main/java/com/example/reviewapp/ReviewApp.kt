// app/src/main/java/com/example/reviewapp/ReviewApp.kt
package com.example.reviewapp

import android.app.Application
import androidx.work.*
import dagger.hilt.android.HiltAndroidApp
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import com.example.reviewapp.services.GeofenceRefreshWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

@HiltAndroidApp
class ReviewApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseFirestore.setLoggingEnabled(true)
        scheduleDailyGeofenceRefresh()
    }

    private fun scheduleDailyGeofenceRefresh() {
        val now = ZonedDateTime.now()
        val target = now.withHour(17).withMinute(2).withSecond(0).withNano(0)
            .let { if (it.isBefore(now)) it.plusDays(1) else it }
        val delayMin = Duration.between(now, target).toMinutes()

        val req = OneTimeWorkRequestBuilder<GeofenceRefreshWorker>()
            .setInitialDelay(delayMin, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "geofence_refresh_daily",
            ExistingWorkPolicy.REPLACE,
            req
        )
    }
}
