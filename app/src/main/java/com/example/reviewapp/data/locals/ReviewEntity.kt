package com.example.reviewapp.data.locals

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reviews",
    indices = [Index("placeId"), Index("userId")]
)
data class ReviewEntity(
    @PrimaryKey val id: String,
    val placeId: String,
    val placeName: String?,
    val placeAddress: String?,
    val userId: String,
    val userName: String,
    val pastryName: String,
    val stars: Int,
    val comment: String,
    val photoLocalPath: String?,
    val photoCloudUrl: String?,
    val createdAt: Long
)