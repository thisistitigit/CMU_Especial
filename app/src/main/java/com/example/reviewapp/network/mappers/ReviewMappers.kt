package com.example.reviewapp.network.mappers

import com.example.reviewapp.data.models.Review
import com.example.reviewapp.network.dto.ReviewRemoteDto

// ---- Mapeadores ----
 fun Review.toEntity() = com.example.reviewapp.data.locals.ReviewEntity(
    id = id,
    placeId = placeId,
    userId = userId,
    userName = userName,
    pastryName = pastryName,
    stars = stars,
    comment = comment,
    photoLocalPath = photoLocalPath,
    photoCloudUrl = photoCloudUrl,
    createdAt = createdAt
)

 fun com.example.reviewapp.data.locals.ReviewEntity.toModel() = Review(
    id = id,
    placeId = placeId,
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
   placeName = placeName ?: "",          // <- novo
   placeAddress = placeAddress,          // <- novo
   userId = userId,
   userName = userName,
   pastryName = pastryName,
   stars = stars,
   comment = comment,
   createdAt = createdAt,
   photoCloudUrl = photoUrl
)

