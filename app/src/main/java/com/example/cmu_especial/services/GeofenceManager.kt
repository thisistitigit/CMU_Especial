package com.example.cmu_especial.services

import com.example.cmu_especial.domain.model.GeoPoint
import jakarta.inject.Inject

class GeofenceManager @Inject constructor(/* FusedLocationProviderClient, GeofencingClient */) {
    fun register(points: List<GeoPoint>) { /* cria geofences com raio 50m */ }
}