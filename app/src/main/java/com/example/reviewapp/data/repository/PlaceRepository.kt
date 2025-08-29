package com.example.reviewapp.data.repository

import com.example.reviewapp.data.models.Place

interface PlaceRepository {
    suspend fun searchAround(lat: Double, lng: Double, radiusMeters: Int = 250): List<Place>
    suspend fun getDetails(placeId: String): Place
    suspend fun leaderboard(limit: Int = 50): List<Place>
    // atalho opcional
    suspend fun nearby(lat: Double, lng: Double, radiusMeters: Int): List<Place> =
        searchAround(lat, lng, radiusMeters)

}
