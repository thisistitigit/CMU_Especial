package com.example.cmu_especial.domain.model

data class Review(
    val id: String,
    val userId: String,
    val userName: String,
    val establishmentId: String,
    val dessert: DessertType,
    val rating: Int, // 1..5
    val comment: String?,
    val photoLocalPath: String?, // privado ao utilizador
    val photoCloudUrl: String?,  // opcional público
    val createdAt: Long,         // epoch ms
    val location: GeoPoint
)