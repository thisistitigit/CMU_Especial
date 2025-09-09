package com.example.reviewapp.di

import com.example.reviewapp.data.dao.PlaceDao
import com.example.reviewapp.geofence.GeofenceRegistrar
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * **Hilt EntryPoint** usado por `BroadcastReceiver`s sem injeção direta,
 * permitindo obter dependências do grafo de aplicação.
 *
 * Usado por:
 * - [com.example.reviewapp.geofence.GeofenceReceiver]
 * - [com.example.reviewapp.geofence.LocationUpdateReceiver]
 *
 * @since 1.0
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface GeofenceReceiverEntryPoint {
    /** DAO de locais para consulta rápida de metadata a partir de receivers. */
    fun placeDao(): PlaceDao

    /** Serviço de registo/refresh de geofences. */
    fun geofenceRegistrar(): GeofenceRegistrar
}
