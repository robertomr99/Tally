# Evidencia router-automatico (sesion 035)

## Tab S9+ (SM-X810), 2026-10-18
- `MemTotal`: 11454088 kB (ActivityManager.totalMem = 11728986112 B >= 10_000_000_000).
- Modelo `gemma-3n-E2B-it-int4.litertlm` (3655827456 B) presente antes y despues (instalacion -r, sin desinstalar).
- `am instrument` (clase `ExtraccionLocalRealTest`): `qualifiedEnginesAreLocalAndCloudAndTheDefaultIsTheRouter` y
  `routerPicksLocalOnCapableDeviceWithModel` -> OK (2 tests, 113 s).
- El router (`TextEngine` sin calificar) devolvio `motor=LOCAL` y extrajo `texto_crudo_2` con total 3.5 (papel 3.50,
  acierto) en 47149 ms, `TicketExtraido.motor=LOCAL`, fuera del hilo principal. Log: `tab_s9_logcat.txt`.
- Nota de latencia (aclarada por el validator): el log contiene UNA sola linea `metricas` (una extraccion). Su `generationMillis=107103` no es atribuible a la extraccion medida (el `totalMs` del router en la misma ejecucion fue 47149 ms; posible mezcla de ejecuciones en el logcat), asi que esas cifras no son fiables. La repeticion independiente, con logcat limpio, dio `loadMillis=574` y `generationMillis=29390` (~30 s en total), coherente con los 25-41 s del gate de `extraccion-local` y por debajo del limite de 60 s por ticket.

## Pendiente (opcional, no bloqueante)
- Flujo de UI con camara real en la Tab (badge "Motor Local · Gemma 3n", `motor=LOCAL` en el ticket guardado,
  cancelacion fisica de procesado-ui).
- realme GT 2 sin modelo (Nube; requiere cuota Gemini) y registrar su `MemTotal`.
