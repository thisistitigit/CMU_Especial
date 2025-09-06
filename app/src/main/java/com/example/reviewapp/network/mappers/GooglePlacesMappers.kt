// network/mappers/GooglePlacesMappers.kt
package com.example.reviewapp.network.mappers

import com.example.reviewapp.data.models.Place
import com.example.reviewapp.network.dto.GooglePlaceDetailsResponse
import com.example.reviewapp.network.dto.GooglePlacesResponse

fun GooglePlacesResponse.toPlaces(): List<Place> =
    results.mapNotNull { r ->
        val lat = r.geometry?.location?.lat
        val lng = r.geometry?.location?.lng
        val name = r.name ?: r.vicinity
        val id = r.placeId ?: if (lat != null && lng != null && name != null) "$lat,$lng:$name" else null
        if (id == null || lat == null || lng == null || name == null) return@mapNotNull null

        Place(
            id = id,
            name = name,
            category = r.types?.firstOrNull(),
            phone = null,
            lat = lat,
            lng = lng,
            address = r.vicinity,
            avgRating = r.rating ?: 0.0,
            ratingsCount = r.userRatingsTotal ?: 0
        )
    }
