package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.ModelAccessTokenStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Indica si hay un token de acceso guardado (nunca expone su valor). */
class ObservarTokenModelo @Inject constructor(
    private val tokenStore: ModelAccessTokenStore,
) {
    operator fun invoke(): Flow<Boolean> = tokenStore.hayToken
}
