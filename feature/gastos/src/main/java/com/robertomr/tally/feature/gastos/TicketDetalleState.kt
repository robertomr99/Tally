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
 * Estado puramente visual del detalle (AGENTS.md): scroll, dialogo de
 * confirmacion de borrado, visor de la foto y la version del formulario.
 *
 * [versionFormulario] sube con "Descartar" para re-crear el formulario
 * (`key(version)`): sus campos guardan el texto con `rememberSaveable` y sin
 * recrearlos no volverian al valor guardado.
 */
@Stable
class TicketDetalleState(
    val scroll: ScrollState,
    mostrarConfirmarBorrado: Boolean = false,
    mostrarFoto: Boolean = false,
    versionFormulario: Int = 0,
) {
    var mostrarConfirmarBorrado by mutableStateOf(mostrarConfirmarBorrado)
        private set
    var mostrarFoto by mutableStateOf(mostrarFoto)
        private set
    var versionFormulario by mutableStateOf(versionFormulario)
        private set

    fun abrirConfirmarBorrado() {
        mostrarConfirmarBorrado = true
    }

    fun cerrarConfirmarBorrado() {
        mostrarConfirmarBorrado = false
    }

    fun abrirFoto() {
        mostrarFoto = true
    }

    fun cerrarFoto() {
        mostrarFoto = false
    }

    fun recrearFormulario() {
        versionFormulario++
    }
}

@Composable
fun rememberTicketDetalleState(): TicketDetalleState {
    val scroll = rememberScrollState()
    return remember(scroll) { TicketDetalleState(scroll) }
}
