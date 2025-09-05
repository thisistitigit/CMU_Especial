package com.example.reviewapp.services

import android.content.Context
import android.util.Log
import com.example.reviewapp.utils.NotificationUtils
import com.example.reviewapp.utils.PermissionUtils

fun notifyNearby(context: Context, title: String, text: String): Boolean {
    NotificationUtils.ensureChannel(context)

    if (PermissionUtils.hasNotificationPermission(context)) {
        val notification = NotificationUtils.createNotification(context, title, text)
        val ok = NotificationUtils.showNotification(context, notification)
         Log.d("Geofence","notificação mostrada? $ok")

        return ok
    }

    return false
}