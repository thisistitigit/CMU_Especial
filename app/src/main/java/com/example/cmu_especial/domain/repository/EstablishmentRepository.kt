package com.example.cmu_especial.domain.repository

import com.example.cmu_especial.domain.model.Establishment
import com.example.cmu_especial.domain.model.GeoPoint
import com.example.cmu_especial.domain.model.LeaderboardEntry

interface EstablishmentRepository {
    suspend fun searchNearby(center: GeoPoint, radiusMeters: Int): List<Establishment>
    suspend fun getById(id: String): Establishment
    suspend fun topRated(limit: Int): List<LeaderboardEntry>
}
