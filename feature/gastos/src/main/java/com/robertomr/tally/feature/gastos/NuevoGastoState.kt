package com.robertomr.tally.feature.gastos

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Estado puramente visual de "Nuevo gasto" (AGENTS.md): scroll y version del
 * formulario. [versionFormulario] sube con "Descartar" para re-crear el
 * formulario (`key(version)`), porque sus campos guardan el texto con
 * `rememberSaveable` y sin recrearlos no volverian al valor inicial.
 */
@Stable
class NuevoGastoState(
    val scroll: ScrollState,
    versionFormulario: Int = 0,
) {
    var versionFormulario by mutableStateOf(versionFormulario)
        private set

    fun recrearFormulario() {
        versionFormulario++
    }
}

@Composable
fun rememberNuevoGastoState(): NuevoGastoState {
    val scroll = rememberScrollState()
    return remember(scroll) { NuevoGastoState(scroll) }
}
