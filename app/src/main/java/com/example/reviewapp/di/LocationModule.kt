package com.example.reviewapp.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de localização: provider do Fused Location.
 *
 * @since 1.0
 */
@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    /** Cliente de localização unificado do Google Play Services. */
    @Provides @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext ctx: Context
    ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(ctx)
}
