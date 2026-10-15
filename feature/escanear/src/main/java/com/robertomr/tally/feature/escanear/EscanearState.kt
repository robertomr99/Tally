package com.robertomr.tally.feature.escanear

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Estado puramente visual de la pantalla Escanear (AGENTS.md, capa de
 * presentacion): solo variaciones de copy. El estado del flujo (encuadre /
 * procesando / revision) vive en [EscanearViewModel].
 */
@Stable
class EscanearState {

    /**
     * true si el usuario ya denego el permiso de camara al menos una vez: con
     * ese dato se muestra una ayuda adicional sin volver a lanzar el dialogo
     * del sistema en bucle.
     */
    var permisoDenegadoAntes by mutableStateOf(false)
        private set

    fun onPermisoDenegado() {
        permisoDenegadoAntes = true
    }
}

@Composable
fun rememberEscanearState(): EscanearState = remember { EscanearState() }
