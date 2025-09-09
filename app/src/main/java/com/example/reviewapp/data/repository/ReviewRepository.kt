package com.example.reviewapp.data.repository

import com.example.reviewapp.data.models.Place
import com.example.reviewapp.data.models.Review
import kotlinx.coroutines.flow.Flow

/**
 * Contrato para gestão do ciclo de vida de reviews.
 *
 * Abrange:
 * - criação com políticas anti-abuso (distância/tempo);
 * - leitura paginada/streaming;
 * - sincronização cache ↔ cloud (Firestore/Storage);
 * - _rate limiting_ (global e por local).
 *
 * @since 1.0
 */
interface ReviewRepository {

    /**
     * Adiciona uma review aplicando validações de proximidade e frequência.
     *
     * @param review payload base (userId pode ser normalizado internamente).
     * @param userLat latitude do utilizador no momento.
     * @param userLng longitude do utilizador no momento.
     * @param now epoch millis do momento de submissão (por omissão `System.currentTimeMillis()`).
     * @throws IllegalStateException se não existir sessão ativa.
     * @throws ReviewRepositoryImpl.ReviewDeniedException se violar regras.
     */
    suspend fun addReview(
        review: Review,
        userLat: Double,
        userLng: Double,
        now: Long = System.currentTimeMillis()
    )

    /** Últimas reviews de um local (limit 10 na origem do DAO). */
    suspend fun latestReviews(placeId: String): List<Review>

    /** Histórico do utilizador. */
    suspend fun history(userId: String): List<Review>

    /** Verifica se o utilizador **pode** avaliar este local agora. */
    suspend fun canUserReviewHere(
        userId: String, place: Place, now: Long, userLat: Double, userLng: Double
    ): Boolean

    /** Sincroniza e devolve reviews do local a partir do backend. */
    suspend fun refreshPlaceReviews(placeId: String): List<Review>

    /** Sincroniza e devolve reviews do utilizador a partir do backend. */
    suspend fun refreshUserReviews(uid: String): List<Review>

    /** Obtém uma review por id. */
    suspend fun getReview(id: String): Review?

    /** Todas as reviews do local (ordenadas por data desc). */
    suspend fun allReviews(placeId: String): List<Review>

    /** Último timestamp de review do utilizador (para _rate limiting_ global). */
    suspend fun lastReviewAtByUser(userId: String): Long?

    /** UID atual (ou `null`). */
    fun currentUid(): String?

    /**
     * Sincroniza um *snapshot* de todas as reviews (paginação defensiva).
     *
     * @return número de reviews lidas.
     */
    suspend fun refreshAllReviews(maxToFetch: Int = 2000, pageSize: Int = 500): Int

    /** Stream reativo de reviews de um local (cache + opcional remoto). */
    fun streamPlaceReviews(placeId: String): Flow<List<Review>>

    /** Stream reativo global de reviews. */
    fun streamAllReviews(): Flow<List<Review>>

    /** Stream do *meta* mínimo do local inferido da review mais recente. */
    fun streamUserHistory(uid: String): Flow<List<Review>>
    fun streamPlaceMetaFromReviews(placeId: String): Flow<Place?>
}
