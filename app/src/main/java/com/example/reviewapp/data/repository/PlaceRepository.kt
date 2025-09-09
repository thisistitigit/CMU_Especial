package com.example.reviewapp.data.repository

import com.example.reviewapp.data.enums.PlaceType
import com.example.reviewapp.data.models.Place
import com.google.android.gms.maps.model.LatLng
/**
 * Contrato de acesso a locais (Places) combinando cache local e fontes remotas.
 *
 * Responsável por:
 * - _nearby search_ por tipos;
 * - detalhes de um local;
 * - leaderboard local;
 * - enriquecimento de listas com _details_ (lazy).
 *
 * Implementação de referência: [PlaceRepositoryImpl].
 *
 * @since 1.0
 */
interface PlaceRepository {

    /**
     * Pesquisa locais próximos.
     *
     * @param lat latitude do utilizador.
     * @param lng longitude do utilizador.
     * @param radiusMeters raio de pesquisa em metros (default: 5000).
     * @param types conjunto de tipos (Google Places) a considerar.
     * @return lista de locais.
     */
    suspend fun nearby(
        lat: Double,
        lng: Double,
        radiusMeters: Int = 5000,
        types: Set<PlaceType> = PlaceType.DEFAULT
    ): List<Place>

    /** Lê (ou busca e _cacheia_) os detalhes de um local por `placeId`. */
    suspend fun getDetails(placeId: String): Place

    /** Leaderboard por média e contagem de ratings (cache local). */

    suspend fun leaderboard(limit: Int = 50): List<Place>


    /** Enriquecer uma lista de IDs: devolve um Map<placeId, Place> (já cacheado no Room). */
    suspend fun enrichIds(placeIds: Collection<String>): Map<String, Place>


    /**
     * Enriquecimento de uma lista mantendo a ordem original.
     */
    suspend fun enrichPlaces(list: List<Place>): List<Place>
}
