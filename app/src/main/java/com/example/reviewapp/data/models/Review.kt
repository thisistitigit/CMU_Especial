package com.example.reviewapp.data.models

import java.util.UUID

data class Review(
    val id: String = UUID.randomUUID().toString(),
    val placeId: String,
    val userId: String,
    val userName: String,
    val pastryName: String,
    val stars: Int,              // 1..5
    val comment: String,
    val photoLocalPath: String?, // privada no device
    val photoCloudUrl: String?,  // opcional (público)
    val createdAt: Long          // epoch millis
)