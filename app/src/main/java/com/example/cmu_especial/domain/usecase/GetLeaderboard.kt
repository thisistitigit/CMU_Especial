package com.example.cmu_especial.domain.usecase

import com.example.cmu_especial.domain.model.LeaderboardEntry
import com.example.cmu_especial.domain.repository.EstablishmentRepository

/**
 * Obtém o leaderboard por média de avaliações.
 */
class GetLeaderboard(
    private val repo: EstablishmentRepository
) {
    suspend operator fun invoke(limit: Int = 50): List<LeaderboardEntry> {
        require(limit > 0) { "limit deve ser > 0" }
        return repo.topRated(limit)
    }
}
