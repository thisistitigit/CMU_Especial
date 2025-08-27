package com.example.cmu_especial.domain.repository

import com.example.cmu_especial.domain.model.Review


interface ReviewRepository {
    suspend fun add(review: Review)
    suspend fun recentForEstablishment(establishmentId: String, limit: Int = 10): List<Review>
    suspend fun myHistory(userId: String): List<Review>
    suspend fun lastUserReviewTime(userId: String, establishmentId: String): Long?
}