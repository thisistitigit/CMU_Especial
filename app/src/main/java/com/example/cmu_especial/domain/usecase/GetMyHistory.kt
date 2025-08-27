package com.example.cmu_especial.domain.usecase

import com.example.cmu_especial.domain.model.Review
import com.example.cmu_especial.domain.repository.ReviewRepository
import com.example.cmu_especial.domain.repository.UserRepository

/**
 * Histórico de avaliações do utilizador (mais recentes primeiro).
 */
class GetMyHistory(
    private val reviewRepo: ReviewRepository,
    private val userRepo: UserRepository
) {
    suspend operator fun invoke(userId: String? = null): List<Review> {
        val uid = userId ?: userRepo.currentUserId()
        return reviewRepo.myHistory(uid)
    }
}
