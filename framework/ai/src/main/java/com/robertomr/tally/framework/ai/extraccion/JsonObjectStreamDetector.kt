package com.robertomr.tally.framework.ai.extraccion

/**
 * Detector puro de "objeto JSON completo" sobre un flujo de texto acumulado
 * (spec extraccion-local, scenario 2): marca el fin cuando se cierra la `}` del
 * objeto de nivel superior, o cuando aparece un marcador de fin de turno
 * (`<end_of_turn>`, `<eos>`). Sirve para cortar la generacion del modelo local,
 * que sin parada degenera hasta `maxTokens` (53 s en el spike, R1).
 *
 * Estado incremental: [accept] solo recorre los caracteres nuevos. Respeta
 * llaves dentro de strings y escapes `\"`. Texto previo al primer `{` se ignora.
 */
internal class JsonObjectStreamDetector {

    private var scanned = 0
    private var depth = 0
    private var inString = false
    private var escaped = false

    /** Indice (exclusivo) del fin del texto util, o -1 si aun no esta completo. */
    var endIndex: Int = -1
        private set

    val isComplete: Boolean get() = endIndex >= 0

    /**
     * Recibe el texto ACUMULADO hasta ahora; devuelve `true` si ya hay un
     * objeto JSON completo (o un marcador de fin) y se puede detener la generacion.
     */
    fun accept(accumulated: String): Boolean {
        if (isComplete) return true
        var i = scanned
        while (i < accumulated.length) {
            val c = accumulated[i]
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
            } else {
                when {
                    c == '"' && depth > 0 -> inString = true
                    c == '{' -> depth++
                    c == '}' && depth > 0 -> {
                        depth--
                        if (depth == 0) {
                            endIndex = i + 1
                            scanned = i + 1
                            return true
                        }
                    }
                    c == '<' -> {
                        val marcador = MARCADORES_FIN.firstOrNull { accumulated.startsWith(it, i) }
                        if (marcador != null) {
                            endIndex = i
                            scanned = i
                            return true
                        }
                        // Prefijo de marcador al final del chunk: reanudar desde aqui.
                        val resto = accumulated.substring(i)
                        if (MARCADORES_FIN.any { it.startsWith(resto) }) {
                            scanned = i
                            return false
                        }
                    }
                }
            }
            i++
        }
        scanned = accumulated.length
        return false
    }

    companion object {
        val MARCADORES_FIN = listOf("<end_of_turn>", "<eos>")

        /**
         * Recorta [texto] al primer objeto JSON completo o antes de un marcador
         * de fin. Si no hay cierre devuelve el texto sin marcadores (el parser
         * compartido lo rechazara si esta truncado).
         */
        fun recortar(texto: String): String {
            val detector = JsonObjectStreamDetector()
            return if (detector.accept(texto)) texto.substring(0, detector.endIndex) else texto
        }
    }
}
