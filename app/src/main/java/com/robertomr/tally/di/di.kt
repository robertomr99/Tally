package com.robertomr.tally.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Módulo Hilt de `:app` (spec periodo-selector, D4): provee el [Clock] del
 * sistema que consume `ObtenerPeriodo`. Los tests pasan `Clock.fixed(...)`.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
