package com.robertomr.tally.framework.ai.di

import com.robertomr.tally.domain.expenses.data.OcrEngine
import com.robertomr.tally.domain.expenses.data.TextEngine
import com.robertomr.tally.framework.ai.extraccion.GeminiCloudEngine
import com.robertomr.tally.framework.ai.ocr.MlKitOcrEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

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
}
