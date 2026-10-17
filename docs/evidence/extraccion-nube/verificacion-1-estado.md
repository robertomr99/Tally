# Extracción en nube — verificación 1 (extracción real): COMPLETADA 2026-10-17

Fuente: spec `docs/specs/extraccion-nube.md`, T7 / verificación 1 / Scenario 1-2.
Sesión 018 de `PROGRESS.md`. Estado: **completada** (run real 5/5 sin skips +
contraste humano de los 4 totales registrado).

## Qué se ejecutó

Los 4 textos crudos reales (`docs/evidence/ocr-mlkit/texto_crudo_1..4.txt`,
copiados como assets del test) contra `GeminiCloudEngine` (Firebase AI Logic,
backend `googleAI`, modelo **`gemini-3.8-flash`**, `responseSchema`) con
`ExtraccionNubeRealTest`:

- Emulador `emulator-5554` (`Medium_Phone_API_36.1`, API 36, imagen CON GMS)
  — dispositivo de referencia del run final.
- Tab S9+ física (`SM-X810`, ADB inalámbrico) — usada en los runs previos de
  diagnóstico (App Check / modelo deprecado).

Comandos (Windows, sin `bash`):

```
$env:ANDROID_SERIAL = "emulator-5554"; .\gradlew.bat :app:connectedDebugAndroidTest `
  "-Pandroid.testInstrumentationRunnerArguments.class=com.robertomr.tally.extraccion.ExtraccionNubeRealTest"
$env:ANDROID_SERIAL = "emulator-5554"; .\gradlew.bat :app:connectedDebugAndroidTest
```

## Resultado real (SIN inventar)

JUnit XML crudo en este directorio:

- `junit-app-emulador-5554-run-final.xml` — `ExtraccionNubeRealTest`:
  **tests=5 failures=0 errors=0 skipped=0** (2026-10-17 14:08, `emulator-5554`).
  Métodos: `theInjectedEngineIsTheCloudOne` (motor == NUBE),
  `ticket1ExtractionIsValidWithTotal3530`,
  `ticket2ExtractionUsesTheTotalWithVatNotTheBase`,
  `ticket3ExtractionIsValidWithTotal1390`,
  `ticket4ExtractionIsValidWithTotal791`.
- `junit-app-emulador-5554-suite-completa.xml` — suite completa del mismo
  dispositivo (2026-10-17 14:10): **tests=11 failures=0 errors=0 skipped=0**
  = `CapturaFlujoTest` 3/3 + `NavigationTest` 3/3 + `ExtraccionNubeRealTest` 5/5
  (regresión E2E en verde con la extracción real dentro).

Extracciones reales exportadas: `extraccion_1.txt` .. `extraccion_4.txt`
(contenido EXACTO entre `<<<` y `>>>` del logcat con tag `ExtraccionNube`, sin
reformular; el formato es el `toString` de `TicketExtraido`, por eso `.txt` y no
`.json`).

## Tabla de contraste humano vs papel (R3, decisión del usuario 2026-10-17)

El papel manda sobre el test (U4 del spec). Los totales reales los fija el
usuario mirando los tickets físicos; las expectativas del test se corrigieron a
coherencia con el papel, no al revés.

| Ticket | Comercio | Total extraído por el modelo | Total real del papel | Veredicto |
| --- | --- | --- | --- | --- |
| 1 | HOLA ALMIRANTE, S.L. | 35,30 | **35,30** | CORRECTO. La expectativa del test (32,09) estaba mal derivada del OCR: la foto trae DOS bloques y el texto crudo ambos importes ("Total (impuestos incl.) 32,09" y "Total: 35,30"); el total CON IVA real es 35,30 (corrección 32,09 → 35,30). |
| 2 | SANTOMÁS | 3,50 | **3,50** | CORRECTO. Caso trampa de R3: base 3,36 + cuota 0,14; el modelo elige el total CON IVA (3,50), nunca la base (3,36). |
| 3 | FOOD COURT OZONE S.L. | 13,90 | **13,90** | CORRECTO. El "Total (impuestos incl.) 12,64" del OCR era engañoso; el papel manda: 50,00 entregado − 36,10 de cambio = 13,90 (corrección de expectativa: de "solo registrado" a assert 13,90). |
| 4 | GRUPO DIA | 7,91 | **7,91** | CORRECTO. |

Nota metodológica: los asserts comparan con tolerancia 0,01; los valores
extraídos por el modelo en el run final fueron 35.3 / 3.5 / 13.9 / 7.91. El
modelo devolvió 13,90 en el ticket 3 sin endurecer el prompt (el importe 13,90
aparece en el texto crudo y es consistente con Entregado−Cambio), por lo que no
se tocó `TicketExtractionPrompt.kt` (su test sigue 6/6).

## Cronología de la sesión 018 (por qué este fichero estaba "bloqueado")

1. Run real inicial (Tab S9+ y emulador): `ServerException: Firebase App Check
   token is invalid` en los 4 tickets (Firebase AI Logic exige App Check
   enforced). Evidencia histórica: `junit-app-tab-sm-x810.xml`,
   `junit-app-emulador-5554.xml`.
2. El usuario registró el debug token fijo (`app/appcheck-debug-token.txt`,
   UUID `983ba4d7-…`, LOCAL y gitignoreado) en la consola de Firebase
   (Security > App Check > Apps > Manage debug tokens) → App Check válido.
3. Con App Check válido, el modelo original `gemini-2.5-flash` devolvió
   `ServerException: This model models/gemini-2.5-flash is no longer available
   to new users … use models/gemini-3.8-flash` (proyecto nuevo `tally-586d1`):
   evidencia en `junit-app-emulador-5554-run-modelo-deprecado.xml`. Contingencia
   ya prevista: `TicketExtractionPrompt.MODELO = "gemini-3.8-flash"` (U3
   resuelta con el modelo que indicó el servidor).
4. Run con App Check válido y modelo nuevo: 4/5 — solo fallaba
   `ticket1ExtractionIsValidWithTotal3209` (`expected:<32.09> but was:<35.3>`),
   es decir, el modelo devolvía el total real del papel y la expectativa del
   test era la errónea.
5. **Contraste humano con el papel (2026-10-17, R3/U4)**: el usuario fija los
   totales reales — ticket 1 = 35,30 (el "otro Total"; 32,09 era el bloque
   equivocado de los dos que trae la foto) y ticket 3 = 13,90 (50,00−36,10 de
   cambio; el "Total (impuestos incl.) 12,64" del OCR era engañoso). El papel
   manda sobre el test: se corrigen las expectativas
   (`ExtraccionNubeRealTest.kt`) y se actualiza el KDoc con la decisión y su fecha.
6. Run final → **5/5 failures=0 skipped=0** + exportación de las 4 extracciones
   + regresión E2E 11/11 en la misma suite. Verificación 1 cumplida.

## Cierre de T7

- Verificación 1 cumplida: schema+enum en los 4 y totales según contraste humano
  (35,30 / 3,50 / 13,90 / 7,91 con tolerancia 0,01).
- Contraste humano de los 4 totales vs papel registrado (tabla de arriba, R3).
- Extracciones reales exportadas a `extraccion_1..4.txt` (logcat tag
  `ExtraccionNube`).
