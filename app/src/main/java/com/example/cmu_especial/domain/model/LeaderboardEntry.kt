package com.example.cmu_especial.domain.model

// domain/model/LeaderboardEntry.kt
data class LeaderboardEntry(
    val establishment: Establishment,
    val avgRating: Double,
    val count: Int
)