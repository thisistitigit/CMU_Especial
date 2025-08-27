package com.example.cmu_especial.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.cmu_especial.data.entities.EstablishmentEntity
import com.example.cmu_especial.data.entities.ReviewEntity
import com.example.cmu_especial.data.entities.UserVisitEntity
import com.example.cmu_especial.data.local.dao.EstablishmentDao
import com.example.cmu_especial.data.local.dao.ReviewDao
import com.example.cmu_especial.data.local.dao.UserVisitDao

@Database(
    entities = [
        EstablishmentEntity::class,
        ReviewEntity::class,
        UserVisitEntity::class
    ],
    version = 1, exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun establishmentDao(): EstablishmentDao
    abstract fun reviewDao(): ReviewDao
    abstract fun userVisitDao(): UserVisitDao
}