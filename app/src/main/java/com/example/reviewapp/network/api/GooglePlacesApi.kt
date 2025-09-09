package com.example.reviewapp.network.api

import com.example.reviewapp.network.dto.GooglePlaceDetailsResponse
import com.example.reviewapp.network.dto.GooglePlacesResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Cliente Retrofit para a **Google Places API** (subset Nearby + Details).
 *
 * Base URL esperada: `https://maps.googleapis.com/maps/api/place/`
 *
 * @since 1.0
 */
interface GooglePlacesApi {

    /**
     * Nearby Search por coordenadas.
     *
     * @param location `"lat,lng"` (p.ex. `"41.15,-8.61"`).
     * @param radius raio em metros (máx. recomendado pela Google: 50.000).
     * @param type tipo de place (ex.: `"bakery"`, `"cafe"`).
     * @param keyword palavra-chave adicional (opcional).
     * @param apiKey chave da Google Maps Platform.
     */
    @GET("nearbysearch/json")
    suspend fun nearby(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("type") type: String? = null,
        @Query("keyword") keyword: String? = null,
        @Query("key") apiKey: String
    ): GooglePlacesResponse

    /**
     * Place Details por `place_id`.
     *
     * @param placeId identificador do local.
     * @param fields lista de campos pretendidos (por omissão um conjunto mínimo útil).
     * @param apiKey chave da Google Maps Platform.
     */
    @GET("details/json")
    suspend fun details(
        @Query("place_id") placeId: String,
        @Query("fields")
        fields: String = "place_id,name,formatted_address,formatted_phone_number,geometry/location,rating,user_ratings_total,types",
        @Query("key") apiKey: String
    ): GooglePlaceDetailsResponse
}
