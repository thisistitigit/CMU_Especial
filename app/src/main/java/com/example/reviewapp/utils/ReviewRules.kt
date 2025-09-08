package com.example.reviewapp.utils

import android.location.Location

object ReviewRules {
     const val MIN_DISTANCE_METERS = 250.0
     const val MIN_INTERVAL_MINUTES = 30L

    fun canReview(distanceMeters: Double, lastReviewAt: Long?, now: Long): Boolean {
        val okDist = distanceMeters <= MIN_DISTANCE_METERS
        val okTime = lastReviewAt?.let { now - it >= MIN_INTERVAL_MINUTES * 60_000 } ?: true
        return okDist && okTime
    }

     fun distanceMeters(
        userLat: Double, userLng: Double,
        placeLat: Double, placeLng: Double
    ): Double {
        val r = FloatArray(1)
        Location.distanceBetween(userLat, userLng, placeLat, placeLng, r)
        return r[0].toDouble()
    }


}
