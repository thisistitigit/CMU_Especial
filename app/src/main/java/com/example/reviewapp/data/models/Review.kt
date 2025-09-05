package com.example.reviewapp.data.models

import java.util.UUID

data class Review(
    val id: String = UUID.randomUUID().toString(),
    val placeId: String,
    val userId: String,
    val userName: String,
    val pastryName: String,
    val stars: Int,
    val comment: String,
    val photoLocalPath: String?,
    val photoCloudUrl: String?,
    val createdAt: Long,
    val placeName: String? = null,
    val placeAddress: String? = null,
)