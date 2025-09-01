package com.example.reviewapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.reviewapp.data.locals.PlaceEntity

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places WHERE (lat BETWEEN :minLat AND :maxLat) AND (lng BETWEEN :minLng AND :maxLng)")
    suspend fun listInBounds(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): List<PlaceEntity>

    @Query("SELECT * FROM places ORDER BY avgRating DESC, ratingsCount DESC LIMIT :limit")
    suspend fun leaderboard(limit: Int): List<PlaceEntity>
    @Query("SELECT * FROM places")
    suspend fun listAll(): List<PlaceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAll(items: List<PlaceEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: PlaceEntity)
    @Query("SELECT * FROM places WHERE id = :id") suspend fun get(id: String): PlaceEntity?
}