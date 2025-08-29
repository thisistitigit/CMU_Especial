package com.example.reviewapp.data.locals

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String?,
    val lat: Double,
    val lng: Double,
    val address: String?,
    val avgRating: Double,
    val ratingsCount: Int,
    val lastFetchedAt: Long
)