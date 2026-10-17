# Extracción en nube — verificación 3 (seguridad): sin claves en el APK ni en BuildConfig

Fecha: 2026-10-17 (sesión 018 de `PROGRESS.md`). Fuente: spec
`docs/specs/extraccion-nube.md`, T8 / verificación 3 / Scenario 3.

## Búsquedas ejecutadas (árbol de fuentes/recursos, excluyendo `docs/` y `**/build/**`)

| Patrón | Coincidencias en fuentes/recursos |
| --- | --- |
| `AIza` | 0 |
| `api[_-]?key` | 0 |
| `buildConfigField` | 0 |
| `GEMINI_API` | 0 |
| `(?i)(apikey\|api_key\|api-key\|client_secret\|Bearer \|GEMINI\|generativelanguage\|BuildConfig\.)` en `*.kt` | solo nombres de modelo (`gemini-2.5-flash`) y comentarios en español |
| `(?i)(apikey\|api_key\|AIza[0-9A-Za-z_-]{10,})` en `*.xml,*.kts,*.properties,*.toml` | 0 |

Las únicas coincidencias literales de los patrones de la verificación están en
la propia spec (`docs/specs/extraccion-nube.md`), que cita los patrones.

## Material de configuración del APK

- El único material de configuración es el **mecanismo estándar del plugin
  `com.google.gms.google-services`**: `app/build/generated/res/processDebugGoogleServices/values/values.xml`
  (`google_app_id`, `gcm_defaultSenderId`, `google_api_key`,
  `google_crash_reporting_api_key`, `google_storage_bucket`, `project_id`).
  Son recursos estándar que inyecta Firebase a partir de
  `app/google-services.json`; son config pública de Firebase (restringible por
  package + SHA-1), no claves autorales.
- **No hay ninguna clave autoral en el código**: 0 `buildConfigField` en todo el
  repo (ni en `build-logic/`); de hecho el build no genera `BuildConfig.java`
  para `:app`.
- `app/google-services.json` está **fuera de VCS**: `.gitignore:18` lo excluye
  (`git check-ignore -v app/google-services.json` → `.gitignore:18`) y
  `git status` NO lo muestra.
- El debug token de App Check (`app/appcheck-debug-token.txt`) también está
  fuera de VCS (`.gitignore:25`, patrón idéntico al `google-services.json`) y
  solo llega al dispositivo vía `testInstrumentationRunnerArguments` del test
  instrumentado: **no viaja en el APK** ni en release ni en debug.

## Arquitectura (Scenario 6)

- `:domain:expenses` sin imports de Android/Firebase: búsqueda de
  `import (android|androidx|com.google.firebase)` sobre `domain/**/*.kt` →
  **0 coincidencias**.
- `TextEngine` en `domain/expenses/data/`; `GeminiCloudEngine` `internal` en
  `framework/ai/extraccion/`; único `di.kt` por módulo (`AiModule` en
  `framework/ai/di/di.kt`); sin módulos Gradle nuevos; 0 versiones literales
  fuera de `gradle/libs.versions.toml`.

Conclusión: la verificación 3 queda **cumplida** con esta revisión citable.
