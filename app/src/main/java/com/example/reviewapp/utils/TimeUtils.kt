package com.example.reviewapp.utils

import kotlin.math.abs


object TimeUtils {
    fun now() = System.currentTimeMillis()
    fun minutesBetween(a: Long, b: Long) = abs(a - b) / 60_000
}