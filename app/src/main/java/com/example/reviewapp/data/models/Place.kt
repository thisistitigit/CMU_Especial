package com.example.reviewapp.data.models

data class Place(
    val id: String,              // estável (API ou hash lat/lng+nome)
    val name: String,
    val category: String?,
    val phone: String?,
    val lat: Double,
    val lng: Double,
    val address: String?,
    val avgRating: Double = 0.0,
    val ratingsCount: Int = 0
)
