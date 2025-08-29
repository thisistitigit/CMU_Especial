package com.example.reviewapp.network.api

import com.example.reviewapp.services.GeopifyPlacesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GeopifyApi {
    @GET("v2/places")
    suspend fun getPlaces(
        @Query("categories") categories: String,
        @Query("filter") filter: String,   // "circle:lng,lat,radius"
        @Query("limit") limit: Int,
        @Query("apiKey") apiKey: String    // <-- sem default
    ): GeopifyPlacesResponse
}
