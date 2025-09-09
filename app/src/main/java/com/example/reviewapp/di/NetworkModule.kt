package com.example.reviewapp.di

import com.example.reviewapp.BuildConfig
import com.example.reviewapp.network.api.GooglePlacesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Módulo de rede: OkHttp + Retrofit configurados para a Google Places API.
 *
 * - `HttpLoggingInterceptor`: `BODY` em `debug`, `BASIC` em `release`.
 * - *Timeouts* simétricos de 20s.
 * - `GsonConverterFactory` para (de)serialização JSON.
 *
 * @since 1.0
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** Cliente OkHttp com _logging_ condicional e timeouts. */
    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /** Retrofit apontado para a base URL do Places API. */
    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/place/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    /** Interface da Google Places API. */
    @Provides @Singleton
    fun provideGooglePlacesApi(retrofit: Retrofit): GooglePlacesApi =
        retrofit.create(GooglePlacesApi::class.java)
}
