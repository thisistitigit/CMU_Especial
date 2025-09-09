package com.example.reviewapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.reviewapp.data.locals.PlaceEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO de leitura/escrita de **locais** (places) no cache Room.
 *
 * Índices e ordenações favorecem cenários de:
 * - _bounding-box_ (mapa);
 * - _leaderboards_ por média e contagem de ratings.
 *
 * **Consistência:** o cache é _eventually consistent_ face à origem (Firestore/Places).
 *
 * @since 1.0
 */
@Dao
interface PlaceDao {

    /**
     * Lista todos os locais dentro de uma *bounding box* geográfica.
     *
     * @param minLat latitude mínima (graus).
     * @param maxLat latitude máxima (graus).
     * @param minLng longitude mínima (graus).
     * @param maxLng longitude máxima (graus).
     */
    @Query(
        "SELECT * FROM places WHERE (lat BETWEEN :minLat AND :maxLat) AND (lng BETWEEN :minLng AND :maxLng)"
    )
    suspend fun listInBounds(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): List<PlaceEntity>

    /**
     * Devolve a leaderboard local por média e contagem de avaliações.
     *
     * @param limit número máximo de registos.
     */
    @Query("SELECT * FROM places ORDER BY avgRating DESC, ratingsCount DESC LIMIT :limit")
    suspend fun leaderboard(limit: Int): List<PlaceEntity>

    /** Lista completa (para debug/admin). */
    @Query("SELECT * FROM places")
    suspend fun listAll(): List<PlaceEntity>

    /** Upsert em lote, substitui em conflito por `id`. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PlaceEntity>)

    /** Upsert de um único registo. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PlaceEntity)

    /** Obtém um local por `id` ou `null`. */
    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun get(id: String): PlaceEntity?

    /** *Flow* reativo de todos os locais. */
    @Query("SELECT * FROM places")
    fun flowAll(): Flow<List<PlaceEntity>>

    /** *Flow* reativo de um local por `id`. */
    @Query("SELECT * FROM places WHERE id = :id LIMIT 1")
    fun flowById(id: String): Flow<PlaceEntity?>

    /** Obtém apenas o nome por `id`. Útil para enriquecer reviews. */
    @Query("SELECT name FROM places WHERE id = :id LIMIT 1")
    suspend fun getNameById(id: String): String?

    /**
     * Procura por nome (case-insensitive).
     *
     * @param name nome exato (sem *wildcards*).
     */
    @Query("SELECT * FROM places WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getByName(name: String): PlaceEntity?

    /**
     * Projeção leve para *lists/cards*.
     */
    data class IdNameAddr(
        val id: String,
        val name: String,
        val address: String?
    )

    /**
     * Obtém `id`, `name`, `address` para um conjunto de IDs.
     */
    @Query("SELECT id, name, address FROM places WHERE id IN (:ids)")
    suspend fun getNamesAndAddressesByIds(ids: List<String>): List<IdNameAddr>
}
