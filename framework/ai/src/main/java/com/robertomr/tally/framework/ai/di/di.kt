package com.robertomr.tally.framework.ai.di

import android.content.Context
import com.robertomr.tally.domain.expenses.data.DownloadConditions
import com.robertomr.tally.domain.expenses.data.ModelAccessTokenStore
import com.robertomr.tally.domain.expenses.data.ModelDownloader
import com.robertomr.tally.domain.expenses.data.OcrEngine
import com.robertomr.tally.domain.expenses.data.TextEngine
import com.robertomr.tally.framework.ai.extraccion.GeminiCloudEngine
import com.robertomr.tally.framework.ai.extraccion.GemmaLocalEngine
import com.robertomr.tally.framework.ai.extraccion.MediaPipeLocalLlm
import com.robertomr.tally.framework.ai.modelo.AndroidDownloadConditions
import com.robertomr.tally.framework.ai.modelo.FileModelAccessTokenStore
import com.robertomr.tally.framework.ai.modelo.WorkManagerModelDownloader
import com.robertomr.tally.framework.ai.ocr.MlKitOcrEngine
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
     * del ADR 0002 (spec extraccion-nube). `extraccion-local` y el futuro
     * `EngineRouter` (ADR 0001) entraran aqui sin cambiar el contrato.
     */
    @Provides
    fun provideTextEngine(): TextEngine = GeminiCloudEngine()

    /**
     * Extraccion local (Gemma 3n via MediaPipe, spec extraccion-local). Calificado
     * `@Named("local")`: el `TextEngine` sin calificar sigue siendo la nube hasta
     * `router-automatico` (D1).
     */
    @Provides
    @Singleton
    @Named("local")
    fun provideLocalTextEngine(
        @ApplicationContext context: Context,
        modelDownloader: ModelDownloader,
    ): TextEngine = GemmaLocalEngine(modelDownloader, MediaPipeLocalLlm(context))

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
