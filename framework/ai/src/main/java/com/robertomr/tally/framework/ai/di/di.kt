package com.robertomr.tally.framework.ai.di

import android.content.Context
import com.robertomr.tally.domain.expenses.data.CapacidadDispositivo
import com.robertomr.tally.domain.expenses.data.DownloadConditions
import com.robertomr.tally.domain.expenses.data.EngineConRegistro
import com.robertomr.tally.domain.expenses.data.EngineRouter
import com.robertomr.tally.domain.expenses.data.ExtractorPorMotor
import com.robertomr.tally.domain.expenses.data.ModelAccessTokenStore
import com.robertomr.tally.domain.expenses.data.ModelDownloader
import com.robertomr.tally.domain.expenses.data.OcrEngine
import com.robertomr.tally.domain.expenses.data.PreferenciaMotorSource
import com.robertomr.tally.domain.expenses.data.PreferenciaMotorStore
import com.robertomr.tally.domain.expenses.data.RegistroExtracciones
import com.robertomr.tally.domain.expenses.data.Reloj
import com.robertomr.tally.domain.expenses.data.TextEngine
import com.robertomr.tally.framework.ai.extraccion.GeminiCloudEngine
import com.robertomr.tally.framework.ai.extraccion.GemmaLocalEngine
import com.robertomr.tally.framework.ai.extraccion.MediaPipeLocalLlm
import com.robertomr.tally.framework.ai.modelo.AndroidDownloadConditions
import com.robertomr.tally.framework.ai.modelo.FileModelAccessTokenStore
import com.robertomr.tally.framework.ai.modelo.WorkManagerModelDownloader
import com.robertomr.tally.framework.ai.ocr.MlKitOcrEngine
import com.robertomr.tally.framework.ai.preferencias.SharedPreferencesPreferenciaMotor
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

    /** Preferencia de motor persistida en SharedPreferences (spec ajustes-motor, D3). */
    @Provides
    @Singleton
    fun providePreferenciaMotorStore(
        @ApplicationContext context: Context,
    ): PreferenciaMotorStore = SharedPreferencesPreferenciaMotor(
        context.getSharedPreferences(SharedPreferencesPreferenciaMotor.FICHERO, Context.MODE_PRIVATE),
    )

    /** Misma instancia que el almacen: es lo que lee el router en cada extraccion. */
    @Provides
    @Singleton
    fun providePreferenciaMotorSource(store: PreferenciaMotorStore): PreferenciaMotorSource = store

    /**
     * Router Local/Nube (ADR 0001, spec router-automatico): una unica
     * instancia expuesta como [TextEngine] sin calificar y como
     * [ExtractorPorMotor] (spec reintento-otro-motor).
     */
    @Provides
    @Singleton
    fun provideEngineRouter(
        @Named("local") local: TextEngine,
        @Named("nube") nube: TextEngine,
        capacidad: CapacidadDispositivo,
        modelDownloader: ModelDownloader,
        preferencia: PreferenciaMotorSource,
    ): EngineRouter = EngineRouter(local, nube, capacidad, modelDownloader, preferencia)

    /**
     * Decorador de registro (spec log-extracciones, D2): UNA instancia que
     * envuelve al router y se expone como [TextEngine] y como [ExtractorPorMotor],
     * de modo que ambas rutas (router y reintento) quedan registradas en un solo sitio.
     */
    @Provides
    @Singleton
    fun provideEngineConRegistro(
        router: EngineRouter,
        registro: RegistroExtracciones,
        reloj: Reloj,
    ): EngineConRegistro = EngineConRegistro(router, router, registro, reloj)

    /** `TextEngine` sin calificar = router Local/Nube con registro de extracciones. */
    @Provides
    @Singleton
    fun provideTextEngine(engine: EngineConRegistro): TextEngine = engine

    /** Extraccion explicita por motor para el reintento con el otro motor (con registro). */
    @Provides
    @Singleton
    fun provideExtractorPorMotor(engine: EngineConRegistro): ExtractorPorMotor = engine

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
