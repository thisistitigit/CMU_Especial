package com.example.reviewapp.network.api

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
}
