package com.example.cmu_especial.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cmu_especial.data.entities.UserVisitEntity

@Dao
interface UserVisitDao {

    /**
     * Guarda/atualiza uma visita (normalmente id=UUID, portanto REPLACE só substitui se repetires o mesmo id).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(visit: UserVisitEntity)

    /**
     * Última visita do utilizador a um dado estabelecimento (para a regra dos >= 30 minutos).
     */
    @Query("""
        SELECT * FROM user_visits
        WHERE userId = :userId AND establishmentId = :establishmentId
        ORDER BY visitedAt DESC
        LIMIT 1
    """)
    suspend fun lastVisit(userId: String, establishmentId: String): UserVisitEntity?

    /**
     * Visitas recentes, útil para histórico.
     */
    @Query("""
        SELECT * FROM user_visits
        WHERE userId = :userId
        ORDER BY visitedAt DESC
        LIMIT :limit
    """)
    suspend fun recent(userId: String, limit: Int = 50): List<UserVisitEntity>

    /**
     * Conta visitas num intervalo (para limitar notificações repetidas).
     */
    @Query("""
        SELECT COUNT(*) FROM user_visits
        WHERE userId = :userId
          AND establishmentId = :establishmentId
          AND visitedAt >= :fromEpochMs
    """)
    suspend fun countSince(userId: String, establishmentId: String, fromEpochMs: Long): Int

    /**
     * Limpeza opcional de registos antigos (poupança de espaço).
     */
    @Query("DELETE FROM user_visits WHERE visitedAt < :olderThanEpochMs")
    suspend fun purgeOlderThan(olderThanEpochMs: Long)
}
