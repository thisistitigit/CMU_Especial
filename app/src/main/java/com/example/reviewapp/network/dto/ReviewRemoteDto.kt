// ex: com/example/reviewapp/data/remote/dto/ReviewRemoteDto.kt
package com.example.reviewapp.data.remote.dto

data class ReviewRemoteDto(
    val id: String,
    val placeId: String,
    val userId: String,
    val userName: String,
    val pastryName: String,
    val stars: Int,
    val comment: String,
    val createdAt: Long,
    val photoCloudUrl: String? = null
)

// ---- helpers ----
inline fun <K, V : Any> Map<K, V?>.toNonNullMap(): Map<K, V> =
    buildMap { for ((k, v) in this@toNonNullMap) if (v != null) put(k, v) }

// Converte o DTO num Map<String, Any> *sem* chaves nulas
fun ReviewRemoteDto.toMapNonNull(): Map<String, Any> = mapOf(
    "id" to id,
    "placeId" to placeId,
    "userId" to userId,
    "userName" to userName,
    "pastryName" to pastryName,
    "stars" to stars,
    "comment" to comment,
    "createdAt" to createdAt,
    "photoCloudUrl" to photoCloudUrl
).toNonNullMap()
