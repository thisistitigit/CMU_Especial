package com.example.cmu_especial.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// data/local/entities/RoomEntities.kt
@Entity(tableName = "establishments")
data class EstablishmentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String?,
    val lat: Double, val lon: Double,
    val address: String?,
    val avgRating: Double,
    val ratingsCount: Int
)

@Entity(
    tableName = "reviews",
    indices = [Index("establishmentId"), Index("userId")]
)
data class ReviewEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val establishmentId: String,
    val dessert: String,
    val rating: Int,
    val comment: String?,
    val photoLocalPath: String?,
    val photoCloudUrl: String?,
    val createdAt: Long,
    val lat: Double, val lon: Double
)
