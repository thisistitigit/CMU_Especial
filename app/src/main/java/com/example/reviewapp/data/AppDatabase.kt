package com.example.reviewapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.reviewapp.data.dao.PlaceDao
import com.example.reviewapp.data.dao.ReviewDao
import com.example.reviewapp.data.locals.PlaceEntity
import com.example.reviewapp.data.locals.ReviewEntity

/**
 * Base de dados Room da aplicação.
 *
 * Entidades:
 * - [PlaceEntity] (locais)
 * - [ReviewEntity] (reviews)
 *
 * @since 1.0
 */
@Database(entities = [PlaceEntity::class, ReviewEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    /** DAO de locais. */
    abstract fun placeDao(): PlaceDao

    /** DAO de reviews. */
    abstract fun reviewDao(): ReviewDao
}

/**
 * Migração 1→2: adiciona colunas `placeName` e `placeAddress` às reviews
 * para armazenar _snapshots_ de metadata do estabelecimento.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reviews ADD COLUMN placeName TEXT")
        db.execSQL("ALTER TABLE reviews ADD COLUMN placeAddress TEXT")
    }
}
