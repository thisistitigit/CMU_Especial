package com.example.reviewapp.data.repository

import com.example.reviewapp.data.models.Place
import com.example.reviewapp.data.models.Review

interface ReviewRepository {
    suspend fun addReview(
        review: Review,
        userLat: Double,
        userLng: Double,
        now: Long = System.currentTimeMillis()
    )


    suspend fun latestReviews(placeId: String): List<Review>
    suspend fun history(userId: String): List<Review>
    suspend fun canUserReviewHere(
        userId: String, place: Place, now: Long, userLat: Double, userLng: Double
    ): Boolean
    suspend fun refreshPlaceReviews(placeId: String): List<Review>
    suspend fun refreshUserReviews(uid: String): List<Review>
    suspend fun getReview(id: String): Review?
    suspend fun allReviews(placeId: String): List<Review>
    suspend fun lastReviewAtByUser(userId: String): Long?
    fun currentUid(): String?
    suspend fun refreshAllReviews(maxToFetch: Int = 2000, pageSize: Int = 500): Int

    fun streamPlaceReviews(placeId: String): kotlinx.coroutines.flow.Flow<List<Review>>
    fun streamAllReviews(): kotlinx.coroutines.flow.Flow<List<Review>>

    fun streamUserHistory(uid: String): kotlinx.coroutines.flow.Flow<List<Review>>
    fun streamPlaceMetaFromReviews(placeId: String): kotlinx.coroutines.flow.Flow<Place?>



}