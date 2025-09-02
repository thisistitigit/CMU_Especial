package com.example.reviewapp.data.repository

import com.example.reviewapp.data.models.Place
import com.example.reviewapp.data.models.Review

interface ReviewRepository {
    suspend fun addReview(review: Review)
    suspend fun latestReviews(placeId: String): List<Review>
    suspend fun history(userId: String): List<Review>
    suspend fun canUserReviewHere(userId: String, place: Place, now: Long): Boolean

    suspend fun getReview(id: String): Review?
    suspend fun allReviews(placeId: String): List<Review>
    suspend fun lastReviewAtByUser(userId: String): Long?
    fun currentUid(): String?

}