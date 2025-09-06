package com.example.reviewapp.network.dto

import androidx.annotation.Keep

@Keep
data class GooglePlaceDetailsResponse(
    val status: String? = null,
    val result: Result? = null
) {
    @Keep data class Result(
        val place_id: String? = null,
        val name: String? = null,
        val formatted_address: String? = null,
        val formatted_phone_number: String? = null,
        val geometry: Geometry? = null,
        val rating: Double? = null,
        val user_ratings_total: Int? = null,
        val types: List<String>? = null
    )
    @Keep data class Geometry(val location: Location? = null)
    @Keep data class Location(val lat: Double? = null, val lng: Double? = null)
}
