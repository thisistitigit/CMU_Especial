package com.example.cmu_especial.domain.model

data class Establishment(
    val id: String,
    val name: String,
    val phone: String? = null,
    val location: GeoPoint,
    val address: String? = null,
    val avgRating: Double = 0.0,
    val ratingsCount: Int = 0
)