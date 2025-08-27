package com.example.cmu_especial.domain.usecase

import com.example.cmu_especial.domain.model.Review
import com.example.cmu_especial.domain.repository.ReviewRepository

class GetRecentReviews(
    private val repo: ReviewRepository
) {
    suspend operator fun invoke(establishmentId: String, limit: Int = 10): List<Review> {
        return repo.recentForEstablishment(establishmentId, limit)
    }
}
