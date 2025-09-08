package com.example.reviewapp.network.dto

import com.google.gson.annotations.SerializedName

data class GooglePlacesResponse(
    @SerializedName("results") val results: List<GooglePlace> = emptyList(),
    @SerializedName("status") val status: String? = null,
    @SerializedName("error_message") val errorMessage: String? = null
)

data class GooglePlace(
    @SerializedName("place_id") val placeId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("vicinity") val vicinity: String? = null,
    @SerializedName("geometry") val geometry: GPGeometry? = null,
    @SerializedName("rating") val rating: Double? = null,
    @SerializedName("user_ratings_total") val userRatingsTotal: Int? = null,
    @SerializedName("types") val types: List<String>? = null
)

data class GPGeometry(
    @SerializedName("location") val location: GPLocation? = null
)

data class GPLocation(
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lng") val lng: Double? = null
)
