package com.example.reviewapp.data.dao

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.reviewapp.data.locals.PlaceEntity
import com.example.reviewapp.data.locals.ReviewEntity
// ReviewDao.kt
@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE placeId = :placeId ORDER BY createdAt DESC LIMIT 10")
    suspend fun latestForPlace(placeId: String): List<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE placeId = :placeId ORDER BY createdAt DESC")
    suspend fun allForPlace(placeId: String): List<ReviewEntity>
    @Query("UPDATE reviews SET photoCloudUrl = :url WHERE id = :reviewId")
    suspend fun updateCloudUrl(reviewId: String, url: String)
    @Query("SELECT * FROM reviews WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun history(userId: String): List<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReviewEntity?

    @Query("SELECT * FROM reviews")
    suspend fun listAll(): List<ReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ReviewEntity>)

    @Query("SELECT * FROM reviews WHERE placeId = :placeId ORDER BY createdAt DESC")
    fun flowForPlace(placeId: String): kotlinx.coroutines.flow.Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews")
    fun flowAll(): kotlinx.coroutines.flow.Flow<List<ReviewEntity>>

    @Query("SELECT createdAt FROM reviews WHERE userId = :userId ORDER BY createdAt DESC LIMIT 1")
    suspend fun lastCreatedAtByUser(userId: String): Long?

    @Query("""
        SELECT * FROM reviews
        WHERE userId = :uid
        ORDER BY createdAt DESC
    """)
    fun flowHistoryForUser(uid: String): kotlinx.coroutines.flow.Flow<List<ReviewEntity>>

}

