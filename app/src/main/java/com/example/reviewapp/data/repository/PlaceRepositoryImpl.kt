package com.example.reviewapp.data.repository

import com.example.reviewapp.data.SearchConfig
import com.example.reviewapp.data.dao.PlaceDao
import com.example.reviewapp.data.enums.PlaceType
import com.example.reviewapp.data.locals.PlaceEntity
import com.example.reviewapp.data.models.Place
import com.example.reviewapp.network.api.GooglePlacesApi
import com.example.reviewapp.network.mappers.toPlaces
import com.example.reviewapp.utils.logD
import com.example.reviewapp.utils.logE
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.cos
import javax.inject.Named

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
        logD("Repo.fetchAround: lat=$lat lng=$lng r=$radiusMeters types=${types.map { it.name }}")
        logD("Repo.key=${googleApiKey.mask()}")

        // cache bounds baseado no raio
        val dLat = radiusMeters / 111_320.0
        val dLng = radiusMeters / (111_320.0 * cos(Math.toRadians(lat)))
        val minLat = lat - dLat; val maxLat = lat + dLat
        val minLng = lng - dLng; val maxLng = lng + dLng
        logD("Repo.bounds: lat[$minLat,$maxLat] lng[$minLng,$maxLng]")

        val cached = placeDao.listInBounds(minLat, maxLat, minLng, maxLng).map { it.toModel() }
        logD("Repo.cache: hit=${cached.size}")
        if (cached.isNotEmpty()) {
            cached.take(5).forEachIndexed { i, p ->
                logD("Repo.cache[$i]: ${p.name} @ ${p.lat},${p.lng} | ${"%.1f".format(p.avgRating)}★")
            }
            return cached.sortedByDescending { it.avgRating }
        }

        return runCatching {
            val loc = "$lat,$lng"
            val merged = LinkedHashMap<String, Place>()

            for (t in types) {
                // LOG antes do request
                logD("Repo.http[prep]: type=${t.apiName} url=/nearbysearch/json?location=$loc&radius=$radiusMeters&type=${t.apiName}")

                val resp = api.nearby(
                    location = loc,
                    radius   = radiusMeters,
                    type     = t.apiName,
                    keyword  = null,
                    apiKey   = googleApiKey
                )

                logD("Repo.http: type=${t.apiName} status=${resp.status} error=${resp.errorMessage} results=${resp.results.size}")

                if (resp.status != "OK" && resp.status != "ZERO_RESULTS") {
                    logE("Repo.http FAIL: type=${t.apiName} status=${resp.status} err=${resp.errorMessage}")
                }

                // Amostras do bruto
                resp.results.take(5).forEachIndexed { i, r ->
                    val la = r.geometry?.location?.lat
                    val lo = r.geometry?.location?.lng
                    logD("Repo.raw[$t,$i]: ${r.placeId} | ${r.name} | $la,$lo | rating=${r.rating}")
                }

                val mapped = resp.toPlaces()
                logD("Repo.map: type=${t.apiName} mapped=${mapped.size}")
                mapped.take(5).forEachIndexed { i, p ->
                    logD("Repo.map[$t,$i]: ${p.name} @ ${p.lat},${p.lng} | ${"%.1f".format(p.avgRating)}★ | ${p.address}")
                }
                mapped.forEach { merged[it.id] = it }
            }

            val list = merged.values.toList()
            logD("Repo.final: merged=${list.size}")
            list.take(10).forEachIndexed { i, p ->
                logD("Repo.final[$i]: ${p.name} @ ${p.lat},${p.lng} | ${"%.1f".format(p.avgRating)}★ | ${p.address}")
            }

            placeDao.upsertAll(list.map { it.toEntity(System.currentTimeMillis()) })
            list
        }.onFailure {
            logE("Repo.fetchAround - FAILED: ${it.message}", it)
        }.getOrElse { emptyList() }
    }



    override suspend fun nearby(
        lat: Double,
        lng: Double,
        radiusMeters: Int,
        types: Set<PlaceType>
    ): List<Place> = fetchAround(lat, lng, radiusMeters, types)

    override suspend fun searchAround(
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
}

// Room <-> Domain
private fun PlaceEntity.toModel() = Place(
    id = id,
    name = name,
    category = null,
    phone = phone,
    lat = lat, lng = lng,
    address = address,
    avgRating = avgRating,
    ratingsCount = ratingsCount
)

private fun Place.toEntity(now: Long) = PlaceEntity(
    id = id,
    name = name,
    phone = phone,
    lat = lat, lng = lng,
    address = address,
    avgRating = avgRating,
    ratingsCount = ratingsCount,
    lastFetchedAt = now
)
