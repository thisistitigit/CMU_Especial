package com.example.reviewapp.services

import android.content.Context
import com.example.reviewapp.utils.NotificationUtils
import com.example.reviewapp.utils.PermissionUtils

fun notifyNearby(context: Context, title: String, text: String): Boolean {
    NotificationUtils.ensureChannel(context)

    if (PermissionUtils.hasNotificationPermission(context)) {
        val notification = NotificationUtils.createNotification(context, title, text)
        return NotificationUtils.showNotification(context, notification)
    }

    return false
}