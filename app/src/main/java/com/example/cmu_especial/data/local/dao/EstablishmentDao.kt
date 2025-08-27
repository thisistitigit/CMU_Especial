package com.example.cmu_especial.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cmu_especial.data.entities.EstablishmentEntity

@Dao
interface EstablishmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(vararg items: EstablishmentEntity)

    @Query("SELECT * FROM establishments ORDER BY avgRating DESC LIMIT :limit")
    suspend fun topRated(limit: Int): List<EstablishmentEntity>

    @Query("SELECT * FROM establishments WHERE id=:id")
    suspend fun getById(id: String): EstablishmentEntity
}