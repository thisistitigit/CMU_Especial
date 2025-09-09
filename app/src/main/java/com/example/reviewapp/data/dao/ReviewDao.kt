package com.example.reviewapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.reviewapp.data.locals.ReviewEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestão de **reviews**.
 *
 * Abrange consultas para:
 * - últimos comentários por estabelecimento;
 * - histórico por utilizador;
 * - filas de _upload_ de fotos (offline-first);
 * - _flows_ reativos por local e globais.
 *
 * @since 1.0
 */
@Dao
interface ReviewDao {

    /** Últimas 10 reviews do estabelecimento (mais recentes primeiro). */
    @Query("SELECT * FROM reviews WHERE placeId = :placeId ORDER BY createdAt DESC LIMIT 10")
    suspend fun latestForPlace(placeId: String): List<ReviewEntity>

    /** Todas as reviews do estabelecimento (ordenadas por data desc). */
    @Query("SELECT * FROM reviews WHERE placeId = :placeId ORDER BY createdAt DESC")
    suspend fun allForPlace(placeId: String): List<ReviewEntity>

    /** Atualiza a URL na cloud após *upload* assíncrono. */
    @Query("UPDATE reviews SET photoCloudUrl = :url WHERE id = :reviewId")
    suspend fun updateCloudUrl(reviewId: String, url: String)

    /** Histórico completo do utilizador (mais recentes primeiro). */
    @Query("SELECT * FROM reviews WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun history(userId: String): List<ReviewEntity>

    /** Lê uma review por `id`. */
    @Query("SELECT * FROM reviews WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReviewEntity?

    /** Lista completa (debug/admin). */
    @Query("SELECT * FROM reviews")
    suspend fun listAll(): List<ReviewEntity>

    /** Upsert de uma review. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ReviewEntity)

    /** Upsert em lote. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ReviewEntity>)

    /** *Flow* reativo das reviews de um local. */
    @Query("SELECT * FROM reviews WHERE placeId = :placeId ORDER BY createdAt DESC")
    fun flowForPlace(placeId: String): Flow<List<ReviewEntity>>

    /** *Flow* reativo global. */
    @Query("SELECT * FROM reviews")
    fun flowAll(): Flow<List<ReviewEntity>>

    /** Timestamp da última review do utilizador (para *rate limiting* global). */
    @Query("SELECT createdAt FROM reviews WHERE userId = :userId ORDER BY createdAt DESC LIMIT 1")
    suspend fun lastCreatedAtByUser(userId: String): Long?

    /** *Flow* do histórico do utilizador (para UI reativa). */
    @Query(
        """
        SELECT * FROM reviews
        WHERE userId = :uid
        ORDER BY createdAt DESC
        """
    )
    fun flowHistoryForUser(uid: String): Flow<List<ReviewEntity>>

    /**
     * Reviews com foto **local** mas sem URL **cloud** — usadas por um
     * _uploader_ em background para sincronização.
     *
     * @param limit máximo de registos a devolver (FIFO).
     */
    @Query(
        """
        SELECT * FROM reviews
        WHERE photoLocalPath IS NOT NULL
          AND (photoCloudUrl IS NULL OR photoCloudUrl = '')
        ORDER BY createdAt ASC
        LIMIT :limit
        """
    )
    suspend fun pendingPhotoUpload(limit: Int = 50): List<ReviewEntity>

    /**
     * Timestamp da última review do utilizador **neste** estabelecimento
     * (para *rate limiting* contextual: 30 minutos no mesmo local).
     */
    @Query(
        "SELECT createdAt FROM reviews WHERE userId=:uid AND placeId=:placeId ORDER BY createdAt DESC LIMIT 1"
    )
    suspend fun lastCreatedAtByUserAtPlace(uid: String, placeId: String): Long?
}
