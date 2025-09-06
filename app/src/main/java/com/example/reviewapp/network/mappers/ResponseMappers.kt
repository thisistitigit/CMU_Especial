package com.example.reviewapp.network.mappers

import com.example.reviewapp.data.models.Place
import com.example.reviewapp.network.dto.GooglePlaceDetailsResponse


fun GooglePlaceDetailsResponse.toPlace(): Place? {
    val r = result ?: return null
    val id   = r.place_id ?: return null
    val lat  = r.geometry?.location?.lat ?: return null
    val lng  = r.geometry?.location?.lng ?: return null
    val name = r.name ?: r.formatted_address ?: return null

    return Place(
        id = id,
        name = name,
        category = r.types?.firstOrNull(),
        phone = r.formatted_phone_number,
        lat = lat,
        lng = lng,
        address = r.formatted_address,
        avgRating = r.rating ?: 0.0,
        ratingsCount = r.user_ratings_total ?: 0
    )
}
