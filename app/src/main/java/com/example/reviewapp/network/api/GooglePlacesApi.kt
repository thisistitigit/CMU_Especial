package com.example.reviewapp.network.api

import com.example.reviewapp.network.dto.GooglePlaceDetailsResponse
import com.example.reviewapp.network.dto.GooglePlacesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GooglePlacesApi {
    @GET("nearbysearch/json")
    suspend fun nearby(
        @Query("location") location: String,   // "lat,lng"
        @Query("radius") radius: Int,         // em metros (máx 50 000)
        @Query("type") type: String? = null,  // e.g., "bakery" ou "cafe"
        @Query("keyword") keyword: String? = null,
        @Query("key") apiKey: String
    ): GooglePlacesResponse
    @GET("details/json")
    suspend fun details(
        @Query("place_id") placeId: String,
        // só pedimos o que vamos guardar — poupa quota
        @Query("fields")
        fields: String = "place_id,name,formatted_address,formatted_phone_number,geometry/location,rating,user_ratings_total,types",
        @Query("key") apiKey: String
    ): GooglePlaceDetailsResponse
}
