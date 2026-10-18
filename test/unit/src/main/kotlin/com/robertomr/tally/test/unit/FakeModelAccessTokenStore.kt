package com.robertomr.tally.test.unit

import com.robertomr.tally.domain.expenses.data.ModelAccessTokenStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Doble de [ModelAccessTokenStore] (spec descarga-modelo). */
class FakeModelAccessTokenStore(initial: String? = null) : ModelAccessTokenStore {
    var token: String? = initial
        private set

    private val flow = MutableStateFlow(initial != null)
    override val hayToken: Flow<Boolean> = flow

    override fun guardar(token: String) {
        this.token = token
        flow.value = true
    }

    override fun borrar() {
        token = null
        flow.value = false
    }
}
