package com.robertomr.tally.di

import com.robertomr.tally.domain.expenses.data.ExtractorPorMotor
import com.robertomr.tally.domain.expenses.data.PreferenciaMotorStore
import com.robertomr.tally.domain.expenses.data.RegistroExtracciones
import com.robertomr.tally.domain.expenses.data.TextEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

/**
 * EntryPoint de [TextEngine] para consumidores fuera del patron `@Inject
 * constructor` (spec extraccion-nube): `ExtraccionNubeRealTest` (androidTest)
 * obtiene el motor real del grafo con `EntryPointAccessors.fromApplication`.
 *
 * Debe vivir en el codigo MAIN de `:app` (no en androidTest): los `@EntryPoint`
 * solo se agregan al componente de `TallyApplication` si se procesan en la
 * compilacion principal; en androidTest el casteo falla con `ClassCastException`
 * (verificado 2026-10-16). Alternativa del spec R-NUBE6 a `HiltTestApplication`
 * (que exige `HiltAndroidRule` en todos los tests y rompia NavigationTest/
 * CapturaFlujoTest).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TextEngineEntryPoint {
    /** `TextEngine` sin calificar: el router (spec router-automatico). */
    fun textEngine(): TextEngine

    /** Motor de nube (Gemini) directo, sin router. */
    @Named("nube")
    fun textEngineNube(): TextEngine

    /** Motor local (spec extraccion-local) directo, sin router. */
    @Named("local")
    fun textEngineLocal(): TextEngine

    /** Extraccion explicita por motor (con el decorador de registro; log-extracciones). */
    fun extractorPorMotor(): ExtractorPorMotor

    /** Log de extracciones (log-extracciones): linea base y limpieza de los E2E. */
    fun registroExtracciones(): RegistroExtracciones

    /** Preferencia de motor: los E2E la fijan y la restauran. */
    fun preferenciaMotorStore(): PreferenciaMotorStore
}
