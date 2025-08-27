package com.example.cmu_especial.domain.usecase

import com.example.cmu_especial.domain.model.GeoPoint
import com.example.cmu_especial.domain.repository.ReviewRepository

// regra 50m/30min

class CanUserReviewNow(
    private val reviewRepo: ReviewRepository
) {
    suspend operator fun invoke(
        userId: String,
        establishmentId: String,
        userLocation: GeoPoint,
        establishmentLocation: GeoPoint,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        val dist = Geo.distanceMeters(userLocation, establishmentLocation)
        if (dist > 50.0) return false // a avaliação exige < 50m, e ≥30min desde a última.

        val last = reviewRepo.lastUserReviewTime(userId, establishmentId)
        val minutesSince = last?.let { (now - it) / (60_000) } ?: Long.MAX_VALUE
        return minutesSince >= 30 // ≥30 minutos entre avaliações do utilizador.
    }
}