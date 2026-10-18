package com.robertomr.tally.domain.expenses.entities

/**
 * Preferencia de motor de extraccion (ADR 0001). [AUTO] deja decidir al router;
 * [LOCAL] y [NUBE] fuerzan el motor. Hasta `ajustes-motor` el valor es fijo [AUTO].
 */
enum class PreferenciaMotor {
    AUTO,
    LOCAL,
    NUBE,
}
