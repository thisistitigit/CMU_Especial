package com.example.cmu_especial.domain.usecase

import com.example.cmu_especial.domain.model.Establishment
import com.example.cmu_especial.domain.model.GeoPoint
import com.example.cmu_especial.domain.repository.EstablishmentRepository

/**
 * Pesquisa estabelecimentos perto de um ponto, respeitando o raio mínimo (≥ 250 m).
 */
class SearchEstablishments(
    private val repo: EstablishmentRepository
) {
    suspend operator fun invoke(center: GeoPoint, radiusMeters: Int): List<Establishment> {
        require(radiusMeters >= 250) { "O raio mínimo é 250 metros" }
        return repo.searchNearby(center, radiusMeters)
    }
}
