package com.example.reviewapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.reviewapp.data.dao.PlaceDao
import com.example.reviewapp.data.dao.ReviewDao
import com.example.reviewapp.data.locals.PlaceEntity
import com.example.reviewapp.data.locals.ReviewEntity

@Database(entities = [PlaceEntity::class, ReviewEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
    abstract fun reviewDao(): ReviewDao
    }
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE reviews ADD COLUMN placeName TEXT")
            db.execSQL("ALTER TABLE reviews ADD COLUMN placeAddress TEXT")
        }
}