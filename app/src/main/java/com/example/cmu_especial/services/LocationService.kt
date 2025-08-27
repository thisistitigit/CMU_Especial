package com.example.cmu_especial.services

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.cmu_especial.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    @Inject lateinit var locationTracker: LocationTracker

    override fun onCreate() {
        super.onCreate()
        val notif: Notification = NotificationCompat.Builder(this, "proximity")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Localização ativa")
            .setContentText("A monitorizar proximidade a pastelarias…")
            .build()
        startForeground(1, notif)
        locationTracker.start()
    }

    override fun onDestroy() {
        locationTracker.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
