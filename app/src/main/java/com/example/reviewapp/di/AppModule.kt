    // di/AppModule.kt
    package com.example.reviewapp.di

    import android.content.Context
    import androidx.room.Room
    import com.example.reviewapp.R
    import com.example.reviewapp.data.AppDatabase
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

    @Module
    @InstallIn(SingletonComponent::class)
    object AppModule {

        // Room
        @Provides @Singleton
        fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
            Room.databaseBuilder(ctx, AppDatabase::class.java, "reviews.db").build()

        @Provides fun providePlaceDao(db: AppDatabase): PlaceDao = db.placeDao()
        @Provides fun provideReviewDao(db: AppDatabase): ReviewDao = db.reviewDao()

        // Firebase
        @Provides @Singleton fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
        @Provides @Singleton fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
        @Provides @Singleton fun provideStorage(): FirebaseStorage = FirebaseStorage.getInstance()

        // 🔑 Google Places Web Service key (do secrets.xml)
        @Provides @Singleton
        @Named("GOOGLE_PLACES_KEY")
        fun provideGooglePlacesKey(@ApplicationContext ctx: Context): String =
            ctx.getString(R.string.google_places_key)

        // Repositório
        @Provides @Singleton
        fun providePlaceRepository(
            placeDao: PlaceDao,
            api: GooglePlacesApi,
            @Named("GOOGLE_PLACES_KEY") googleKey: String,
            firestore: FirebaseFirestore
        ): PlaceRepository = PlaceRepositoryImpl(placeDao, api, googleKey, firestore)

        @Provides @Singleton
        fun provideReviewRepository(
            reviewDao: ReviewDao,
            placeDao: PlaceDao,
            firestore: FirebaseFirestore,
            storage: FirebaseStorage
        ): ReviewRepository =
            ReviewRepositoryImpl(reviewDao, placeDao, firestore, storage)
    }
