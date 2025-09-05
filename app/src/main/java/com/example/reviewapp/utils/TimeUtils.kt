package com.example.reviewapp.utils

import java.time.LocalTime

object TimeUtils {
    fun now() = System.currentTimeMillis()
    private val start = LocalTime.of(16, 0)
    private val end   = LocalTime.of(18, 0)
    fun isWithinPromoWindow(now: LocalTime = LocalTime.now()): Boolean =
        !now.isBefore(start) && !now.isAfter(end)}