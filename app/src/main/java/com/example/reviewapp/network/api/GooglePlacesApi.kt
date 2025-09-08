package com.example.reviewapp.network.api

import com.example.reviewapp.network.dto.GooglePlaceDetailsResponse
import com.example.reviewapp.network.dto.GooglePlacesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GooglePlacesApi {
    @GET("nearbysearch/json")
    suspend fun nearby(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("type") type: String? = null,
        @Query("keyword") keyword: String? = null,
        @Query("key") apiKey: String
    ): GooglePlacesResponse
    @GET("details/json")
    suspend fun details(
        @Query("place_id") placeId: String,
        @Query("fields")
        fields: String = "place_id,name,formatted_address,formatted_phone_number,geometry/location,rating,user_ratings_total,types",
        @Query("key") apiKey: String
    ): GooglePlaceDetailsResponse
}
