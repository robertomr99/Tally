# extraccion-local: latencias y calidad

Estado: **gate fisico en Tab S9+ EJECUTADO (2026-10-18): 4/4 extracciones validas con `motor=LOCAL`, generacion 25-39 s por ticket (< 60 s)**; totales 2/4 correctos.

## Humo en `emulator-5554` (NO normativo)

Dispositivo: AVD `Medium_Phone_API_36.1`, x86_64, Android 16 (API 36), arrancado con
`-memory 5120` (con los 2,5 GB por defecto el lowmemorykiller mata la app durante la carga).
Modelo `gemma-3n-E2B-it-int4.litertlm` (3.655.827.456 bytes), `tasks-genai` 0.10.35,
`maxTokens=2048`, `topK=1`, `temperature=0.2`. Log crudo: `emulador_humo_logcat.txt`.

| Ticket | loadMillis | generationMillis | promptTokens | responseTokens | ranOnMainThread | total extraido | total papel | acierto |
|---|---|---|---|---|---|---|---|---|
| 1 | 607 | 17.287 | 850 | 225 | false | 32,09 | 35,30 | no (bloque equivocado) |
| 2 | 480 | 25.973 | 893 | 426 | false | 3,50 | 3,50 | si (no cae en la base 3,36) |
| 3 | 263 | 10.799 | 740 | 152 | false | 36,10 | 13,90 | no (cogio el cambio) |
| 4 | 252 | 15.516 | 1.009 | 239 | false | 7,91 | 7,91 | si |

4/4 pasan el parser compartido con `motor = LOCAL`; 2/4 totales correctos. La primera
llamada (ticket 2 en solitario) tardo load 1.676 ms + generacion 31.498 ms. La parada por
detector funciona (la respuesta termina en la `}`; spike sin parada: 53 s). Comparar con la
nube en `docs/evidence/extraccion-nube/` (4/4 totales correctos).

## Tab S9+ (gate fisico, NORMATIVO)

Dispositivo: Galaxy Tab S9+ (`SM-X810`, `gts9pwifi`), Android 16 (API 36), arm64-v8a, ~11,4 GB de RAM.
Mismo modelo (`gemma-3n-E2B-it-int4.litertlm`, 3.655.827.456 bytes subido con `adb push`),
`tasks-genai` 0.10.35, `maxTokens=2048`, `topK=1`, `temperature=0.2`. Ejecutado con
`am instrument -e class com.robertomr.tally.extraccion.ExtraccionLocalRealTest` (5/5 OK, 144 s en total).
Log crudo: `tab_s9_logcat.txt`.

| Ticket | loadMillis | generationMillis | promptTokens | responseTokens | ranOnMainThread | total extraido | total papel | acierto |
|---|---|---|---|---|---|---|---|---|
| 1 | 6.887 | 36.620 | 850 | 225 | false | 32,09 | 35,30 | no (bloque equivocado) |
| 2 | 591 | 32.526 | 893 | 192 | false | 3,50 | 3,50 | si (no cae en la base 3,36) |
| 3 | 562 | 25.142 | 740 | 150 | false | 36,10 | 13,90 | no (cogio el cambio) |
| 4 | 620 | 39.263 | 1.009 | 239 | false | 7,91 | 7,91 | si |

- 4/4 pasan el parser compartido con `motor = LOCAL` y nunca en el hilo principal.
- Latencia por ticket (carga + generacion): 7,5 s de carga solo en el primero (modelo frio), despues ~0,6 s;
  generacion 25-39 s, **por debajo del umbral de 60 s** (R-LOC1; plan B de R1 no necesario).
- Calidad: 2/4 totales correctos (tickets 2 y 4), los mismos aciertos y fallos que en el emulador.
  Los fallos de 1 y 3 coinciden con la ambiguedad del OCR (R3); es un hallazgo honesto de la
  comparativa con la nube (4/4 en `docs/evidence/extraccion-nube/`), no un fallo de la feature (D5).
- La Tab no tenia el modelo: el directorio `models/` debe crearlo la app (o `run-as`), no el shell de
  adb; si lo crea el shell la app lo ve como "No descargado".
