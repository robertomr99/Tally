package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.RegistroExtracciones
import javax.inject.Inject

/** Vacía el log de extracciones ("Borrar registro" en la sección de diagnóstico debug). */
class BorrarRegistroExtracciones @Inject constructor(
    private val registro: RegistroExtracciones,
) {
    suspend operator fun invoke() = registro.borrarTodo()
}
