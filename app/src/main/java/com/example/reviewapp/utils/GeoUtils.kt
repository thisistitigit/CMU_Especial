package com.example.reviewapp.utils

import android.location.Location

object GeoUtils {
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val res = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, res)
        return res[0].toDouble()
    }
}