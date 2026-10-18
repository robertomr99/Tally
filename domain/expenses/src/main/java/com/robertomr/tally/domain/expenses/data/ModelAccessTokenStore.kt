package com.robertomr.tally.domain.expenses.data

import kotlinx.coroutines.flow.Flow

/**
 * Token de lectura de Hugging Face introducido por el usuario (el modelo es
 * gated; spec descarga-modelo, D4). Nunca viaja en el APK ni en logs.
 */
interface ModelAccessTokenStore {
    val hayToken: Flow<Boolean>

    fun guardar(token: String)

    fun borrar()
}
