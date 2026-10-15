package com.robertomr.tally.domain.expenses.entities

/**
 * Motor que extrajo los datos del ticket (CONTEXT.md): Local (Gemma 3n con
 * MediaPipe, en el dispositivo) o Nube (Gemini vía Firebase AI Logic). Se
 * registra con cada ticket y se muestra siempre en la UI.
 */
enum class Motor {
    LOCAL,
    NUBE,
}
