# Feature Implementation Spec: Ajustes básicos, forzar motor

## Source Feature

- `id`: ajustes-motor
- `area`: ajustes
- `title`: Ajustes básicos: forzar motor y estado del modelo
- `user_visible_behavior`: Pantalla de ajustes mínima: forzar motor (Auto/Local/Nube) y estado del modelo local; el override cambia el comportamiento del router.
- `depends_on`: ["router-automatico", "descarga-modelo"] (ambas `accepted`)
- `status`: not_started (planificada 2026-10-18)
- `verification` (fuente): (1) forzar Nube en la Tab S9+ fuerza Nube en la siguiente extracción y viceversa; (2) el ajuste persiste entre ejecuciones; (3) unit test de la preferencia y su efecto en el router.
- `notes` (fuente): alcance cerrado, no pantalla de preferencias genérica.

## Goal

En la pantalla Ajustes existente (`:feature:ajustes`) aparece una sección "Motor de extracción" con tres opciones (Auto / Local / Nube). La elección se persiste entre ejecuciones y `EngineRouter` la lee en la **siguiente extracción**. El estado del modelo local ya se muestra (descarga-modelo) y se reutiliza. ADR 0001: sin fallback silencioso; un motor forzado que no puede ejecutarse produce un error visible, nunca un cambio de motor.

## Non-Goals

- Pantalla de preferencias genérica, otros ajustes, tema, idioma.
- Cambiar el reintento explícito "Probar con el otro motor": sigue ignorando la preferencia (spec reintento-otro-motor).
- Pre-detección de red, fallback automático, cuotas de Gemini.
- Módulos Gradle nuevos, dependencias nuevas, Room/`schemas/`, `init.sh`, `build-logic/`.
- Migrar el almacén del token de Hugging Face. No se lee ni imprime el token en ningún momento.

## Job Story

When quiero comparar motores o mi demo exige uno concreto,
I want fijar Auto/Local/Nube una vez y que se respete en cada extracción y tras reiniciar,
so I can controlar qué motor se usa sabiendo siempre cuál produjo el resultado (badge).

## Acceptance Scenarios

### S1: selector y valor por defecto
Given instalación limpia, When abro Ajustes, Then la sección "Motor de extracción" muestra tres opciones y Auto está seleccionada (tags `motor_opcion_auto`, `motor_opcion_local`, `motor_opcion_nube`; contenedor `seccion_motor`).

### S2: forzar y efecto en la siguiente extracción
Given elijo Nube, When escaneo un ticket, Then la extracción usa Nube aunque el dispositivo sea capaz y el modelo esté descargado; el badge dice Nube. Y simétricamente Local (con modelo y dispositivo capaz) usa Local aunque AUTO habría elegido igual o no. Cambiar la preferencia durante una extracción en curso no la afecta; aplica a la siguiente.

### S3: persistencia
Given elegí Local, When la app se cierra y reabre (proceso nuevo), Then Ajustes sigue mostrando Local y el router lo usa.

### S4: Local forzado sin modelo o dispositivo no capaz
Given preferencia Local y (modelo no descargado o dispositivo no capaz), When extraigo, Then falla con `MotorNoDisponibleException(LOCAL, SIN_MODELO | NO_CAPAZ)` sin invocar ningún motor, Procesado muestra `ExtraccionFallida` con el motivo (mismos textos de `motivoNoDisponible`: "Descarga el modelo local en Ajustes para usarlo" / "Este dispositivo no puede ejecutar el motor local") y badge FALLIDO de Local. El botón "Probar con Nube" (reintento) sigue disponible. Nunca se cambia a Nube solo. Prioridad si fallan ambas: `NO_CAPAZ`.

### S5: Nube forzada sin red
Given preferencia Nube sin conectividad, When extraigo, Then el fallo propio de `GeminiCloudEngine` se muestra como `ExtraccionFallida` (badge FALLIDO Nube) y el usuario puede "Reintentar" o "Probar con Local". Sin pre-detección de red ni cambio silencioso.

### S6: aviso anticipado en Ajustes
Given selecciono Local y el modelo no está `Descargado`, Then bajo el selector se ve un aviso (tag `motor_aviso_local`): "Local necesita el modelo descargado; sin él la extracción fallará". Informativo: no impide elegir.

### S7: reintento no afectado
Given preferencia Nube, When reintento con "Probar con Local" (disponible), Then se usa Local: el reintento explícito ignora la preferencia.

## Repository Research (inspeccionado)

- `domain/expenses/.../data/EngineRouter.kt`: `elegirMotor(pref, capaz, modelo)` pura (LOCAL y NUBE incondicionales, AUTO exige capaz+modelo); `disponibilidadMotor(...)`; `EngineRouter.extract(textoCrudo)` llama `preferencia.actual()` por extracción (ya relee cada vez, no hay que cambiar eso); `extract(motor, texto)` valida disponibilidad y lanza `MotorNoDisponibleException`.
- `.../data/PreferenciaMotorSource.kt`: `fun interface actual(): PreferenciaMotor`, comentario "ajustes-motor lo reimplementará".
- `.../entities/PreferenciaMotor.kt`: enum AUTO/LOCAL/NUBE.
- `framework/ai/.../di/di.kt`: `providePreferenciaMotorSource()` fijo AUTO ("único punto que sustituirá ajustes-motor"); engine router singleton.
- `framework/ai/.../modelo/FileModelAccessTokenStore.kt` (solo ubicación; contenido sensible no leído) y `ModelAccessTokenStore` (interfaz `hayToken: Flow<Boolean>`, `guardar`, `borrar`): precedente de almacén de ajustes en `framework/ai` expuesto por interfaz de dominio y use cases `Observar*/Configurar*`.
- `feature/ajustes`: `AjustesViewModel` (combine de flows, `AjustesUiState`), `AjustesScreen` (Cards, tags `seccion_modelo_local`, etc.), `AjustesState`, tests `AjustesViewModelTest`. Comentario de pantalla: "Forzar motor es de ajustes-motor".
- `feature/escanear/.../EscanearViewModel.kt` (~l.550): `mensajeDeError` ya traduce `MotorNoDisponibleException` a su motivo; los demás errores usan `error.message`.
- `libs.versions.toml`: grep de datastore/preferences sin resultados (no hay DataStore). No inspeccionado: `test:unit` (dobles existentes), `app` androidTest de Ajustes (no hay androidTest en `feature/ajustes`), `GemmaLocalEngine` completo, mensaje de `ModeloLocalNoDisponibleException`.

## Technical Approach

D1. **Persistencia: SharedPreferences, sin dependencia nueva.** Es API de plataforma (`framework` puede usarla), basta un único enum, y evita DataStore (nueva dependencia, versión en `libs.versions.toml`, I/O asíncrono que choca con `PreferenciaMotorSource.actual()` síncrono). Se lee con carga perezosa del fichero y se escribe con `apply()`. Si el usuario prefiere DataStore, ver decisión abierta 1.
D2. **Contrato de dominio** (`domain/expenses/data/`): `interface PreferenciaMotorStore : PreferenciaMotorSource { val preferencia: Flow<PreferenciaMotor>; fun guardar(p: PreferenciaMotor) }`. `actual()` es lo que lee el router; `preferencia` alimenta la UI. Valor ilegible o desconocido en disco -> AUTO.
D3. **Implementación** `SharedPreferencesPreferenciaMotor` (`internal`, `framework/ai/preferencias/`): `MutableStateFlow` interno inicializado desde el fichero; `guardar` actualiza flow y `apply()`. `di.kt` sustituye el provider fijo por este (singleton, `@ApplicationContext`) y expone `PreferenciaMotorStore` y `PreferenciaMotorSource` con la misma instancia.
D4. **Use cases** (`domain/expenses/usecases/`): `ObservarPreferenciaMotor(): Flow<PreferenciaMotor>`, `ConfigurarPreferenciaMotor(p)` (patrón `ObservarTokenModelo`/`ConfigurarTokenModelo`).
D5. **Router, Local forzado** (S4): en `EngineRouter.extract(textoCrudo)`, si la preferencia es LOCAL, comprobar `disponibilidad(LOCAL)` y lanzar `MotorNoDisponibleException` si no es `DISPONIBLE` antes de delegar. `elegirMotor` y sus tests actuales no cambian (política pura intacta). NUBE no comprueba nada (S5). El mensaje a UI ya lo traduce `EscanearViewModel`.
D6. **UI**: `AjustesViewModel` añade `preferencia` al `combine`; `onElegirMotor(p)`; `AjustesUiState.preferencia` y `avisoLocal` (= LOCAL seleccionado y `estado !is Descargado`). `AjustesScreen` añade `SeccionMotor` (Card + `RadioButton`/`SegmentedButton`, M3, estilo de `SeccionModeloLocal`) **encima** de "Modelo local"; el estado del modelo ya cubre la mitad "estado del modelo local" de la feature.

## Expected File Changes

- Nuevos: `domain/expenses/.../data/PreferenciaMotorStore.kt`; `.../usecases/ObservarPreferenciaMotor.kt`, `ConfigurarPreferenciaMotor.kt`; `framework/ai/.../preferencias/SharedPreferencesPreferenciaMotor.kt`; tests `ObservarPreferenciaMotorTest`/`ConfigurarPreferenciaMotorTest`, `SharedPreferencesPreferenciaMotorTest` (Robolectric solo si ya está en `framework:ai`; si no, ver Tasks 3).
- Modificados: `EngineRouter.kt` (D5), `PreferenciaMotorSource.kt` (comentario), `PreferenciaMotor.kt` (comentario), `framework/ai/.../di/di.kt`, `AjustesViewModel.kt`, `AjustesScreen.kt`, `AjustesViewModelTest.kt`, `EngineRouterTest.kt`, doble `FakePreferenciaMotorStore` en `:test:unit` si hay fakes compartidos de este tipo.
- Sin cambios: `feature_list.json`/`PROGRESS.md` los actualiza orquestador/implementer tras validar; `libs.versions.toml`, Gradle, `AndroidManifest`.

## Visual Design Impact

Una Card más en Ajustes con el mismo lenguaje que "Modelo local" (surfaceContainer, esquinas 12 dp, `titleMedium`). Cuerpo en español, aviso con color `error` como `modelo_aviso`. `DESIGN.md` no leído en esta planificación: el implementer lo consulta para la forma del selector. No hay mockup Stitch específico: confirmar que no existe uno en `docs/design/stitch/`.

## Durable Documentation Impact

- `AGENTS.md`: not needed (sin regla nueva). `ARCHITECTURE.md`/`CONSTRAINTS.md`: no existen o no necesarios. `docs/adr/0001`: not needed (ya prevé el override y la ausencia de fallback). `docs/architecture.md` / `docs/technical-discovery.md`: actualizar solo si describen la preferencia como "fija"; revisar con grep. `PROGRESS.md`: actualizar al cierre.

## Implementation Plan / Tasks

1. Dominio: `PreferenciaMotorStore`, dos use cases + tests (Turbine para `Observar`).
2. `EngineRouter`: D5 + tests (matriz: preferencia x capaz x modelo; LOCAL sin modelo -> `MotorNoDisponibleException` sin invocar fakes; LOCAL no capaz -> `NO_CAPAZ`; NUBE ignora capacidad/modelo; la preferencia se relee en cada extracción: cambiarla entre dos llamadas cambia el motor; `extract(motor,...)` sigue ignorándola).
3. Framework: `SharedPreferencesPreferenciaMotor` + DI. Test JVM del fallback a AUTO con valor inválido y de persistencia vía instancia nueva sobre el mismo fichero (Robolectric solo si ya está en el módulo; si no, abstraer el almacenamiento tras una mini-interfaz clave-valor interna o validar en instrumentado).
4. UI: ViewModel (estado por `combine`, sin `load()`), `SeccionMotor`, tags, tests de VM (por defecto AUTO, elegir llama al use case, aviso Local sin modelo).
5. E2E/instrumentado en `:app` (patrón de `ProcesadoFlujoTest`, `am instrument`): elegir Nube en Ajustes y comprobar el estado/persistencia; Hilt con `@TestInstallIn`.
6. Verificación física opcional en Tab S9+ (sin dispositivos físicos para el planner/implementer sin permiso del usuario).

## Verification Plan

- JVM: `gradlew.bat assembleDebug testDebugUnitTest --console=plain` o `bash init.sh`; más `:domain:expenses:test`, `:framework:ai:testDebugUnitTest`, `:feature:ajustes:testDebugUnitTest`.
- Instrumentado en `emulator-5554`/`5556` vía `am instrument` (no `connected*`): selector visible, selección persiste tras `am force-stop` + relanzar.
- Manual en Tab S9+ (opcional, acordar con usuario): Nube forzada -> badge Nube; Local forzado sin modelo -> error visible; reinicio.

## Evidence To Capture

Salida de tests (nombres y conteos) de router/use cases/VM; log de `am instrument`; captura o dump UI de Ajustes con las tres opciones y del error S4; nota de qué se probó solo en emulador.

## Validator Checklist

- [ ] Preferencia persistida y leída por el router en cada extracción; default AUTO.
- [ ] LOCAL sin modelo/no capaz: error visible, ningún motor invocado, no hay cambio a Nube.
- [ ] NUBE sin red: error del motor, sin fallback.
- [ ] `elegirMotor` y reintento explícito sin cambios de comportamiento.
- [ ] Sin dependencias/módulos nuevos; versiones solo en `libs.versions.toml`; `domain` sin Android.
- [ ] Token de HF no leído ni impreso en código nuevo, logs o evidencia.
- [ ] Un `di.kt` por módulo; implementaciones `internal`.
- [ ] Sin pantalla de preferencias genérica.

## Open Decisions (defaults)

1. Almacén: SharedPreferences (default) vs DataStore.
2. Local forzado con dispositivo no capaz: bloquear con `NO_CAPAZ` (default, evita OOM/crash) vs intentar igualmente (como hoy, D1 router-automatico).
3. Aviso Ajustes: solo por modelo no descargado (default); no se muestra capacidad ni red.
4. Mensaje de Nube sin red: el del motor (default) vs texto específico "Sin conexión".
