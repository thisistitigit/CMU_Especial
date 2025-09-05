package com.example.reviewapp.network.mappers

import com.example.reviewapp.data.locals.ReviewEntity
import com.example.reviewapp.data.models.Review
import com.example.reviewapp.network.dto.ReviewRemoteDto

// ---- Mapeadores ----
fun Review.toEntity() = ReviewEntity(
   id = id,
   placeId = placeId,
   placeName = placeName,           // NOVO
   placeAddress = placeAddress,     // NOVO
   userId = userId,
   userName = userName,
   pastryName = pastryName,
   stars = stars,
   comment = comment,
   photoLocalPath = photoLocalPath,
   photoCloudUrl = photoCloudUrl,
   createdAt = createdAt
)



fun ReviewEntity.toModel() = Review(
   id = id,
   placeId = placeId,
   placeName = placeName,           // NOVO
   placeAddress = placeAddress,     // NOVO
   userId = userId,
   userName = userName,
   pastryName = pastryName,
   stars = stars,
   comment = comment,
   photoLocalPath = photoLocalPath,
   photoCloudUrl = photoCloudUrl,
   createdAt = createdAt
)

fun Review.toRemoteDto(
   photoUrl: String?,
   placeName: String?,
   placeAddress: String?
) = ReviewRemoteDto(
   id = id,
   placeId = placeId,
   placeName = placeName ?: this.placeName.orEmpty(),   // garante envio
   placeAddress = placeAddress ?: this.placeAddress,
   userId = userId,
   userName = userName,
   pastryName = pastryName,
   stars = stars,
   comment = comment,
   createdAt = createdAt,
   photoCloudUrl = photoUrl
)

