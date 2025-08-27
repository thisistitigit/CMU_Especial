package com.example.cmu_especial.domain.usecase

import com.example.cmu_especial.domain.model.Establishment
import com.example.cmu_especial.services.GeofenceManager
import com.example.cmu_especial.services.NotificationManager

// 16–18h

class ScheduleProximityAlerts(
    private val geofence: GeofenceManager,
    private val notifications: NotificationManager
) {
    // ativa geofences e agenda janela 16h–18h
    fun enable(establishments: List<Establishment>) {
        geofence.register(establishments.map { it.location })
        notifications.setQuietHours(startHour = 16, endHour = 18) // notificar 16–18h ao estar <50m.
    }
}