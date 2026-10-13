# Feature Implementation Spec: Navegación con bottom bar de 3 destinos

## Source Feature

- `id`: navegacion-base
- `area`: base
- `depends_on`: [bootstrap-gradle] (accepted 2026-10-06)
- `status`: not_started (planificada 2026-10-13)
- `source`: `feature_list.json`

## Goal

Montar el esqueleto de navegación de Tally: un `NavHost` único en `:app` con
tres destinos (Resumen / Gastos / Escanear) y la bottom bar del mockup 02 con
pill activo según `DESIGN.md`. Las tres pantallas son placeholders navegables
sin crash (Resumen ya existe como placeholder); las pantallas reales llegan en
features posteriores (`resumen-total`, `listado-gastos`, `captura-foto`, ...).
Al terminar, toda feature posterior solo añade contenido dentro de un destino
existente, nunca estructura de navegación.

## Non-Goals

- Contenido real de Resumen/Gastos/Escanear (features posteriores).
- FAB "Escanear" anclado sobre la bottom bar (mockup 02): pertenece al flujo de
  captura/resumen, no al shell de navegación.
- ViewModels ni `XState`: los placeholders no tienen lógica (mismo criterio que
  el placeholder actual de `:feature:resumen`; `tally.hilt` llega con el primer
  ViewModel — hallazgo Low de la validación de `bootstrap-gradle`).
- Room, cámara, OCR, motores de IA, descarga de modelo.
- NavigationRail ni layouts expandidos con rail (`DESIGN.md`: no MVP).
- Cambiar `init.sh`, ADRs ni documentos de descubrimiento.

## Job Story

Cuando abro Tally tras el bootstrap,
quiero moverme entre Resumen, Gastos y Escanear desde la bottom bar con el pill
activo del diseño,
para tener el esqueleto de navegación donde se colgarán las pantallas reales
sin rehacer la estructura.

## Users And Permissions

- Autor/estudiante: único actor (sin auth en el producto). Navega con la bottom
  bar. Verificación por defecto en el emulador `emulator-5554` (ver Open
  Questions sobre el criterio "ambos móviles").

## Acceptance Scenarios

### Scenario 1: navegación entre los 3 destinos

Given la app arrancada mostrando el placeholder de Resumen (destino inicial)
When el usuario toca "Gastos" y después "Escanear" en la bottom bar
Then se muestra el placeholder del destino tocado, el pill activo se mueve al
item actual y no hay crash.

### Scenario 2: pill activo según diseño

Given el usuario en cualquiera de los 3 destinos
When observa la bottom bar
Then el item activo muestra el icono en pill `secondary-container` +
`on-secondary-container` con label semibold; los inactivos muestran icono y
label en `on-surface-variant` (mockup 02 / `DESIGN.md` Colors).

### Scenario 3: rutas tipadas y límites de módulos (revisión de código)

Given el código implementado
When inspecciono `:app` y los módulos `feature`
Then el `NavHost` y las rutas (`sealed class`) viven en `:app`; ninguna
pantalla de `:feature:*` recibe `NavController` (solo callbacks/lambdas).

### Scenario 4: back del sistema

Given el usuario en "Gastos" o "Escanear"
When pulsa Atrás
Then vuelve a Resumen (destino inicial); un segundo Atrás sale de la app
(comportamiento estándar de bottom navigation).

## Repository Research

### Files Inspected

- `AGENTS.md` — reglas de Navegación (NavHost en `:app`, rutas `sealed class`,
  pantallas sin `NavController`), "No crees módulos Gradle nuevos sin
  preguntar", "un `di.kt` por módulo", capa de presentación.
- `PROGRESS.md` — sesión 007; emulador de referencia `emulator-5554`; criterio
  de dispositivo enmendado a emulador en `bootstrap-gradle` (usuario,
  2026-09-30); siguiente feature: esta.
- `feature_list.json` — verificación exigida (3 criterios; ver Goal/Open Questions).
- `settings.gradle.kts` — 8 módulos; no existen `:feature:gastos` ni
  `:feature:escanear`.
- `app/build.gradle.kts`, `app/src/main/java/com/robertomr/tally/MainActivity.kt`
  — `setContent { TallyTheme { ResumenScreen() } }` directo; sin `NavHost` ni
  `Scaffold`; sin `testInstrumentationRunner`.
- `feature/resumen/src/main/java/.../ResumenScreen.kt` — placeholder existente
  (emblema Tally sobre `primary`), sin ViewModel (patrón placeholder).
- `feature/resumen/build.gradle.kts` — patrón de módulo feature:
  `tally.android.library` + `tally.android.compose`, sin `tally.hilt`.
- `feature/common/src/main/java/.../theme/{Color,Type,Theme}.kt` — tokens
  Forest Precision disponibles (`ForestSecondaryContainer`,
  `ForestOnSecondaryContainer`, `ForestSurfaceContainerLow`, ...).
- `docs/design/stitch/02-resumen-principal/code.html` (líneas 402–433) y
  `docs/design/stitch/03-listado-gastos/code.html` (416–437) — estructura exacta
  de la bottom bar: 3 items (Resumen `insights`, Gastos `receipt_long`,
  Escanear `document_scanner`), alto `h-20` (80dp), fondo
  `surface-container-low`, pill `rounded-full` en el item activo, label semibold.
- `DESIGN.md` — Layout (bottom bar de 3 destinos; FAB aparte), Colors (pill
  activo = `secondary-container`/`on-secondary-container`), Shapes (pill
  9999dp), Accessibility (≥44dp táctil), Responsive (la bottom bar se mantiene
  en tablet).
- `gradle/libs.versions.toml` — sin `navigation-compose` ni
  `material-icons-extended`; Compose BOM `2026.02.01` presente.
- `build-logic/AndroidComposeConventionPlugin.kt` — añade BOM+ui+ui-graphics+
  tooling-preview+material3; navigation e iconos hay que declararlos en los
  módulos.
- `build-logic/AndroidApplicationConventionPlugin.kt` — no fija
  `testInstrumentationRunner`; cada módulo lo declara (patrón de
  `framework/ai/build.gradle.kts`).
- `docs/architecture.md` — diagrama de módulos a actualizar si se crean módulos.
- `docs/adr/0001-*`, `docs/adr/0002-*` — no aplican (sin IA en esta feature).

### Existing Patterns To Follow

- Módulo feature = `tally.android.library` + `tally.android.compose`; sin
  `tally.hilt` hasta que haya ViewModel.
- Versiones solo en `gradle/libs.versions.toml` (0 literales en
  `**/*.gradle.kts`, verificado en sesiones previas).
- Identificadores en inglés; comentarios y copy de UI en español.
- `androidTest` instrumentado ya probado en `:framework:ai`
  (`ANDROID_SERIAL=... gradlew.bat ... connectedDebugAndroidTest`).

### Current Gaps

- No hay navegación alguna: `MainActivity` compone `ResumenScreen` directo.
- No existen `:feature:gastos` ni `:feature:escanear` (ver Open Questions).
- `navigation-compose` e iconos de la barra no están en el catálogo
  (`insights`, `receipt_long`, `document_scanner` están en
  `material-icons-extended`, no en `material-icons-core`).
- `:app` sin `testInstrumentationRunner` ni deps de `androidTest`.
- Criterio "en ambos móviles" de `feature_list.json` vs. uso de emulador
  (ver Open Questions).

## Technical Approach

- **Shell en `:app`**: nuevo `navigation/TallyApp.kt` con `Scaffold` +
  `NavHost(navController, startDestination = Resumen)` y
  `bottomBar = TallyBottomBar(...)`. `MainActivity.setContent` pasa a
  `TallyTheme { TallyApp() }`. El `NavController` solo vive en `:app`.
- **Rutas tipadas**: `navigation/TallyRoute.kt` con `sealed class
  TallyRoute(val valor: String)` → `Resumen` / `Gastos` / `Escanear` (patrón
  `sealed class` de AGENTS.md; NO usar el API type-safe con
  kotlinx-serialization). Navegación de bottom bar estándar: `launchSingleTop`,
  `restoreState` y `popUpTo(startDestination) { saveState = true }`.
- **Bottom bar reutilizable**: `TallyBottomBar.kt` en `:feature:common` (es
  "composable reutilizable"). Recibe `destinoActivo: BottomBarDestino` y
  `onNavigate: (BottomBarDestino) -> Unit` — sin `NavController`. El enum
  `BottomBarDestino` (Resumen/Gastos/Escanear con icono, label y testTag) vive
  en `:feature:common`; `:app` lo mapea 1:1 con `TallyRoute`. Dibuja el mockup
  02: fila de 80dp sobre `surface-container-low`; item activo = icono dentro de
  pill (`secondary-container`/`on-secondary-container`, forma pill) + label
  semibold + tinte `primary`; inactivo = `on-surface-variant`; área táctil
  ≥44dp.
- **Iconos**: `androidx.compose.material:material-icons-extended` gestionado
  por el Compose BOM existente (sin versión literal). Alternativa aceptable:
  3 vector drawables propios en `:feature:common`; registrar la elección en
  `PROGRESS.md`.
- **Placeholders**: mantener `ResumenScreen`; crear `GastosScreen.kt` y
  `EscanearScreen.kt` (Box con superficie del tema + título en español), cada
  uno con `Modifier.testTag` (`pantalla_resumen`, `pantalla_gastos`,
  `pantalla_escanear`) para el test instrumentado. Sin ViewModel ni `XState`.
- **Módulos nuevos — REQUIERE autorización del usuario (Open Question 1)**:
  recomendado `:feature:gastos` y `:feature:escanear` (patrón idéntico a
  `:feature:resumen`; registrar en `settings.gradle.kts` y en `:app`). Si no se
  autorizan, variante B: los dos placeholders viven en `:feature:common`
  (`PlaceholderScreen(titulo, testTag)`), `:app` los invoca desde el `NavHost` y
  se omiten las tareas de creación de módulos.
- **Test instrumentado de navegación** en `:app/src/androidTest/`:
  `NavigationTest.kt` con `createAndroidComposeRule<MainActivity>()` — arranca
  en Resumen, toca Gastos y Escanear en la bottom bar (por testTag/texto),
  afirma que cada placeholder se muestra y que el pill activo cambia; sin
  crash. Añadir a `:app` `testInstrumentationRunner` (patrón de
  `framework/ai`) + `ui-test-junit4` / `ui-test-manifest` (BOM). Con
  `@HiltAndroidApp`/`@AndroidEntryPoint` de producción no hacen falta
  sustituciones Hilt.
- Sin cambios en `init.sh`, `domain`, `framework` ni tema.

## Expected File Changes

- `gradle/libs.versions.toml` — modify; `navigation-compose` (versión estable
  coherente, solo aquí), `androidx-compose-ui-test-junit4` /
  `androidx-compose-ui-test-manifest` (BOM) e iconos según decisión.
- `app/build.gradle.kts` — modify; `navigation-compose`,
  `testInstrumentationRunner` y deps `androidTest`.
- `app/src/main/java/com/robertomr/tally/MainActivity.kt` — modify;
  `setContent` → `TallyTheme { TallyApp() }`.
- `app/src/main/java/com/robertomr/tally/navigation/TallyRoute.kt` — create;
  `sealed class` de rutas.
- `app/src/main/java/com/robertomr/tally/navigation/TallyApp.kt` — create;
  `Scaffold` + `NavHost` + wiring de la bottom bar (mapeo
  `BottomBarDestino` ↔ `TallyRoute`).
- `feature/common/src/main/java/com/robertomr/tally/feature/common/TallyBottomBar.kt`
  (+ `BottomBarDestino`) — create; bottom bar del mockup 02.
- `feature/common/build.gradle.kts` — modify solo si los iconos se resuelven
  desde `common`.
- `:feature:gastos/`, `:feature:escanear/` — create (decisión A):
  `build.gradle.kts` (patrón de `:feature:resumen`) + `GastosScreen.kt` /
  `EscanearScreen.kt`; registrar en `settings.gradle.kts` y `app/build.gradle.kts`.
- `feature/resumen/src/main/java/.../ResumenScreen.kt` — modify; `testTag` en
  el placeholder.
- `app/src/androidTest/java/.../NavigationTest.kt` — create; E2E de navegación.
- `docs/architecture.md` — update (decisión A); diagrama con los módulos nuevos.
- `PROGRESS.md`, `feature_list.json` — update al completar (evidencia).

## Visual Design Impact

- UI involved: yes.
- Design source: `DESIGN.md` + `docs/design/stitch/02-resumen-principal/` y
  `docs/design/stitch/03-listado-gastos/` (bottom bar; el HTML es referencia de
  estructura, se traduce a Compose + Material 3, nunca se copia literal).
- Screens or states affected: bottom bar compartida (nueva) + placeholders de
  Resumen (existente), Gastos y Escanear (nuevos). Estados: item activo/inactivo.
- New design artifact required: no — `DESIGN.md` y los mockups cubren la barra.

## Durable Documentation Impact

- `docs/architecture.md`: update (decisión A) — el diagrama de módulos gana
  `:feature:gastos` y `:feature:escanear`. Con variante B: not needed.
- `ARCHITECTURE.md` (raíz): not needed — la arquitectura operativa vive en
  `AGENTS.md`/`docs/architecture.md` (mismo criterio que `bootstrap-gradle`).
- `CONSTRAINTS.md`: not needed — las reglas de navegación ya están en
  `AGENTS.md`; no se introducen MUST/MUST NOT nuevos.
- `AGENTS.md`: not needed — no cambia el workflow ni la ruta de arranque.
- Other docs: `PROGRESS.md` y `feature_list.json` — update obligatorio con
  evidencia al completar. `DESIGN.md` y ADRs sin cambios.

## Implementation Plan

1. Confirmar con el usuario la decisión de módulos (Open Question 1) y el
   criterio de dispositivo (Open Question 2).
2. Añadir al catálogo `navigation-compose` (+ iconos / ui-test según decisión).
3. Crear placeholders Gastos/Escanear (decisión A o variante B) y `testTag` en
   `ResumenScreen`.
4. Implementar `TallyRoute` (sealed class) y `TallyApp` (NavHost + Scaffold) en
   `:app`; conectar `MainActivity`.
5. Implementar `TallyBottomBar` + `BottomBarDestino` en `:feature:common` según
   mockup 02 y cablear callbacks + estado activo.
6. Ajustar navegación estándar de bottom bar y back; crear `NavigationTest`
   instrumentado en `:app`.
7. Ejecutar la verificación completa (comandos + manual en emulador + revisión
   de código).
8. Actualizar `docs/architecture.md` (si aplica), `PROGRESS.md` y
   `feature_list.json` con evidencia.

## Implementation Tasks

- [x] T1 Decisión de módulos (A/B) y de dispositivo confirmada por el usuario; resultado registrado.
- [x] T2 Catálogo: `navigation-compose` (+ iconos / ui-test) sin versiones literales fuera del toml.
- [x] T3 Placeholders `GastosScreen`/`EscanearScreen` (A o B) + `testTag` en `ResumenScreen`.
- [x] T4 `TallyRoute` sealed class + `TallyApp` (Scaffold + NavHost, start = Resumen) en `:app`; `MainActivity` conectado.
- [x] T5 `TallyBottomBar` + `BottomBarDestino` en `:feature:common` (pill activo, 80dp, ≥44dp táctil, callbacks sin NavController).
- [x] T6 Navegación bottom bar estándar (launchSingleTop/restoreState/popUpTo start) + back correcto (escenario 4).
- [x] T7 `NavigationTest` instrumentado en `:app` (3 destinos, pill activo, sin crash).
- [x] T8 Verificación en verde: comandos + manual en `emulator-5554` + revisión de código (escenario 3).
- [x] T9 `docs/architecture.md` (si aplica) + `PROGRESS.md` + `feature_list.json` con evidencia.

## Verification Plan

- `gradlew.bat assembleDebug` → BUILD SUCCESSFUL.
- `gradlew.bat testDebugUnitTest` → BUILD SUCCESSFUL (regresión).
- `gradlew.bat :test:unit:test` → BUILD SUCCESSFUL (regresión).
- `ANDROID_SERIAL=emulator-5554 gradlew.bat :app:connectedDebugAndroidTest`
  → `NavigationTest` en verde. E2E persistente: el repo ya usa
  `connectedDebugAndroidTest` (`:framework:ai`) y esta feature cambia
  navegación/rutas visibles al usuario, así que procede cobertura E2E
  instrumentada enfocada (1 fichero, sin UI fickle más allá de la bottom bar).
- Manual en `emulator-5554` (`gradlew.bat :app:installDebug`): abrir la app,
  navegar los 3 destinos con la bottom bar, comprobar pill activo y back del
  sistema; capturas de pantalla como evidencia.
- Revisión de código: rutas `sealed class` en `:app`; ninguna pantalla de
  `:feature:*` recibe `NavController`; Select-String de versiones literales
  (`x.y.z`, `version="0-9`, GAV literal) en `**/*.gradle.kts` → 0 coincidencias.
- `init.sh`: sin cambios (`git diff init.sh` vacío). Su contenido
  (`assembleDebug` + `testDebugUnitTest`) ya se ejecuta arriba; nunca procesos
  largos. (Esta máquina no invoca `bash`; se verifica el diff.)
- Criterio "ambos móviles" solo si el usuario lo confirma: `gradlew.bat
  :app:installDebug` + navegación manual en Tab S9+ y realme GT 2; si se
  enmienda a emulador (precedente `bootstrap-gradle`), dejarlo constado en
  `feature_list.json`/`PROGRESS.md`.
  → **Resuelto 2026-10-13 (sesión 008)**: criterio enmendado a emulador
  `emulator-5554` siguiendo el precedente de `bootstrap-gradle` (decisión del
  usuario 2026-09-30); los móviles físicos siguen reservados a `captura-foto`
  y features de Gemma. Constado en `feature_list.json` y `PROGRESS.md`.

## Implementation Findings (sesión 008)

- Versión de `navigation-compose`: elegida **2.9.8** (última estable de la línea
  compatible con compileSdk 36; la 2.10.x arrastra `lifecycle 2.11.0` que exige
  compileSdk 37). Solo en `gradle/libs.versions.toml` (Open Question 3 cerrada).
- Espresso se fija a **3.7.0** en el catálogo: el `3.5.0` transitivo de
  `ui-test-junit4` falla al inyectar eventos en API 35+
  (`NoSuchMethodException: InputManager.getInstance`).
- Iconos de la bottom bar: `material-icons-extended` gestionado por el Compose
  BOM (alternativa del Technical Approach; sin versión literal).
- Hallazgo del E2E (corregido en `NavigationTest`): tras el segundo back,
  `isFinishing` no es síncrono en el mismo `runOnUiThread`
  (`finishAfterTransition`/predictive back en API 35+ aplazan `finish()`); el
  test espera activamente al cierre. El comportamiento real del back se
  verificó a mano con `adb` en `emulator-5554` (back → Resumen; segundo back →
  la actividad termina y el launcher toma foco; el proceso queda cacheado,
  comportamiento normal de Android).

## Evidence To Capture

- Salidas BUILD SUCCESSFUL de los tres comandos Gradle y del
  `connectedDebugAndroidTest` (nombre del test y dispositivo).
- Capturas de los 3 destinos con pill activo en `docs/evidence/`
  (`navegacion-base-*.png`).
- Resultado de la revisión de código (sealed class en `:app`, pantallas sin
  `NavController`).
- Entradas actualizadas: `feature_list.json` (`evidence` de `navegacion-base`)
  y `PROGRESS.md` (sesión correspondiente).

## Validator Checklist

(Ítems marcados `[x]` = trabajo del implementador auto-verificado; los de juicio
—alcance, escenarios y trabajo no relacionado— quedan para la validación
independiente de `feature-validator`.)

- [ ] Implementation stays within this feature's scope.
- [ ] Acceptance scenarios pass.
- [x] Verification evidence is present.
- [x] Persistent E2E coverage added (`NavigationTest` en `:app` vía
      `connectedDebugAndroidTest`) o justificación alternativa registrada.
- [x] `feature_list.json` y `PROGRESS.md` fueron actualizados correctamente.
- [ ] No unrelated product behavior or extra feature work was added.
- [x] Ninguna versión literal fuera de `gradle/libs.versions.toml`.
- [x] Ninguna pantalla de `:feature:*` recibe `NavController`.
- [x] `init.sh`, `AGENTS.md`, `docs/architecture.md` (salvo el update previsto),
      ADRs y docs de descubrimiento sin cambios indebidos.

## Open Questions

1. **Módulos nuevos (bloqueante hasta confirmar en T1)**: AGENTS.md prohíbe
   crear módulos Gradle sin preguntar. Recomendación: crear `:feature:gastos`
   y `:feature:escanear` (trayectoria coherente con `:feature:resumen`, que ya
   es un placeholder que crecerá). Alternativa sin módulos nuevos: variante B
   (placeholders en `:feature:common`). El implementador NO debe crear módulos
   sin confirmación expresa del usuario.
2. **Criterio de dispositivo**: `feature_list.json` dice "en ambos móviles",
   pero `bootstrap-gradle` enmendó ese criterio a emulador (decisión del
   usuario 2026-09-30; los móviles quedaron reservados para `captura-foto` y
   el spike). Confirmar si el emulador `emulator-5554` valida esta feature
   (recomendado por consistencia) o se exige instalación física manual.
3. **Versión de `navigation-compose`**: elegir la versión estable coherente con
   AGP 9.2.1 / Kotlin 2.2.10 / compileSdk 36 y declararla solo en
   `gradle/libs.versions.toml`. Si la resolución falla, reportar en vez de
   cambiar de API de navegación ni romper la regla de versiones.
