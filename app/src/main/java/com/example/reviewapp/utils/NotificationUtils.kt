// app/src/main/java/com/example/reviewapp/utils/NotificationUtils.kt
package com.example.reviewapp.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.reviewapp.R

object NotificationUtils {
    private const val NEARBY_CHANNEL_ID = "nearby_reviews"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(NEARBY_CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    NEARBY_CHANNEL_ID,
                    "Estabelecimentos por perto",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Alertas quando estás perto de uma pastelaria com reviews" }
                mgr.createNotificationChannel(ch)
            }
        }
    }

    fun createNotification(context: Context, title: String, text: String): Notification {
        return NotificationCompat.Builder(context, NEARBY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    fun showNotification(context: Context, notification: Notification): Boolean {
        return try {
            with(NotificationManagerCompat.from(context)) {
                notify(System.currentTimeMillis().toInt(), notification)
            }
            true
        } catch (e: SecurityException) {
            false
        }
    }
}