package com.example.cmu_especial.domain.usecase

import com.example.cmu_especial.domain.model.Review
import com.example.cmu_especial.domain.repository.ReviewRepository

/**
 * Adiciona uma avaliação (grava local e sincroniza remoto via repo).
 */
class AddReview(
    private val repo: ReviewRepository
) {
    suspend operator fun invoke(review: Review) {
        // Pré-condições (validação simples)
        require(review.rating in 1..5) { "Rating inválido" }
        require(review.userId.isNotBlank()) { "userId em falta" }
        require(review.establishmentId.isNotBlank()) { "establishmentId em falta" }

        // Delegar no repositório (Room + Firestore/Storage)
        repo.add(review)
    }
}
