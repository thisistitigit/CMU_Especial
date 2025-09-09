package com.example.reviewapp.network.mappers

import com.example.reviewapp.data.locals.ReviewEntity
import com.example.reviewapp.data.models.Review

/** Converte [Review] (domínio) para `ReviewEntity` (Room). */
fun Review.toEntity() = ReviewEntity(
   id = id,
   placeId = placeId,
   placeName = placeName,
   placeAddress = placeAddress,
   userId = userId,
   userName = userName,
   pastryName = pastryName,
   stars = stars,
   comment = comment,
   photoLocalPath = photoLocalPath,
   photoCloudUrl = photoCloudUrl,
   createdAt = createdAt
)

/** Converte `ReviewEntity` (Room) para [Review] (domínio). */
fun ReviewEntity.toModel() = Review(
   id = id,
   placeId = placeId,
   placeName = placeName,
   placeAddress = placeAddress,
   userId = userId,
   userName = userName,
   pastryName = pastryName,
   stars = stars,
   comment = comment,
   photoLocalPath = photoLocalPath,
   photoCloudUrl = photoCloudUrl,
   createdAt = createdAt
)
