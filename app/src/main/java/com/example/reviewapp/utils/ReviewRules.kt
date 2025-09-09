package com.example.reviewapp.utils

import android.location.Location

/**
 * Regras de negócio para submissão de **reviews** com anti-abuso.
 *
 * - Distância mínima ao local: [MIN_DISTANCE_METERS] (garante presença física);
 * - Intervalo mínimo entre reviews do mesmo utilizador: [MIN_INTERVAL_MINUTES].
 *
 * A distância usa o utilitário do Android `Location.distanceBetween` (Haversine).
 */
object ReviewRules {
    /** Raio de proximidade obrigatório (m). */
    const val MIN_DISTANCE_METERS = 250.0
    /** Intervalo mínimo entre reviews (minutos). */
    const val MIN_INTERVAL_MINUTES = 30L

    /**
     * Verifica se o utilizador pode submeter review agora.
     *
     * @param distanceMeters distância atual ao local (m).
     * @param lastReviewAt epoch millis da última review do utilizador (ou `null` se nenhuma).
     * @param now epoch millis atual.
     */
    fun canReview(distanceMeters: Double, lastReviewAt: Long?, now: Long): Boolean {
        val okDist = distanceMeters <= MIN_DISTANCE_METERS
        val okTime = lastReviewAt?.let { now - it >= MIN_INTERVAL_MINUTES * 60_000 } ?: true
        return okDist && okTime
    }

    /**
     * Distância em metros entre utilizador e local (rápida e fiável).
     */
    fun distanceMeters(
        userLat: Double, userLng: Double,
        placeLat: Double, placeLng: Double
    ): Double {
        val r = FloatArray(1)
        Location.distanceBetween(userLat, userLng, placeLat, placeLng, r)
        return r[0].toDouble()
    }
}
