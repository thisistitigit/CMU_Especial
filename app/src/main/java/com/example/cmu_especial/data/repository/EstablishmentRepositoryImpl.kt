package com.example.cmu_especial.data.repository

import com.example.cmu_especial.data.local.dao.EstablishmentDao
import com.example.cmu_especial.data.mappers.toDomain
import com.example.cmu_especial.data.mappers.toEntity
import com.example.cmu_especial.data.remote.places.PlacesApi
import com.example.cmu_especial.domain.model.Establishment
import com.example.cmu_especial.domain.model.GeoPoint
import com.example.cmu_especial.domain.model.LeaderboardEntry
import com.example.cmu_especial.domain.repository.EstablishmentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EstablishmentRepositoryImpl @Inject constructor(
    private val dao: EstablishmentDao,
    private val placesApi: PlacesApi
) : EstablishmentRepository {

    override suspend fun searchNearby(center: GeoPoint, radiusMeters: Int): List<Establishment> {
        val remote = placesApi.search(center.lat, center.lon, radiusMeters)
        if (remote.isNotEmpty()) {
            val entities = remote.map { it.toEntity() }.toTypedArray()
            dao.upsertAll(*entities)
        }
        return dao.topRated(limit = 100).map { it.toDomain() }
    }

    override suspend fun getById(id: String): Establishment {
        val e = dao.getById(id)
        return e.toDomain()
    }

    override suspend fun topRated(limit: Int): List<LeaderboardEntry> {
        return dao.topRated(limit).map {
            LeaderboardEntry(
                establishment = it.toDomain(),
                avgRating = it.avgRating,
                count = it.ratingsCount
            )
        }
    }
}
