package com.robertomr.tally.framework.ai.di

import com.robertomr.tally.domain.expenses.data.OcrEngine
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
}
