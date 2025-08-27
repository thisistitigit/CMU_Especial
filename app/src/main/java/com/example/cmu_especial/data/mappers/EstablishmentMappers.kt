package com.example.cmu_especial.data.mappers

import com.example.cmu_especial.data.entities.EstablishmentEntity
import com.example.cmu_especial.data.remote.places.PlaceDto
import com.example.cmu_especial.domain.model.Establishment
import com.example.cmu_especial.domain.model.GeoPoint

fun PlaceDto.toEntity() = EstablishmentEntity(
    id = id,
    name = name,
    phone = phone,
    lat = lat, lon = lon,
    address = address,
    avgRating = 0.0,
    ratingsCount = 0
)

fun EstablishmentEntity.toDomain() = Establishment(
    id = id,
    name = name,
    phone = phone,
    location = GeoPoint(lat, lon),
    address = address,
    avgRating = avgRating,
    ratingsCount = ratingsCount
)
