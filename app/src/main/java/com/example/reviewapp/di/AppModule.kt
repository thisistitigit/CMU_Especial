package com.example.reviewapp.di

import android.content.Context
import androidx.room.Room
import com.example.reviewapp.BuildConfig
import com.example.reviewapp.data.AppDatabase
import com.example.reviewapp.data.MIGRATION_1_2
import com.example.reviewapp.data.dao.PlaceDao
import com.example.reviewapp.data.dao.ReviewDao
import com.example.reviewapp.data.repository.PlaceRepository
import com.example.reviewapp.data.repository.PlaceRepositoryImpl
import com.example.reviewapp.data.repository.ReviewRepository
import com.example.reviewapp.data.repository.ReviewRepositoryImpl
import com.example.reviewapp.network.api.GooglePlacesApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Módulo Hilt com _singletons_ de infraestrutura, DAOs e repositórios.
 *
 * @since 1.0
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** Base de dados Room (inclui migração 1→2). */
    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "reviews.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun providePlaceDao(db: AppDatabase): PlaceDao = db.placeDao()
    @Provides fun provideReviewDao(db: AppDatabase): ReviewDao = db.reviewDao()

    @Provides @Singleton fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    @Provides @Singleton fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
    @Provides @Singleton fun provideStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    /** Lê a chave da Google Places API de `strings.xml`. */
    @Provides @Singleton
    @Named("GOOGLE_PLACES_KEY")
    fun provideGooglePlacesKey(): String = BuildConfig.PLACES_API_KEY

    /** Repositório de locais. */
    @Provides @Singleton
    fun providePlaceRepository(
        placeDao: PlaceDao,
        api: GooglePlacesApi,
        @Named("GOOGLE_PLACES_KEY") googleKey: String,
        firestore: FirebaseFirestore
    ): PlaceRepository = PlaceRepositoryImpl(placeDao, api, googleKey, firestore)

    /** Repositório de reviews. */
    @Provides @Singleton
    fun provideReviewRepository(
        reviewDao: ReviewDao,
        placeDao: PlaceDao,
        firestore: FirebaseFirestore?,
        storage: FirebaseStorage?,
        auth: FirebaseAuth,
        @ApplicationContext context: Context
    ): ReviewRepository = ReviewRepositoryImpl(
        reviewDao = reviewDao,
        placeDao = placeDao,
        firestore = firestore,
        storage = storage,
        auth = auth,
        appContext = context
    )
}
