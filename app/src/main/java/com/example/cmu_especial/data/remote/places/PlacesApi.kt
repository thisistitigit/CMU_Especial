package com.example.cmu_especial.data.remote.places
data class PlaceDto(
    val id: String,
    val name: String,
    val phone: String?,
    val lat: Double,
    val lon: Double,
    val address: String?
)

interface PlacesApi {
    suspend fun search(lat: Double, lon: Double, radius: Int): List<PlaceDto>
}
