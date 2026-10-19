package com.robertomr.tally.feature.ajustes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Estado puramente visual de Ajustes (AGENTS.md): dialogo de confirmacion de
 * "Eliminar modelo" y texto del campo de token (transitorio; el token guardado
 * nunca vuelve a la UI).
 */
@Stable
class AjustesState {

    var confirmandoEliminar by mutableStateOf(false)
        private set

    var tokenEscrito by mutableStateOf("")
        private set

    /** Aviso transitorio "CSV copiado" de la sección de diagnóstico (solo debug). */
    var csvCopiado by mutableStateOf(false)
        private set

    fun avisarCsvCopiado() {
        csvCopiado = true
    }

    fun ocultarAvisoCsv() {
        csvCopiado = false
    }

    fun pedirConfirmarEliminar() {
        confirmandoEliminar = true
    }

    fun cerrarConfirmarEliminar() {
        confirmandoEliminar = false
    }

    fun onTokenEscrito(valor: String) {
        tokenEscrito = valor
    }

    fun limpiarToken() {
        tokenEscrito = ""
    }
}

@Composable
fun rememberAjustesState(): AjustesState = remember { AjustesState() }
