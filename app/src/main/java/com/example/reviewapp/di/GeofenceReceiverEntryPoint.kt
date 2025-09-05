// app/src/main/java/com/example/reviewapp/di/GeofenceReceiverEntryPoint.kt
package com.example.reviewapp.di

import com.example.reviewapp.data.dao.PlaceDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GeofenceReceiverEntryPoint {
    fun placeDao(): PlaceDao
}
