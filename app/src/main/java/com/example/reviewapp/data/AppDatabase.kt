package com.example.reviewapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.reviewapp.data.dao.PlaceDao
import com.example.reviewapp.data.dao.ReviewDao
import com.example.reviewapp.data.locals.PlaceEntity
import com.example.reviewapp.data.locals.ReviewEntity

@Database(entities = [PlaceEntity::class, ReviewEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
    abstract fun reviewDao(): ReviewDao
}