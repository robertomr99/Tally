package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.ModelAccessTokenStore
import javax.inject.Inject

/** Guarda (o borra, si esta vacio) el token de Hugging Face del usuario. */
class ConfigurarTokenModelo @Inject constructor(
    private val tokenStore: ModelAccessTokenStore,
) {
    operator fun invoke(token: String) {
        val limpio = token.trim()
        if (limpio.isEmpty()) tokenStore.borrar() else tokenStore.guardar(limpio)
    }
}
