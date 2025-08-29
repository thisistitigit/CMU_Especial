package com.example.reviewapp.data.dao

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.reviewapp.data.locals.PlaceEntity
import com.example.reviewapp.data.locals.ReviewEntity
@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE placeId = :placeId ORDER BY createdAt DESC LIMIT 10")
    suspend fun latestForPlace(placeId: String): List<ReviewEntity>

    @Query("SELECT * FROM reviews WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun history(userId: String): List<ReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(item: ReviewEntity)
}

