package com.example.cmu_especial.data.repository

import com.example.cmu_especial.data.local.dao.ReviewDao
import com.example.cmu_especial.data.remote.firebase.FirestoreService
import com.example.cmu_especial.domain.model.Review
import com.example.cmu_especial.domain.repository.ReviewRepository

class ReviewRepositoryImpl(
    private val dao: ReviewDao,
    private val fs: FirestoreService
) : ReviewRepository {
    override suspend fun add(review: Review) {
        dao.insert(review.toEntity())
        fs.upsertPublicReview(review.establishmentId, review)
    }
    override suspend fun recentForEstablishment(establishmentId: String, limit: Int) =
        dao.recentForEstablishment(establishmentId, limit).map { it.toDomain() }
    override suspend fun myHistory(userId: String) =
        dao.myHistory(userId).map { it.toDomain() }
    override suspend fun lastUserReviewTime(userId: String, establishmentId: String) =
        dao.lastUserReviewTime(userId, establishmentId)
}
