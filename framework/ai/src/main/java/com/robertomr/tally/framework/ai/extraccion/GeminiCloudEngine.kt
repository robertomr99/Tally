package com.robertomr.tally.framework.ai.extraccion

import com.google.firebase.FirebaseApp
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.APINotConfiguredException
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import com.robertomr.tally.domain.expenses.data.TextEngine
import com.robertomr.tally.domain.expenses.entities.InvalidExtractionException
import com.robertomr.tally.domain.expenses.entities.Motor
import com.robertomr.tally.domain.expenses.entities.TicketExtraido
import com.robertomr.tally.domain.expenses.entities.TicketExtraidoParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementacion de [TextEngine] para la via NUBE (spec extraccion-nube, etapa 2
 * del ADR 0002): Gemini via **Firebase AI Logic** (SDK `firebase-ai`, backend
 * `googleAI`, modelo [TicketExtractionPrompt.MODELO]) con structured output
 * (`responseMimeType = application/json` + `responseSchema`).
 *
 * Seguridad (scenario 3): SIN claves de API en el codigo ni en `BuildConfig`;
 * la config viaja solo en `app/google-services.json` (gitignoreado; lo aporta
 * el humano — prerrequisito U1 del spec) a traves del plugin `google-services`
 * aplicado condicionalmente en `:app`.
 *
 * Politica de errores (scenario 5): todo error (red, cuota, config ausente) se
 * propaga al llamador con mensaje claro, SIN resultados inventados; una
 * [CancellationException] jamas se convierte en resultado. La carga del modelo
 * es lazy y la llamada corre en [Dispatchers.IO], nunca en el hilo principal.
 */
internal class GeminiCloudEngine : TextEngine {

    override val motor: Motor = Motor.NUBE

    /**
     * Modelo Gemini lazy (la creacion resuelve FirebaseApp; sin
     * `app/google-services.json` falla con [MENSAJE_SIN_CONFIG]).
     */
    private val modelo: GenerativeModel by lazy {
        try {
            FirebaseAI.getInstance(FirebaseApp.getInstance(), GenerativeBackend.googleAI()).generativeModel(
                modelName = TicketExtractionPrompt.MODELO,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = TicketExtractionPrompt.responseSchema()
                },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException(MENSAJE_SIN_CONFIG, e)
        }
    }

    override suspend fun extract(textoCrudo: String): TicketExtraido = withContext(Dispatchers.IO) {
        val respuesta = try {
            modelo.generateContent(TicketExtractionPrompt.buildPrompt(textoCrudo))
        } catch (e: CancellationException) {
            throw e
        } catch (e: APINotConfiguredException) {
            throw IllegalStateException(MENSAJE_SIN_CONFIG, e)
        } catch (e: IllegalStateException) {
            // FirebaseApp sin inicializar (falta app/google-services.json).
            throw IllegalStateException(MENSAJE_SIN_CONFIG, e)
        }
        // El parser compartido es el validador del schema (categoria de enum,
        // total con IVA, fechas ISO...); una respuesta malformada ->
        // InvalidExtractionException, nunca un resultado inventado.
        TicketExtraidoParser.parse(respuesta.text.orEmpty(), Motor.NUBE)
    }

    private companion object {
        const val MENSAJE_SIN_CONFIG =
            "Firebase AI no configurado: falta app/google-services.json (prerrequisito U1 " +
                "de docs/specs/extraccion-nube.md) o la API de Gemini no esta habilitada " +
                "en el proyecto Firebase"
    }
}
