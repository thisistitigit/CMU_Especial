package com.example.reviewapp.data.repository

import com.example.reviewapp.data.enums.PlaceType
import com.example.reviewapp.data.models.Place
import com.google.android.gms.maps.model.LatLng

interface PlaceRepository {

    // 2.5 km por omissão; foco em consumo (bakery/cafe/restaurant/takeaway/delivery)
    suspend fun nearby(
        lat: Double,
        lng: Double,
        radiusMeters: Int = 5000,
        types: Set<PlaceType> = PlaceType.DEFAULT
    ): List<Place>
    suspend fun getDetails(placeId: String): Place
    suspend fun leaderboard(limit: Int = 50): List<Place>

    suspend fun searchAround(
        lat: Double,
        lng: Double,
        radiusMeters: Int = 5000,
        types: Set<PlaceType> = PlaceType.DEFAULT
    ): List<Place>
}
