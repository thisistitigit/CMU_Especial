package com.example.reviewapp.services

import android.content.Context
import android.util.Log
import com.example.reviewapp.utils.NotificationUtils
import com.example.reviewapp.utils.PermissionUtils

/**
 * Mostra uma notificação "Estás perto: <placeName>" com tile custom
 * (cores por tema, logo lilás) usando NotificationUtils.
 */
fun notifyNearby(context: Context, placeName: String?): Boolean {
    NotificationUtils.ensureChannel(context)

    if (!PermissionUtils.hasNotificationPermission(context)) {
        Log.w("Geofence", "Sem permissão de notificações.")
        return false
    }

    val notification = NotificationUtils.createNotification(
        context = context,
        placeName = placeName
    )

    val ok = NotificationUtils.showNotification(context, notification)
    Log.d("Geofence", "notificação mostrada? $ok")
    return ok
}
