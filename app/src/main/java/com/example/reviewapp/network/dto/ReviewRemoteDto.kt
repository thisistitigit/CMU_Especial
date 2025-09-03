package com.example.reviewapp.network.dto

import androidx.annotation.Keep
import com.google.firebase.database.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class ReviewRemoteDto(
    var id: String = "",
    var placeId: String = "",
    var placeName: String = "",
    var placeAddress: String? = null,
    var userId: String = "",
    var userName: String = "",
    var pastryName: String = "",
    var stars: Int = 0,
    var comment: String = "",
    var createdAt: Long = 0L,
    var photoCloudUrl: String? = null

)

// ---- helpers ----
inline fun <K, V : Any> Map<K, V?>.toNonNullMap(): Map<K, V> =
    buildMap { for ((k, v) in this@toNonNullMap) if (v != null) put(k, v) }

// Converte o DTO num Map<String, Any> *sem* chaves nulas
fun ReviewRemoteDto.toMapNonNull(): Map<String, Any> = mapOf(
    "id" to id,
    "placeId" to placeId,
    "placeName" to placeName,
    "placeAddress" to placeAddress,
    "userId" to userId,
    "userName" to userName,
    "pastryName" to pastryName,
    "stars" to stars,
    "comment" to comment,
    "createdAt" to createdAt,
    "photoCloudUrl" to photoCloudUrl
).toNonNullMap()
