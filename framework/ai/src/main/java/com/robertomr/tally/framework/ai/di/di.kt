package com.robertomr.tally.framework.ai.di

import android.content.Context
import com.robertomr.tally.domain.expenses.data.CapacidadDispositivo
import com.robertomr.tally.domain.expenses.data.DownloadConditions
import com.robertomr.tally.domain.expenses.data.EngineRouter
import com.robertomr.tally.domain.expenses.data.ModelAccessTokenStore
import com.robertomr.tally.domain.expenses.data.ModelDownloader
import com.robertomr.tally.domain.expenses.data.OcrEngine
import com.robertomr.tally.domain.expenses.data.PreferenciaMotorSource
import com.robertomr.tally.domain.expenses.data.TextEngine
import com.robertomr.tally.domain.expenses.entities.PreferenciaMotor
import com.robertomr.tally.framework.ai.extraccion.GeminiCloudEngine
import com.robertomr.tally.framework.ai.extraccion.GemmaLocalEngine
import com.robertomr.tally.framework.ai.extraccion.MediaPipeLocalLlm
import com.robertomr.tally.framework.ai.modelo.AndroidDownloadConditions
import com.robertomr.tally.framework.ai.modelo.FileModelAccessTokenStore
import com.robertomr.tally.framework.ai.modelo.WorkManagerModelDownloader
import com.robertomr.tally.framework.ai.ocr.MlKitOcrEngine
import com.robertomr.tally.framework.ai.router.RamCapacidadDispositivo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AiModule {

    /** OCR on-device (ML Kit latin bundled); etapa 1 del ADR 0002. */
    @Provides
    fun provideOcrEngine(): OcrEngine = MlKitOcrEngine()

    /**
     * Extraccion estructurada en nube (Gemini via Firebase AI Logic); etapa 2
     * del ADR 0002. Calificado `@Named("nube")`: el `TextEngine` sin calificar
     * es el [EngineRouter] (spec router-automatico).
     */
    @Provides
    @Named("nube")
    fun provideCloudTextEngine(): TextEngine = GeminiCloudEngine()

    /** Extraccion local (Gemma 3n via MediaPipe, spec extraccion-local). */
    @Provides
    @Singleton
    @Named("local")
    fun provideLocalTextEngine(
        @ApplicationContext context: Context,
        modelDownloader: ModelDownloader,
    ): TextEngine = GemmaLocalEngine(modelDownloader, MediaPipeLocalLlm(context))

    /** Capacidad del dispositivo por RAM total (spec router-automatico, D3). */
    @Provides
    @Singleton
    fun provideCapacidadDispositivo(
        @ApplicationContext context: Context,
    ): CapacidadDispositivo = RamCapacidadDispositivo(context)

    /** Preferencia fija AUTO: unico punto que sustituira `ajustes-motor` (D6). */
    @Provides
    @Singleton
    fun providePreferenciaMotorSource(): PreferenciaMotorSource =
        PreferenciaMotorSource { PreferenciaMotor.AUTO }

    /** `TextEngine` sin calificar = router Local/Nube (ADR 0001, spec router-automatico). */
    @Provides
    @Singleton
    fun provideTextEngine(
        @Named("local") local: TextEngine,
        @Named("nube") nube: TextEngine,
        capacidad: CapacidadDispositivo,
        modelDownloader: ModelDownloader,
        preferencia: PreferenciaMotorSource,
    ): TextEngine = EngineRouter(local, nube, capacidad, modelDownloader, preferencia)

    /** Politica WiFi y espacio libre para la descarga del modelo (spec descarga-modelo). */
    @Provides
    @Singleton
    fun provideDownloadConditions(
        @ApplicationContext context: Context,
    ): DownloadConditions = AndroidDownloadConditions(context)

    /** Token de Hugging Face del usuario en `noBackupFilesDir` (spec descarga-modelo, D4). */
    @Provides
    @Singleton
    fun provideModelAccessTokenStore(
        @ApplicationContext context: Context,
    ): ModelAccessTokenStore = FileModelAccessTokenStore(context)

    /** Descarga del modelo local con WorkManager (spec descarga-modelo). */
    @Provides
    @Singleton
    fun provideModelDownloader(
        @ApplicationContext context: Context,
        conditions: DownloadConditions,
    ): ModelDownloader = WorkManagerModelDownloader(context, conditions)
}
