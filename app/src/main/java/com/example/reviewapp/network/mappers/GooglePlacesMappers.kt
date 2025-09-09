package com.example.reviewapp.network.mappers

import com.example.reviewapp.data.models.Place
import com.example.reviewapp.network.dto.GooglePlacesResponse

/**
 * Mapeia uma resposta de Nearby para uma lista de [Place] de domínio.
 *
 * Gera um `id` sintético `lat,lng:name` quando `place_id` não está presente
 * (de forma a permitir cache e cliques na UI), descartando resultados
 * incompletos (sem `lat|lng|name`).
 */
fun GooglePlacesResponse.toPlaces(): List<Place> =
    results.mapNotNull { r ->
        val lat = r.geometry?.location?.lat
        val lng = r.geometry?.location?.lng
        val name = r.name ?: r.vicinity
        val id = r.placeId ?: if (lat != null && lng != null && name != null) "$lat,$lng:$name" else null
        if (id == null || lat == null || lng == null || name == null) return@mapNotNull null

        Place(
            id = id,
            name = name,
            category = r.types?.firstOrNull(),
            phone = null,
            lat = lat,
            lng = lng,
            address = r.vicinity,
            avgRating = r.rating ?: 0.0,
            ratingsCount = r.userRatingsTotal ?: 0
        )
    }
