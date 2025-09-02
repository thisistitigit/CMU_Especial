package com.example.reviewapp.network.mappers

import com.example.reviewapp.data.models.Review
import com.example.reviewapp.network.dto.ReviewRemoteDto

fun Review.toRemoteDto(photoUrl: String?) = ReviewRemoteDto(
    id = id,
    placeId = placeId,
    userId = userId,
    userName = userName,
    pastryName = pastryName,
    stars = stars,
    comment = comment,
    createdAt = createdAt,
    photoCloudUrl = photoUrl // pode ser null; será omitido no toMapNonNull()
)