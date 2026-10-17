package com.robertomr.tally.di

import com.robertomr.tally.domain.expenses.data.TextEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

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
    fun textEngine(): TextEngine
}
