package com.example.cmu_especial.domain.usecase
import com.example.cmu_especial.domain.model.Establishment
import com.example.cmu_especial.domain.repository.EstablishmentRepository

class GetEstablishmentDetails(
    private val repo: EstablishmentRepository
) {
    suspend operator fun invoke(id: String): Establishment {
        return repo.getById(id)
    }
}
