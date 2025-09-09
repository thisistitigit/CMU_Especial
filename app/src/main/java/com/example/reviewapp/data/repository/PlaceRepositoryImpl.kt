package com.example.reviewapp.data.repository

import android.util.Log
import com.example.reviewapp.data.dao.PlaceDao
import com.example.reviewapp.data.enums.PlaceType
import com.example.reviewapp.data.models.Place
import com.example.reviewapp.network.api.GooglePlacesApi
import com.example.reviewapp.network.mappers.toEntity
import com.example.reviewapp.network.mappers.toModel
import com.example.reviewapp.network.mappers.toPlace
import com.example.reviewapp.network.mappers.toPlaces
import com.example.reviewapp.utils.TimeUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.math.cos
import javax.inject.Named

/**
 * Implementação de [PlaceRepository] com **Room + Google Places API**.
 *
 * Estratégia:
 * - tenta primeiro **cache** (bounding box);
 * - _fallback_ para chamadas remotas (Nearby + Details);
 * - guarda em cache com `lastFetchedAt` para invalidação futura.
 *
 * **Concorrência/quotas:** o enriquecimento usa `Semaphore(4)` para limitar
 * a taxa de _details_ em paralelo.
 *
 * @param placeDao DAO Room de locais.
 * @param api cliente Retrofit para Google Places.
 * @param googleApiKey chave de API.
 * @param firestore opcional, para integrações futuras (não essencial aqui).
 * @since 1.0
 */
class PlaceRepositoryImpl(
    private val placeDao: PlaceDao,
    private val api: GooglePlacesApi,
    @Named("MAPS_LOCALS_KEY") private val googleApiKey: String,
    private val firestore: FirebaseFirestore? = null
) : PlaceRepository {

    private fun String.mask(): String =
        if (length <= 8) "****" else "${take(4)}****${takeLast(4)}"

    private fun isLikelyGooglePlaceId(id: String): Boolean =
        !id.contains(",") && !id.contains(":")

    /** Heurística: precisa de *details* se faltar telefone ou `user_ratings_total == 0`. */
    private fun needsDetails(p: Place): Boolean =
        p.phone.isNullOrBlank() || p.ratingsCount == 0

    /** Merge conservador: privilegia campos válidos e não vazios. */
    private fun merge(base: Place, details: Place): Place =
        base.copy(
            name = if (base.name.isBlank()) details.name else base.name,
            address = base.address ?: details.address,
            phone = details.phone ?: base.phone,
            avgRating = if (details.ratingsCount > 0) details.avgRating else base.avgRating,
            ratingsCount = maxOf(base.ratingsCount, details.ratingsCount),
            lat = if (base.lat == 0.0) details.lat else base.lat,
            lng = if (base.lng == 0.0) details.lng else base.lng,
            category = base.category ?: details.category
        )

    /**
     * Busca locais numa área aproximada usando uma **bounding box** derivada do raio.
     * Tenta cache primeiro; se vazia, chama **Nearby Search** por cada tipo.
     */
    private suspend fun fetchAround(
        lat: Double,
        lng: Double,
        radiusMeters: Int,
        types: Set<PlaceType>
    ): List<Place> {
        // Conversões aproximadas graus/metros
        val dLat = radiusMeters / 111_320.0
        val dLng = radiusMeters / (111_320.0 * cos(Math.toRadians(lat)))
        val minLat = lat - dLat; val maxLat = lat + dLat
        val minLng = lng - dLng; val maxLng = lng + dLng

        // 1) Cache
        val cached = placeDao.listInBounds(minLat, maxLat, minLng, maxLng).map { it.toModel() }
        if (cached.isNotEmpty()) {
            return cached.sortedByDescending { it.avgRating }
        }

        // 2) Remoto: Nearby por type (merge sem duplicados)
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
                    Log.w("PlaceRepo", "Nearby status=${resp.status}")
                }
                val mapped = resp.toPlaces()
                mapped.forEach { merged[it.id] = it }
            }
            val list = merged.values.toList()
            placeDao.upsertAll(list.map { it.toEntity(TimeUtils.now()) })
            list
        }.getOrElse { emptyList() }
    }

    /** Busca **Place Details**, cacheia e devolve o modelo ou `null` em falha. */
    private suspend fun fetchDetailsAndCache(placeId: String): Place? =
        runCatching {
            val resp = api.details(
                placeId = placeId,
                apiKey = googleApiKey,
                fields = "place_id,name,formatted_address,formatted_phone_number,geometry,rating,user_ratings_total,types"
            )
            if (resp.status != "OK") return null
            val place = resp.toPlace() ?: return null
            placeDao.upsert(place.toEntity(TimeUtils.now()))
            place
        }.getOrNull()

    override suspend fun getDetails(placeId: String): Place {
        placeDao.get(placeId)?.let { return it.toModel() }
        val fetched = fetchDetailsAndCache(placeId)
        if (fetched != null) return fetched
        throw NoSuchElementException("Place $placeId não encontrado (Room/API)")
    }

    override suspend fun nearby(
        lat: Double,
        lng: Double,
        radiusMeters: Int,
        types: Set<PlaceType>
    ): List<Place> = fetchAround(lat, lng, radiusMeters, types)

    override suspend fun leaderboard(limit: Int): List<Place> =
        placeDao.leaderboard(limit).map { it.toModel() }

    override suspend fun enrichIds(placeIds: Collection<String>): Map<String, Place> {
        if (placeIds.isEmpty()) return emptyMap()

        // 1) Cache local
        val locals = placeIds.mapNotNull { id -> placeDao.get(id)?.toModel()?.let { id to it } }.toMap()

        // 2) Quais precisam de details?
        val toFetch = placeIds.filter { id ->
            val local = locals[id]
            (local == null || needsDetails(local)) && isLikelyGooglePlaceId(id)
        }.distinct()

        if (toFetch.isEmpty()) return locals

        // 3) Details em paralelo (limite 4)
        val sem = Semaphore(4)
        val fetched: Map<String, Place> = coroutineScope {
            toFetch.map { id ->
                async {
                    sem.withPermit {
                        val det = fetchDetailsAndCache(id)
                        if (det != null) id to det else null
                    }
                }
            }.awaitAll().filterNotNull().toMap()
        }

        // 4) Resolve melhor versão por ID
        val result = LinkedHashMap<String, Place>()
        for (id in placeIds) {
            val base = locals[id]
            val det = fetched[id]
            val best = when {
                base != null && det != null -> merge(base, det)
                base != null -> base
                det != null -> det
                else -> null
            }
            if (best != null) result[id] = best
        }
        return result
    }

    override suspend fun enrichPlaces(list: List<Place>): List<Place> {
        if (list.isEmpty()) return list
        val map = enrichIds(list.map { it.id })
        return list.map { base -> map[base.id]?.let { merge(base, it) } ?: base }
    }
}
