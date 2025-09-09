package com.example.reviewapp.network.mappers

import com.example.reviewapp.data.locals.PlaceEntity
import com.example.reviewapp.data.models.Place

/** Converte um `PlaceEntity` (Room) para modelo de domínio [Place]. */
fun PlaceEntity.toModel() = Place(
    id = id,
    name = name,
    category = null,
    phone = phone,
    lat = lat, lng = lng,
    address = address,
    avgRating = avgRating,
    ratingsCount = ratingsCount
)

/** Converte um [Place] para `PlaceEntity` para cache local, marcando `lastFetchedAt`. */
fun Place.toEntity(now: Long) = PlaceEntity(
    id = id,
    name = name,
    phone = phone,
    lat = lat, lng = lng,
    address = address,
    avgRating = avgRating,
    ratingsCount = ratingsCount,
    lastFetchedAt = now
)
