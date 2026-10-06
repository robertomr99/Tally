# Feature Implementation Spec: Proyecto multi-módulo compila y lanza en los dos móviles

## Source Feature

- `id`: bootstrap-gradle
- `area`: base
- `depends_on`: []
- `status`: passing (verificado 2026-09-30; Repair Brief de validación aplicado 2026-10-06)
- `source`: `feature_list.json`

## Goal

Crear el proyecto Gradle multi-módulo de Tally según `AGENTS.md`: 8 módulos
autorizados + `build-logic/` con convention plugins (decisión explícita del
usuario, excepción a las reglas "No crees módulos sin preguntar" y "No toques
build-logic/"). El proyecto compila, pasa tests dummy y el APK se instala y
abre en los dos móviles de prueba con tema Forest Precision y una pantalla
placeholder. Tras esto, las features siguientes solo añaden código dentro de
módulos, nunca estructura.

## Non-Goals

- Navegación, bottom bar ni pantallas del diseño más allá del placeholder
  (features `navegacion-base` y posteriores).
- Room schema, cámara, OCR, motores de IA, descarga de modelo (otras features).
- Dependencias de MediaPipe o Firebase (features `spike-gemma-mediapipe` y
  `extraccion-*`).
- Pulido visual, animaciones o estados vacíos.
- Modificar los documentos de descubrimiento salvo los impactos listados.

## Job Story

Cuando empiezo a construir Tally sobre un repo que solo tiene documentación,
quiero un proyecto Android multi-módulo que compile e instale en mis dos
móviles de prueba,
para que cada feature siguiente implemente comportamiento sin tocar la
estructura.

## Users And Permissions

- Autor/estudiante: único actor (sin auth en el producto). Instala el APK por
  USB en Galaxy Tab S9+ y realme GT 2.

## Acceptance Scenarios

### Scenario 1: build y tests pasan en Windows

Given un checkout limpio con JDK y Android SDK instalados
When ejecuto `gradlew.bat assembleDebug` y `gradlew.bat testDebugUnitTest`
Then ambos terminan con BUILD SUCCESSFUL y el test unitario dummy pasa.

### Scenario 2: la app arranca en ambos móviles

Given el APK debug instalado en la Tab S9+ y el realme GT 2
When el usuario abre Tally
Then se muestra el placeholder con tema Forest Precision (colores y
tipografía de `DESIGN.md`) sin crash.

### Scenario 3: estructura de módulos respetada

Given el proyecto creado
When inspecciono `settings.gradle.kts` y los `build.gradle.kts`
Then existen exactamente los 8 módulos acordados y `:domain:*` no tiene
dependencias de Android.

### Scenario 4: init.sh sigue siendo la ruta estándar

Given el proyecto bootstrapado
When ejecuto `bash init.sh`
Then ejecuta la verificación base (build de debug + tests) sin arrancar
procesos largos e imprime los comandos de instalación como seguimiento.

## Repository Research

### Files Inspected

- `AGENTS.md` — reglas de módulos, stack y "Dónde va cada cosa"; reglas
  "No crees módulos sin preguntar" y "No toques build-logic/" resueltas por
  el usuario en la sesión de planificación.
- `PROGRESS.md` — estado verificado: pre-bootstrap; siguiente feature: esta.
- `feature_list.json` — verificación exigida a esta feature (build + instalación
  en ambos móviles + estructura de módulos).
- `init.sh` — variante pre-bootstrap; esta feature la convierte a la variante
  bootstrapped.
- `README.md` — vacío (no se toca).
- `docs/build-brief.md` — esta feature es la primera de la semana 1.
- `docs/technical-discovery.md` — entorno Windows, despliegue por APK a mano,
  versiones centralizadas en toml.
- `DESIGN.md` — tokens del front matter (colores, tipografía, formas) para el
  tema placeholder.
- `docs/adr/0001-doble-motor-con-router.md`, `docs/adr/0002-ocr-en-dos-etapas-con-ml-kit.md`
  — no aplican (sin IA en esta feature).
- `docs/risks-and-open-questions.md` — esta feature cubre la comprobación del
  toolchain en Windows (Constraint de technical-discovery); R1 queda intacto.

### Existing Patterns To Follow

- No hay código de aplicación todavía: los patrones son los de `AGENTS.md` —
  un `di.kt` por módulo, `domain` sin Android, versiones solo en
  `gradle/libs.versions.toml`, identificadores en inglés y comentarios en
  español, Conventional Commits.
- `init.sh` actual sigue la plantilla pre-bootstrap del harness; debe pasar a
  la variante bootstrapped (checks no bloqueantes, nunca procesos largos).

### Current Gaps

- No existe proyecto Gradle alguno; toolchain de esta máquina (JDK, Android
  SDK, `ANDROID_HOME`) sin verificar todavía.
- Sin CI: la verificación es local + instalación manual por USB.
- Sin fuentes ni tests: hay que empaquetar Inter y crear un test dummy.
- `docs/architecture.md` no existe aunque `AGENTS.md` lo referencia.

## Technical Approach

- **Toolchain**: JDK 17+ (requisito de AGP 8.x), Android SDK con
  compileSdk/targetSdk actuales. Tarea 1 verifica `gradlew -v` y
  `ANDROID_HOME` en Windows y registra el resultado; si falta algo, reportar y
  detenerse (sin adivinar rutas).
- **Versiones**: Gradle Wrapper 8.x, AGP 8.x, Kotlin 2.x, KSP (versión ligada a
  la de Kotlin), Compose BOM, Hilt 2.x, Room 2.6/2.7, Turbine,
  kotlinx-coroutines-test. Versiones concretas coherentes entre sí al escribir
  el código, **solo** en `gradle/libs.versions.toml` (regla de AGENTS.md).
- **build-logic/**: included build con 4 convention plugins —
  `tally.android.application`, `tally.android.library`,
  `tally.android.compose`, `tally.hilt` (nombres provisionales). Los plugins
  leen el catálogo versionado desde `gradle/libs.versions.toml` (catalog
  compartido en `build-logic/settings.gradle.kts`). Ningún módulo declara
  versiones literales.
- **Módulos (8, autorizados)**:
  - `:app` — application + compose + hilt; `TallyApplication` (@HiltAndroidApp),
    `MainActivity` (single-activity), Manifest.
  - `:domain:core`, `:domain:expenses` — jvm-library puros (sin Android).
  - `:framework:database`, `:framework:ai` — android library `internal`, con
    su `di.kt` y sin implementación todavía.
  - `:feature:common` — compose + hilt; tema, tipografía y utilidades de UI.
  - `:feature:resumen` — compose + hilt; pantalla placeholder.
  - `:test:unit` — librería Kotlin con dobles compartidos y test dummy
    (dependencias de Turbine/coroutines-test declaradas).
- **Tema**: `MaterialTheme` en `:feature:common` con el `colorScheme` del front
  matter de `DESIGN.md` (primary `#002718`, secondary-container `#acefc2`,
  surface `#f5fbf6`, etc.) y `Typography` con Inter. Inter se empaqueta en
  `res/font` (2 pesos: regular/semibold); si la descarga no fuera posible,
  fallback a `sans-serif` y dejar constancia en `PROGRESS.md`.
- **init.sh**: pasa a la variante bootstrapped — ejecuta los checks base
  (`assembleDebug` + `testDebugUnitTest`) sin bloquear largamente, imprime
  `gradlew.bat :app:installDebug` como seguimiento manual y nunca arranca
  procesos largos (no `dev`, no emuladores).

## Expected File Changes

Rutas provisionales: todo es creación inicial (el repo no tiene código aún).

- `gradle/libs.versions.toml` — create; único catálogo de versiones.
- `settings.gradle.kts`, `build.gradle.kts` raíz, `gradle.properties`,
  `gradle/wrapper/*` — create; wrapper, registro de módulos e included build.
- `build-logic/` — create; 4 convention plugins (AUTORIZADO por el usuario).
- `app/`, `domain/core/`, `domain/expenses/`, `framework/database/`,
  `framework/ai/`, `feature/common/`, `feature/resumen/`, `test/unit/` —
  create; 8 módulos con `di.kt` donde aplique.
- `app/src/main/.../TallyApplication.kt`, `MainActivity.kt`,
  `AndroidManifest.xml` — create; single-activity + tema.
- `feature/common/src/main/.../theme/` — create; `Color.kt`, `Type.kt`,
  `Theme.kt` con los tokens de `DESIGN.md`.
- `feature/common/src/main/res/font/` — create; Inter (o fallback documentado).
- `feature/resumen/src/main/.../ResumenScreen.kt` — create; placeholder.
- `test/unit/src/test/.../SmokeTest.kt` (o equivalente) — create; test dummy.
- `init.sh` — modify; variante bootstrapped.
- `docs/architecture.md` — create; mínimo (diagrama de módulos y dirección de
  dependencias resultantes).
- `PROGRESS.md`, `feature_list.json` — update; evidencia al completar.

## Visual Design Impact

- UI involved: yes (mínima: placeholder).
- Design source: `DESIGN.md` (tokens) + `docs/design/stitch/01-splash/`
  (referencia de apariencia del placeholder: marca "Tally").
- Screens or states affected: pantalla placeholder única (sin datos, sin
  navegación).
- New design artifact required: no — `DESIGN.md` cubre los tokens; el
  placeholder solo los aplica.

## Durable Documentation Impact

- `ARCHITECTURE.md` (raíz): not needed — la arquitectura operativa ya vive en
  `AGENTS.md`; crear otro duplicaría reglas.
- `docs/architecture.md`: create — `AGENTS.md` lo referencia y no existe;
  contenido mínimo: diagrama de módulos y regla de dependencias de este
  bootstrap. (Justificación de la excepción a "solo docs del harness": rompe la
  referencia rota que `AGENTS.md` ya declara.)
- `CONSTRAINTS.md`: not needed — las reglas MUST/MUST NOT duras ya están en
  `AGENTS.md` ("Qué no hacer"); duplicarlas crearía deriva.
- `AGENTS.md`: not needed — el flujo de arranque (`bash init.sh`) no cambia;
  solo cambia la variante interna del script.
- Other docs: `PROGRESS.md` y `feature_list.json` — update obligatorio con
  evidencia al completar.

## Implementation Plan

1. Verificar toolchain Windows (JDK 17+, SDK, `ANDROID_HOME`, `gradlew -v`) y
   registrar el resultado; detenerse si falta algo.
2. Crear `gradle/libs.versions.toml`, Gradle wrapper y `settings.gradle.kts`
   con los 8 módulos y el included build `build-logic/`.
3. Implementar los 4 convention plugins de `build-logic/` sobre el catálogo.
4. Crear los 8 módulos con contenido mínimo (`di.kt`, Manifest donde aplique).
5. Tema Forest Precision en `:feature:common` + `MainActivity` y placeholder en
   `:feature:resumen` desde `:app`.
6. Test dummy en `:test:unit` (o `:domain:core`) y `init.sh` a variante
   bootstrapped.
7. Verificar build y tests; instalar en ambos móviles (manual) y documentar.
8. Crear `docs/architecture.md` mínimo y actualizar `PROGRESS.md` +
   `feature_list.json` con evidencia.

## Implementation Tasks

- [x] T1 Toolchain verificada (`gradlew -v`, `ANDROID_HOME`); resultado registrado.
- [x] T2 `gradle/libs.versions.toml` con versiones coherentes (AGP/Kotlin/KSP/
      Compose BOM/Hilt/Room/Turbine/coroutines-test).
- [x] T3 `build-logic/` con 4 convention plugins usando el catálogo.
- [x] T4 `settings.gradle.kts` + wrapper + 8 módulos registrados.
- [x] T5 `:app` (`TallyApplication` @HiltAndroidApp, `MainActivity`, Manifest).
- [x] T6 `:domain:core` y `:domain:expenses` como jvm-library puros.
- [x] T7 `:framework:database` y `:framework:ai` como android library
      `internal` con `di.kt` y sin implementación.
- [x] T8 `:feature:common` con tema Forest Precision + Inter (o fallback
      documentado en PROGRESS).
- [x] T9 `:feature:resumen` placeholder y `:test:unit` con test dummy.
- [x] T10 `init.sh` → variante bootstrapped (checks no bloqueantes).
- [x] T11 `assembleDebug` + `testDebugUnitTest` en verde; instalación manual en
      ambos móviles.
- [x] T12 `docs/architecture.md` mínimo + `PROGRESS.md` + `feature_list.json`
      con evidencia.

## Verification Plan

- `gradlew.bat assembleDebug` → BUILD SUCCESSFUL.
- `gradlew.bat testDebugUnitTest` → BUILD SUCCESSFUL con test dummy en verde.
- Inspección de `settings.gradle.kts`/build files → exactamente los 8 módulos;
  `:domain:*` sin dependencias de Android (comprobable también con
  `gradlew.bat :domain:core:dependencies`).
- `bash init.sh` → ejecuta la verificación base, sin procesos largos, e imprime
  el comando de instalación como seguimiento.
- Manual: APK instalado en Tab S9+ y realme GT 2; la app abre con placeholder
  y tema correctos (captura de pantalla como evidencia si hay dispositivos a
  mano; si no, dejarlo explícito en `evidence` y `PROGRESS.md`).
- E2E persistente: no existe harness E2E en el repo y esta feature no introduce
  flujos de usuario ni API; la verificación de UI es manual por ser bootstrap.
  No procede crear cobertura E2E todavía (justificado).

## Evidence To Capture

- Resúmenes de salida de `gradlew.bat assembleDebug` y
  `gradlew.bat testDebugUnitTest` (BUILD SUCCESSFUL).
- Lista de módulos verificada (`settings.gradle.kts`).
- Captura de la app abierta en cada móvil, o nota explícita de pendiente en
  `PROGRESS.md` si no hay dispositivos disponibles.
- Salida de `bash init.sh` con el comportamiento esperado.
- Entradas actualizadas: `feature_list.json` (`evidence` de `bootstrap-gradle`)
  y `PROGRESS.md` (sesión 002).

## Validator Checklist

- [x] Implementation stays within this feature's scope.
- [x] Acceptance scenarios pass.
- [x] Verification evidence is present.
- [x] Persistent E2E coverage not applicable: bootstrap sin flujos de usuario
      ni API y sin harness E2E en el repo (justificado en Verification Plan).
- [x] `feature_list.json` y `PROGRESS.md` fueron actualizados correctamente.
- [x] No unrelated product behavior or extra feature work was added.
- [x] Solo los 8 módulos autorizados; `build-logic/` creado tal como fue
      autorizado explícitamente por el usuario.
- [x] Ninguna versión literal fuera de `gradle/libs.versions.toml` (regla
      `AGENTS.md`).
