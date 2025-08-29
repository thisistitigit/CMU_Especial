package com.example.reviewapp.services

data class GeopifyPlacesResponse(
    val features: List<Feature> = emptyList()
) {
    data class Feature(
        val properties: Properties,
        val geometry: Geometry?
    )

    data class Properties(
        val place_id: String?,      // id estável da geoapify
        val name: String?,
        val formatted: String?,     // morada formatada
        val lat: Double?,           // alguns responses têm aqui
        val lon: Double?,           // mas preferimos geometry
        val phone: String?
    )

    data class Geometry(
        val type: String?,
        val coordinates: List<Double> // [lon, lat]
    )
}
