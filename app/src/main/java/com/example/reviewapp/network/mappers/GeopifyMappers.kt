package com.example.reviewapp.network.mappers

import com.example.reviewapp.data.models.Place
import com.example.reviewapp.services.GeopifyPlacesResponse

fun GeopifyPlacesResponse.toPlaces(): List<Place> =
    features.mapNotNull { f ->
        val props = f.properties
        val coords = f.geometry?.coordinates
        val lon = coords?.getOrNull(0) ?: props.lon
        val lat = coords?.getOrNull(1) ?: props.lat
        if (lat == null || lon == null) return@mapNotNull null

        Place(
            id = props.place_id ?: "${props.name}:${lat},${lon}",
            name = props.name ?: "Sem nome",
            category = null,
            phone = props.phone,
            lat = lat,
            lng = lon,
            address = props.formatted,
            avgRating = 0.0,
            ratingsCount = 0
        )
    }
