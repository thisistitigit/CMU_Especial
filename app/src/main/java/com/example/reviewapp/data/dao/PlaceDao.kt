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

    @Query("SELECT * FROM places")
    fun flowAll(): kotlinx.coroutines.flow.Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places WHERE id = :id LIMIT 1")
    fun flowById(id: String): kotlinx.coroutines.flow.Flow<PlaceEntity?>

    @Query("SELECT name FROM places WHERE id = :id LIMIT 1")
    suspend fun getNameById(id: String): String?

    @Query("SELECT * FROM places WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getByName(name: String): PlaceEntity?

    data class IdNameAddr(
        val id: String,
        val name: String,
        val address: String?
    )

    @Query("SELECT id, name, address FROM places WHERE id IN (:ids)")
    suspend fun getNamesAndAddressesByIds(ids: List<String>): List<IdNameAddr>

}