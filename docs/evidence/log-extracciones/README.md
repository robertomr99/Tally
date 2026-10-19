# Evidencia: log-extracciones

Entorno: `emulator-5556` (Pixel_10_Pro AVD). Nunca dispositivos físicos, nunca `pm clear`.

## Captura de la sección debug (360 dp)

`01-ajustes-diagnostico-360dp.png`: sección "Diagnóstico de extracciones" de Ajustes con
resumen por motor, filas (REI = reintento, ROU = router), "Copiar CSV", "Borrar registro" y
aviso "CSV copiado". Tomada con `wm density 569` (360 dp) y restaurada con `wm density reset`,
mediante `-e evidencia <nombre>` de `LogExtraccionesFlujoTest`.

## CSV copiado (solo metadatos)

```
id,instante_iso,motor,origen,resultado,tipo_fallo,clase_error,duracion_ms,longitud_texto
8,2026-10-19T18:08:31.855Z,LOCAL,REINTENTO,FALLO,MOTOR_NO_DISPONIBLE,MotorNoDisponibleException,2,3
7,2026-10-19T18:08:31.843Z,LOCAL,ROUTER,FALLO,MOTOR_NO_DISPONIBLE,MotorNoDisponibleException,2,28
```

El texto enviado en la fila ROUTER era `TEXTO-SECRETO-DEL-TICKET-E2E` (28 caracteres): solo aparece
la longitud. El test aserta que el CSV no contiene ese texto ni el mensaje de la excepción.

## Migración 1 -> 2 sobre datos reales (install -r, sin pm clear)

BD del emulador antes (build anterior instalado, `PRAGMA user_version` = 1) y después de
instalar el nuevo encima y abrir la app:

| | user_version | tickets (n, suma total, min id, max id) | lineas (n, importe) | extracciones_log |
| --- | --- | --- | --- | --- |
| antes | 1 | 28, 805.0, 1, 408 | 17, 677.0 | no existe |
| después | 2 | 28, 805.0, 1, 408 | 17, 677.0 | existe, 0 filas, con índice |

Copia de seguridad previa de la BD fuera del repo. Inspección con `sqlite3` de platform-tools
sobre una copia extraída con `run-as ... cat`.

## Tests

- JVM: `EngineConRegistroTest` 11/11, `RegistroExtraccionesUsecasesTest` 6/6,
  `AjustesViewModelTest` 16/16.
- `:framework:database` instrumentado: OK (28 tests; incluye `MigrationTest` 2 y
  `ExtraccionLogDaoTest` 4).
- `:app` E2E (sin tests reales de nube/local): OK (45 tests, incluye `LogExtraccionesFlujoTest` 2,
  ejecutados sin assumption failure).
