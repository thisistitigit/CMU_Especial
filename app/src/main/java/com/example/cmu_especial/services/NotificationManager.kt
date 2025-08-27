package com.example.cmu_especial.services

import android.content.Context
import com.example.cmu_especial.domain.model.Establishment
import jakarta.inject.Inject

class NotificationManager @Inject constructor(private val ctx: Context) {
    private var quietStart = 16; private var quietEnd = 18
    fun setQuietHours(startHour: Int, endHour: Int) { quietStart = startHour; quietEnd = endHour }
    fun notifyProximity(est: Establishment) { /* só entre 16–18h, <50m */ }
}
