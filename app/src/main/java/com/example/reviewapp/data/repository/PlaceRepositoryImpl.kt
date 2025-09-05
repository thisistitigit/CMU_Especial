package com.example.reviewapp.data.repository

import com.example.reviewapp.data.dao.PlaceDao
import com.example.reviewapp.data.enums.PlaceType
import com.example.reviewapp.data.models.Place
import com.example.reviewapp.network.api.GooglePlacesApi
import com.example.reviewapp.network.mappers.toEntity
import com.example.reviewapp.network.mappers.toModel
import com.example.reviewapp.network.mappers.toPlaces
import com.example.reviewapp.utils.TimeUtils
import com.google.firebase.firestore.FirebaseFirestore

import kotlin.math.cos
import javax.inject.Named
import kotlin.text.get

class PlaceRepositoryImpl(
    private val placeDao: PlaceDao,
    private val api: GooglePlacesApi,
    @Named("MAPS_LOCALS_KEY") private val googleApiKey: String,
    private val firestore: FirebaseFirestore? = null
) : PlaceRepository {

    private fun String.mask(): String =
        if (length <= 8) "****" else "${take(4)}****${takeLast(4)}"


    private suspend fun fetchAround(
        lat: Double,
        lng: Double,
        radiusMeters: Int,
        types: Set<PlaceType>
    ): List<Place> {

        // cache bounds baseado no raio
        val dLat = radiusMeters / 111_320.0
        val dLng = radiusMeters / (111_320.0 * cos(Math.toRadians(lat)))
        val minLat = lat - dLat; val maxLat = lat + dLat
        val minLng = lng - dLng; val maxLng = lng + dLng

        val cached = placeDao.listInBounds(minLat, maxLat, minLng, maxLng).map { it.toModel() }
        if (cached.isNotEmpty()) {
            cached.take(5).forEachIndexed { i, p ->
            }
            return cached.sortedByDescending { it.avgRating }
        }

        return runCatching {
            val loc = "$lat,$lng"
            val merged = LinkedHashMap<String, Place>()

            for (t in types) {
                val resp = api.nearby(
                    location = loc,
                    radius   = radiusMeters,
                    type     = t.apiName,
                    keyword  = null,
                    apiKey   = googleApiKey
                )

                if (resp.status != "OK" && resp.status != "ZERO_RESULTS") {
                }

                // Amostras do bruto
                resp.results.take(5).forEachIndexed { i, r ->
                    val la = r.geometry?.location?.lat
                    val lo = r.geometry?.location?.lng
                }

                val mapped = resp.toPlaces()
                mapped.take(5).forEachIndexed { i, p ->
                }
                mapped.forEach { merged[it.id] = it }
            }

            val list = merged.values.toList()
            list.take(10).forEachIndexed { i, p ->
            }

            placeDao.upsertAll(list.map { it.toEntity(TimeUtils.now()) })
            list
        }.onFailure {
        }.getOrElse { emptyList() }
    }



    override suspend fun nearby(
        lat: Double,
        lng: Double,
        radiusMeters: Int,
        types: Set<PlaceType>
    ): List<Place> = fetchAround(lat, lng, radiusMeters, types)

    override suspend fun getDetails(placeId: String): Place {
        placeDao.get(placeId)?.let { return it.toModel() }
        throw NoSuchElementException("Place $placeId não encontrado localmente")
    }

    override suspend fun leaderboard(limit: Int): List<Place> =
        placeDao.leaderboard(limit).map { it.toModel() }

    override suspend fun searchAround(
        lat: Double,
        lng: Double,
        radiusMeters: Int,
        types: Set<PlaceType>
    ): List<Place> = fetchAround(lat, lng, radiusMeters, types)


}


