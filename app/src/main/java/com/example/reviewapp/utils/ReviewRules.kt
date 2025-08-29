package com.example.reviewapp.utils

object ReviewRules {
    private const val MIN_DISTANCE_METERS = 50.0
    private const val MIN_INTERVAL_MINUTES = 30L

    fun canReview(distanceMeters: Double, lastReviewAt: Long?, now: Long): Boolean {
        val okDist = distanceMeters <= MIN_DISTANCE_METERS
        val okTime = lastReviewAt?.let { now - it >= MIN_INTERVAL_MINUTES * 60_000 } ?: true
        return okDist && okTime
    }
}
