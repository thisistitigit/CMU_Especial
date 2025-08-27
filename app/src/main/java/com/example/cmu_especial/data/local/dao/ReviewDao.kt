package com.example.cmu_especial.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cmu_especial.data.entities.ReviewEntity

// data/local/dao/ReviewDao.kt
@Dao
interface ReviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(review: ReviewEntity)
    @Query("SELECT * FROM reviews WHERE establishmentId=:id ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recentForEstablishment(id: String, limit: Int): List<ReviewEntity>
    @Query("SELECT * FROM reviews WHERE userId=:uid ORDER BY createdAt DESC")
    suspend fun myHistory(uid: String): List<ReviewEntity>
    @Query("SELECT createdAt FROM reviews WHERE userId=:uid AND establishmentId=:eid ORDER BY createdAt DESC LIMIT 1")
    suspend fun lastUserReviewTime(uid: String, eid: String): Long?
}
