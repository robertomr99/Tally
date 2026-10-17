# Feature Implementation Spec: Revisión del ticket digitalizado y guardado

## Source Feature

- `id`: revision-datos
- `area`: revision
- `title`: Revisión del ticket digitalizado y guardado
- `depends_on`: [procesado-ui, persistencia-room] (ambas `accepted`)
- `status`: not_started (a fecha de planificación, 2026-10-17)
- `source`: `feature_list.json`
- `verification` (fuente):
  1. Extracción vacía → rellenar a mano → guardar → ticket en el listado.
  2. Categoría elegible solo entre las 6 de la enum.
  3. Unit test del guardado (ticket + líneas + ruta de foto, `@Transaction`).
- `notes` (fuente): Mockup 07 (Reintentar/Editar/Guardar). Borrador no persistido;
  la foto ya está en disco (`captura-foto`).

## Goal

Cerrar el ciclo del Borrador: el usuario abre el **ticket digitalizado** (mockup
07) desde el resultado del procesado (ahora el botón "Revisar ticket" está
deshabilitado), corrige a mano **comercio, fecha de emisión, total y categoría**
(chips de la enum fija de 6) sobre la extracción tal cual salió del motor, y
pulsa **Guardar**: se persiste en una única transacción `Ticket` + `Linea`s +
`rutaFoto` + `textoCrudoOcr` con `TicketRepository.save` (Room `@Transaction`,
ya aceptado) y la app vuelve al listado de gastos. Debe funcionar igual cuando la
extracción está vacía (vía manual): rellenar a mano y guardar. Antes de esta
feature no existe ningún mapeo `TicketExtraido` → `Ticket`/`Linea`.

## Non-Goals

- Edición de **líneas** (añadir/borrar/editar concepto/cantidad/precio y "Total
  recalculado"): es `edicion-lineas` (mockup 08). Aquí las líneas se muestran en
  solo lectura tal cual.
- Reintento de la extracción o cambio de motor en la pantalla de revisión: el
  reintento con el **mismo** motor ya existe en el estado de error de
  `procesado-ui`; el otro motor es `reintento-otro-motor` (ADR 0001).
- Editar la **nota** personal ("Añadir nota personal" del mockup): fuera de
  alcance; se persiste `nota = null`.
- Listado real de gastos, período, detalle, borrado/confirmación
  (`listado-gastos`, `periodo-selector`, `detalle-ticket`): no se implementa un
  listado; solo se persiste y se navega a la pestaña Gastos (placeholder).
- Detección de comercios "Frecuentes", sugerencias, edición de la foto original.
- Desglose base/IVA, decoración ficticia del mockup 07 (NIF, "Operación #8294",
  "HASH 8fbc…", "Registro contable", "Tarjeta Débito **** 4812", "Deducción
  IRPF", "Almacenamiento cifrado", "% verificado", "6 ítems detectados"): DESIGN.md
  lo prohíbe.
- Rutas nuevas en `:app`, módulos Gradle nuevos, dependencias nuevas,
  `stateAsResultIn`/`AcScaffold` (no existen), `init.sh`, `build-logic/`, ADRs.
- El borrador persistente tras muerte de la app (riesgo conocido; fuera de scope).

## Job Story

When he revisado el resultado de la extracción y quiero que el ticket quede
guardado con mis correcciones,
I want corregir comercio, fecha, total y categoría y pulsar Guardar,
so I can tener el gasto registrado en la app aunque la extracción fuera
incompleta o el modelo se equivocara.

## Users And Permissions

- Usuario único (sin cuentas ni permisos Android nuevos). Con motor Nube el texto
  crudo ya viajó a Gemini (ADR 0001/0002); esta feature no añade red. El Badge de
  motor (DESIGN.md) se mantiene visible en la pantalla de revisión.

## Acceptance Scenarios

### Scenario 1: extracción vacía → rellenar a mano → guardar (verificación 1)

Given un `TicketExtraido` vacío (OCR blank: comercio "", fecha null, total null,
líneas [], categoría OTROS)
When el usuario abre "Revisar ticket" y rellena comercio, fecha, total y elige
una categoría distinta de OTROS, y pulsa Guardar
Then se persiste UN `Ticket` (comercio y campos corregidos, `fechaCaptura` = ahora,
`motor` = del resultado, `rutaFoto` del Borrador, `textoCrudoOcr` del Borrador,
líneas []) y la app vuelve a la pestaña Gastos; `observeByDateRange` sobre el
rango de la fecha corregida emite ese ticket.

### Scenario 2: guardar con campos vacíos funciona (vía manual)

Given un Borrador sin datos (campos vacíos)
When el usuario pulsa Guardar sin rellenar nada
Then se persiste igualmente (comercio "", fecha propuesta hoy, total 0.0,
categoría OTROS); no hay validación que bloquee el guardado.

### Scenario 3: categoría elegible solo entre las 6 de la enum (verificación 2)

Given la pantalla de revisión
Then solo hay 6 chips de categoría, uno por cada valor de `Categoria.entries`
(Alimentación, Hogar, Transporte, Ocio, Salud, Otros); el activo usa
`secondary-container` + `on-secondary-container` con icono check; no existe
ningún valor fuera de la enum.

### Scenario 4: unit test del guardado con líneas y foto (verificación 3)

Given un `TicketExtraido` con 2 líneas y un Borrador con `rutaFoto` y `textoCrudo`
When se ejecuta el caso de uso de guardado
Then el `Ticket` resultante lleva el id asignado, las 2 líneas, la `rutaFoto`,
el `textoCrudoOcr` y `motor`; la escritura multi-tabla la cubre el
`TicketDao.upsert @Transaction` ya aceptado (regresión `TicketDaoTest`).

### Scenario 5: "Revisar ticket" habilitado y regresión

Given el estado `ExtraccionLista` del procesado (mockup 06)
Then "Revisar ticket" está habilitado y abre la revisión (mockup 07);
`NavigationTest 3/3`, `CapturaFlujoTest` y `ProcesadoFlujoTest` siguen en verde
(actualizando el assert `procesado_revisar` de deshabilitado a habilitado) y no
hay rutas nuevas en `:app`.

### Scenario 6: volver sin guardar preserva el Borrador

Given la pantalla de revisión
When el usuario pulsa back del sistema (o "Volver")
Then no se escribe nada en Room, la foto sigue en disco y se vuelve al resultado
del procesado con el Borrador intacto.

## Repository Research

### Files Inspected

- `feature/escanear/.../EscanearViewModel.kt` — `CapturaUiState`
  (PermisoPendiente/Encuadre/Procesando/Revision/ProcesandoExtraccion/
  ExtraccionLista/ExtraccionFallida/Error), `OrdenExtraccion`
  (Continuar/Reintentar/Cancelar/Volver), cadena declarativa
  `entradaCaptura.flatMapLatest { resultadoDeCaptura }` + `combine(ordenExtraccion)`
  + `flatMapLatest { extraccionDe }`; `runCatchingLanzandoCancelacion`. Punto de
  extensión: añadir estados de revisión/guardado sin romper la cancelación.
- `feature/escanear/.../ProcesadoContent.kt` — `ExtraccionListaContent` con el
  botón "Revisar ticket" `enabled = false` (línea ~185, testTag
  `procesado_revisar`); `etiquetaCategoria`, `formatoImporte`, `camposExtraidos`
  helpers reutilizables.
- `feature/escanear/.../EscanearScreen.kt` — `when (estado)` sobre
  `CapturaUiState`, `BackHandler`s, raíz `pantalla_escanear`; sin parámetros.
- `domain/expenses/.../entities/Ticket.kt` / `Linea.kt` / `Categoria.kt` /
  `Motor.kt` / `TicketExtraido.kt` — contrato del agregado y del resultado del
  motor (comercio String, fechaEmision LocalDateTime?, total Double?, categoria,
  motor; Ticket exige fechaEmision/total no nulos y `fechaCaptura`).
- `domain/expenses/.../data/TicketRepository.kt` — `save(ticket): Long` upsert
  transaccional; `getById`; `observeByDateRange(from, to): Flow<List<Ticket>>`.
- `domain/expenses/.../data/PhotoFileStore.kt` — la foto ya está en disco; el
  guardado solo guarda la **ruta** (no hay que escribirla de nuevo).
- `domain/expenses/.../usecases/ExtraerTicket.kt` — patrón de caso de uso
  (`@Inject constructor`, `operator fun invoke`, `CancellationException` jamás
  tragada).
- `framework/database/.../dao/TicketDao.kt` + schema v1 — `@Transaction upsert`
  con reemplazo del conjunto de líneas; ya aceptado (`persistencia-room`).
- `test/unit/.../FakeTicketDataSource.kt` / `FakePhotoFileStore.kt` — dobles para
  tests JVM de dominio y VM.
- `app/.../navigation/TallyApp.kt` / `TallyRoute.kt` — 3 rutas; `NavHost` solo en
  `:app`; navegación bottom bar con `navigateTo(destino)`; sin rutas de detalle.
- `app/src/androidTest/.../captura/ProcesadoFlujoTest.kt` — assert
  `procesado_revisar` `assertIsNotEnabled` (a actualizar) y patrón de E2E con
  cámara virtual (OCR blank determinista, sin red).
- `feature/gastos/.../GastosScreen.kt` — placeholder `pantalla_gastos` (destino
  de retorno tras Guardar; el listado real es `listado-gastos`).
- `docs/design/stitch/07-ticket-generado/code.html` + `DESIGN.md` (contenedor de
  recibo, chips de categoría, dock Reintentar/Editar/Guardar) +
  `docs/domain-model.md` (transición Guardar, fecha ilegible → hoy, campos
  vacíos no bloquean), `CONTEXT.md` (categoría, línea), ADR 0001/0002.
- No inspeccionado: `07-ticket-generado/screen.png` (el agente no ve imágenes).

### Existing Patterns To Follow

- Subflujo interno de `:feature:escanear` sin rutas nuevas (pantallas 05 y 06);
  `:app` solo conoce los 3 destinos de la bottom bar.
- Flujo unidireccional: estado baja, eventos suben como lambdas; VM sin Compose;
  estado derivado con operadores sobre `MutableStateFlow` (sin `load()`
  imperativa); `runCatchingLanzandoCancelacion` re-lanza `CancellationException`.
- Casos de uso de dominio `@Inject constructor` + `operator fun invoke` (AGENTS.md);
  el VM no inyecta el repositorio directamente (nit conocido de `captura-foto`).
- testTags `revision_*`; E2E con `createAndroidComposeRule<MainActivity>()`;
  dobles de `:test:unit`; versiones solo en `gradle/libs.versions.toml`.

### Current Gaps

- No existe el mapeo `TicketExtraido` → `Ticket`/`Linea` ni ningún caso de uso de
  guardado.
- No existe pantalla de revisión (mockup 07); "Revisar ticket" está `enabled=false`.
- `EscanearScreen` no emite ningún evento hacia `:app` (necesita un callback para
  volver al listado tras Guardar).

## Technical Approach

1. **Dominio**: nuevo caso de uso `GuardarTicket` (`@Inject constructor` de
   `TicketRepository`) con `suspend operator fun invoke(...): Long`. Recibe el
   `TicketExtraido` + los campos corregidos (comercio, `fechaEmision`,
   `total`, `categoria`) + `rutaFoto` + `textoCrudo` y mapea a
   `Ticket(id = 0, comercio, fechaEmision, total, categoria, fechaCaptura = now,
   motor = extraido.motor, rutaFoto, nota = null, textoCrudoOcr = textoCrudo,
   lineas = extraido.lineas)`; devuelve `ticketRepository.save(ticket)`. El mapeo
   es puro y testeable; `@Transaction` lo aporta `TicketDao.upsert` (ya probado).
   DEFAULT de campos ausentes: `total` null → `0.0`, `fechaEmision` null → hoy.
2. **Estado (`EscanearViewModel`)**: nuevos `CapturaUiState.RevisionDatos(rutaFoto,
   textoCrudo, extraido, datos)` (pantalla 07 editable), `Guardando` y
   `Guardado(id)`. El estado de edición (`DatosRevision`: comercio, fecha, total,
   categoría) se prefija desde `TicketExtraido` con `datosRevisionInicial(extraido)`
   (función pura, testeable). Eventos: `onRevisar()`, `onComercioEditado`,
   `onFechaEditada`, `onTotalEditado`, `onCategoriaElegida(Categoria)`,
   `onGuardar()`, `onVolverALaFoto()`, `onGuardadoVisto()`. `Guardar` corre
   `GuardarTicket` con `runCatchingLanzandoCancelacion`; éxito → `Guardado(id)`.
   `onGuardadoVisto()` resetea las entradas → `Encuadre` (cámara limpia).
3. **Navegación tras Guardar (DEFAULT, ver U1/U2)**: **sin rutas nuevas**.
   `EscanearScreen(onGastoGuardado: () -> Unit)`; al observar `Guardado` lanza el
   callback y `onGuardadoVisto()`. `TallyApp` pasa
   `onGastoGuardado = { navController.navigateTo(BottomBarDestino.Gastos) }`
   (pestaña Gastos = listado placeholder hasta `listado-gastos`). Justificación:
   conserva el patrón subflujo (pantallas 05/06) y la regla "las pantallas no
   reciben NavController".
4. **UI `RevisionDatosContent.kt` (mockup 07 adaptado, DESIGN.md)**: contenedor de
   recibo (surface `surface-container-lowest`, acento `primary` arriba, separador
   discontinuo antes del total), badge de motor (reutilizar el badge de
   `ProcesadoContent`), campos editables comercio (OutlinedTextField), fecha
   (campo `d/M/yyyy`; DEFAULT texto, sin `DatePicker` completo), total (campo
   numérico con `tnum` y €) y **chips de categoría** (radio-group de 6, activo
   `secondary-container` + check), líneas en solo lectura, y dock con botón
   **Guardar** (filled pill, ≥44dp). Sin decoración ficticia ni desglose IVA.
5. **Procesado**: `ExtraccionListaContent` recibe `onRevisar` y habilita
   "Revisar ticket" (`testTag procesado_revisar`).
6. **Sin** cambios de build (Hilt/ksp de `:domain:expenses` y `tally.hilt` de
   `:feature:escanear` ya cubren `GuardarTicket`), sin dependencias ni módulos.

## Expected File Changes

- `domain/expenses/src/main/java/.../usecases/GuardarTicket.kt` — create; mapeo
  `TicketExtraido`→`Ticket` + `TicketRepository.save`.
- `domain/expenses/src/main/java/.../entities/DatosRevision.kt` — create (holder
  de los campos corregidos: comercio, fechaEmision, total, categoria); alternativo
  si el implementador prefiere parámetros sueltos, registrarlo en notes.
- `domain/expenses/src/test/java/.../usecases/GuardarTicketTest.kt` — create;
  escenarios 1, 2 y 4 con `TicketRepository(FakeTicketDataSource, FakePhotoFileStore)`.
- `feature/escanear/src/main/java/.../RevisionDatosContent.kt` — create; mockup 07
  (recibo, campos editables, chips de categoría, badge de motor, dock Guardar).
- `feature/escanear/src/main/java/.../EscanearViewModel.kt` — modify; estados
  `RevisionDatos`/`Guardando`/`Guardado`, `DatosRevision` + `datosRevisionInicial`,
  eventos y `GuardarTicket`.
- `feature/escanear/src/main/java/.../EscanearScreen.kt` — modify; render de los
  estados nuevos + `onGastoGuardado` + `LaunchedEffect` de cierre.
- `feature/escanear/src/main/java/.../ProcesadoContent.kt` — modify;
  `ExtraccionListaContent(onRevisar)` y botón habilitado.
- `feature/escanear/src/main/java/.../EscanearState.kt` — modify solo si hace falta
  estado visual (DEFAULT: no tocar).
- `feature/escanear/src/test/java/.../EscanearViewModelTest.kt` — modify;
  revisar→editar categoría→guardar, campos vacíos, volver sin guardar, mapeo.
- `app/src/main/java/.../navigation/TallyApp.kt` — modify; callback
  `onGastoGuardado` → pestaña Gastos (sin ruta nueva).
- `app/src/androidTest/java/.../captura/ProcesadoFlujoTest.kt` — modify;
  `procesado_revisar` habilitado.
- `app/src/androidTest/java/.../captura/RevisionDatosFlujoTest.kt` — create; E2E
  enfocado (resultado → Revisar → elegir categoría → Guardar → pestaña Gastos).
- `docs/evidence/revision-datos/` — create; JUnit XML, `uiautomator`, `run-as`.
- `docs/architecture.md` — update (sección revisión/guardado).
- `PROGRESS.md`, `feature_list.json` — update (evidencia; lo aplica el orquestador).

## Visual Design Impact

- UI involved: sí. Design source: `docs/design/stitch/07-ticket-generado/` +
  `DESIGN.md` + `stitch-design-system.md` (tokens ya en `:feature:common`).
- Normativo del mockup 07: contenedor de recibo (blanco, borde serrado, acento
  `primary`, línea punteada antes del total), cabecera de comercio con icono
  `storefront`, **chips de categoría** (activo `secondary-container`), filas de
  línea (concepto ×cantidad + importe con `tnum`), total destacado con `tnum` y
  dock inferior con **Guardar** (filled pill). Badge de motor presente.
- Decoración ficticia PROHIBIDA (DESIGN.md Do's and Don'ts): NIF, dirección,
  "Operación #8294", "Deducción IRPF", "Subtotal imponible"/"Cuota IVA",
  "HASH", "Registro contable Tally", "Tarjeta Débito", "Almacenamiento cifrado",
  "% verificado", "6 ítems detectados", "Tally Vision".
- Estados: revisión editable, guardando (breve) y vuelta al listado. Copy en
  español; errores de guardado con la paleta `error`.

## Durable Documentation Impact

- `docs/architecture.md`: update — nueva sección "Revisión y guardado (pantalla
  07)": el subflujo de Escanear gana el estado de revisión; `GuardarTicket`
  (dominio) mapea `TicketExtraido`→`Ticket` y persiste por `TicketRepository.save`
  (`@Transaction`, ya existente); la foto ya está en disco y se vincula por ruta.
- `docs/domain-model.md`: not needed — la transición "Guardar → Guardado" ya está
  descrita; no cambia la fuente de verdad.
- `CONSTRAINTS.md` / `ARCHITECTURE.md` (raíz): not needed — no hay MUST/MUST NOT
  nuevos; la arquitectura operativa vive en `AGENTS.md`/`docs/architecture.md`.
- `AGENTS.md`: not needed — no cambia el workflow ni la ruta de arranque.
- `docs/risks-and-open-questions.md`: update opcional — nota de que "ticket en el
  listado" queda como persistencia observable hasta `listado-gastos`.
- `feature_list.json` y `PROGRESS.md`: update (evidencia; lo aplica el orquestador).

## Implementation Plan

1. Dominio: `DatosRevision` (o parámetros) + `GuardarTicket` + `GuardarTicketTest`.
2. VM: `RevisionDatos`/`Guardando`/`Guardado`, `datosRevisionInicial`, eventos,
   cableado de `GuardarTicket`; tests JVM ampliados.
3. UI `RevisionDatosContent.kt` (mockup 07) + `ProcesadoContent`/`EscanearScreen`
   + `TallyApp` (callback).
4. E2E enfocado `RevisionDatosFlujoTest` + actualización de `ProcesadoFlujoTest`.
5. Cierre: regresión completa, búsquedas (decoración ficticia/versiones),
   `docs/architecture.md`, evidencia.

## Implementation Tasks

- [x] T1 Dominio: holder de campos corregidos + `GuardarTicket` (mapeo y
      `TicketRepository.save`) + `GuardarTicketTest` (vacío, con líneas, ruta foto).
- [x] T2 VM: `CapturaUiState.RevisionDatos`/`Guardando`/`Guardado`,
      `datosRevisionInicial(extraido)` (fecha hoy, total 0.0), eventos
      (`onRevisar`, edición de campos, `onCategoriaElegida`, `onGuardar`,
      `onVolverALaFoto`, `onGuardadoVisto`); `CancellationException` re-lanzada.
- [x] T3 UI `RevisionDatosContent.kt` (mockup 07 normativo): recibo, badge de
      motor, campos editables comercio/fecha/total, chips de las 6 categorías,
      líneas solo lectura, Guardar; sin decoración ficticia.
- [x] T4 Cableado: `ProcesadoContent` habilita "Revisar ticket" con `onRevisar`;
      `EscanearScreen` renderiza los estados nuevos y emite `onGastoGuardado`;
      `TallyApp` navega a la pestaña Gastos (sin ruta nueva); back = volver sin
      guardar.
- [x] T5 Tests JVM: `EscanearViewModelTest` (revisar→editar→guardar, campos
      vacíos, categoría de la enum, volver sin guardar sin escribir en Room) +
      `GuardarTicketTest`.
- [x] T6 E2E `RevisionDatosFlujoTest` (resultado → Revisar → chip de categoría →
      Guardar → `pantalla_gastos`) + `ProcesadoFlujoTest` (`procesado_revisar`
      habilitado).
- [x] T7 Cierre: `assembleDebug testDebugUnitTest` + suite instrumentada; búsquedas
      (0 decoración ficticia del mockup 07, 0 versiones literales fuera del toml,
      `git diff init.sh` vacío, `schemas/` sin diff); `docs/architecture.md`;
      evidencia en `docs/evidence/revision-datos/`.

## Verification Plan

- `.\gradlew.bat assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL (gate
  estándar; equivalencia de `init.sh` en esta máquina sin `bash`).
- `.\gradlew.bat :test:unit:test --rerun :domain:expenses:test --rerun
  :feature:escanear:testDebugUnitTest --rerun` → BUILD SUCCESSFUL; JUnit XML:
  `GuardarTicketTest` (mapeo, líneas, ruta foto, campos vacíos) y
  `EscanearViewModelTest` ampliado.
- `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat
  :framework:database:connectedDebugAndroidTest` → `TicketDaoTest` 5/5
  (regresión del `@Transaction`, verificación 3) y resto en verde.
- `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat :app:connectedDebugAndroidTest`
  → suite completa en verde (`RevisionDatosFlujoTest` + `ProcesadoFlujoTest`
  actualizado + `CapturaFlujoTest` + `NavigationTest` + `ExtraccionNubeRealTest`).
- Focused: `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat
  :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.robertomr.tally.captura.RevisionDatosFlujoTest"`.
- Físico opcional (DEFAULT Tab S9+ `SM-X810`): captura real de ticket de papel →
  Continuar → Revisar → corregir → Guardar → `run-as com.robertomr.tally ls -l
  files/fotos` (foto vinculada) y confirmación de que el ticket persiste.
- E2E obligatorio (cambia el flujo de usuario y hay harness): `RevisionDatosFlujoTest`.
  No se usa `@TestInstallIn` de `TextEngine` (rompería `ExtraccionNubeRealTest`);
  con la cámara virtual el OCR es blank → extracción vacía → revisión manual: el
  flujo es determinista y sin red.
- `init.sh`: sin cambios (`git diff init.sh` vacío); su contenido
  (`assembleDebug` + `testDebugUnitTest`) ya se ejecuta arriba.

## Evidence To Capture

- JUnit XML (JVM + instrumentados) copiados a `docs/evidence/revision-datos/`.
- `uiautomator dump` + screencaps de: revisión con campos extraídos, revisión con
  campos vacíos rellenados a mano, chips de categoría y pestaña Gastos tras
  Guardar.
- `run-as com.robertomr.tally ls -l files/fotos` (la foto sigue en disco y queda
  vinculada al ticket) si se hace la verificación física.
- Búsqueda sin decoración ficticia del mockup 07 ni versiones literales fuera de
  `gradle/libs.versions.toml` (comandos citables).
- Todo registrado en `feature_list.json` (evidence) y `PROGRESS.md`, con
  dispositivos.

## Riesgos

- **R-REV1 — "ticket en el listado" sin listado real**: hasta `listado-gastos` la
  prueba se apoya en `observeByDateRange` (JVM) + navegación a `pantalla_gastos`
  (E2E); no se inventa un listado ni se adelanta su scope.
- **R-REV2 — Mapeo con campos ausentes**: `total`/`fecha` null en la extracción;
  DEFAULT no bloquea (0.0 / hoy) para cumplir "campos vacíos"; cubierto por test.
- **R-REV3 — Reset del subflujo tras Guardar**: navegar dejando el Borrador vivo
  mostaría la revisión al volver a Escanear; el VM debe resetear a `Encuadre`
  (`onGuardadoVisto`), cubierto por test del VM.
- **R-REV4 — `CancellationException`** (finding Low recurrente): el guardado es
  suspend; ningún `runCatching` plano que la convierta en estado de error.
- **R-REV5 — Alcance de "Editar"**: riesgo de arrastrar `edicion-lineas`; queda
  explícito como no-goal (líneas en solo lectura).

## Open Questions (con DEFAULT)

- **U1 — Navegación tras Guardar**: "vuelve al listado". DEFAULT = sin rutas
  nuevas; callback `EscanearScreen(onGastoGuardado)` → `:app` navega a la pestaña
  Gastos y el subflujo resetea a Encuadre. Alternativa: destinos `revision`/
  `guardado` en `:app` (rompe el patrón subflujo; no recomendado).
- **U2 — Verificable sin `listado-gastos`**: DEFAULT = el ticket queda persistido
  y observable por `TicketRepository.observeByDateRange` (test JVM) + E2E que
  comprueba la navegación a `pantalla_gastos`; no se adelanta el listado real.
- **U3 — Alcance de "Editar"**: DEFAULT = solo cabecera (comercio/fecha/total/
  categoría); líneas en solo lectura (`edicion-lineas`, mockup 08).
- **U4 — Modo edición**: DEFAULT = campos siempre editables en la pantalla de
  revisión (sin toggle "Editar" separado, que sería un estado redundante).
- **U5 — "Reintentar" en la revisión**: DEFAULT = no se implementa (ya existe el
  reintento mismo-motor en el error de `procesado-ui`; otro motor =
  `reintento-otro-motor`). No-goal.
- **U6 — Total ausente**: DEFAULT = `0.0` (no bloquea el guardado). Alternativa:
  bloquear y exigir total (contradice "campos vacíos" de la verificación 1).
- **U7 — Fecha ausente/ilegible**: DEFAULT = proponer hoy (domain-model).
- **U8 — "Ver foto original" / "Añadir nota"**: DEFAULT = no-goal de esta feature.
- **U9 — Dispositivo físico**: DEFAULT = Tab S9+ `SM-X810` (precedente sesiones
  012-013/018/021) si se hace la verificación física; `emulator-5554` para el E2E.

## Repair y Validación (2026-10-17)

- **Validación 1 (`feature-validator`)**: veredicto `revise` por un defecto real
  **M1** — reintentar "Guardar" tras un fallo era un no-op silencioso (la orden
  `OrdenGuardado.Guardar` no cambiaba y `MutableStateFlow` conflaciona valores
  iguales, así que el flujo interior no se reiniciaba). Hallazgos Low: L1 (sin test
  de `onFechaEditada`/`onTotalEditado`), L2 (nombre de test desincronizado en
  `ProcesadoFlujoTest`), L3 (sin evidencia visual de UI del mockup 07), L4 (el
  evento se llama `onVolverAlResultado`, más correcto que el `onVolverALaFoto` del
  spec), L5 (`Guardando` bloquea el back).
- **Repair aplicado**:
  - **M1**: `OrdenGuardado.Guardar` pasa de `object` a `data class Guardar(val intento: Int)`
    con un contador (`intentoGuardado`); `onGuardar()` reintenta aunque no se edite
    ningún campo. Test nuevo `reintentarTrasFalloGuardaSinEditarNadaPersisteElTicket`.
  - **L1**: test nuevo `editarFechaYTotalActualizaLaCabeceraEIgnoraTextoInvalido`.
  - **L2**: `ProcesadoFlujoTest` renombrado a `...ConRevisarHabilitado` (+ comentario).
  - **L3**: evidencia visual capturada en `emulator-5554` (sin nube: OCR blank →
    extracción vacía → revisión manual): `mockup07-revision.png` +
    `mockup07-revision-uia.xml` (cabecera + chips de las 6 categorías + Guardar) y
    `mockup07-tras-guardar-gastos.png` + `mockup07-tras-guardar-uia.xml` (pestaña
    Gastos tras Guardar). `run-as` no aplica en emulador (foto vinculada; la
    persistencia la cubren `TicketDaoTest` + `GuardarTicketTest`).
  - L4/L5 no bloqueantes: L4 es una mejora semántica (el spec citaba un nombre
    provisional); L5 se acepta por lo breve del guardado.
- **Re-verificación**: `EscanearViewModelTest` **23/23** (0 fallos); `assembleDebug
  testDebugUnitTest` verde; E2E `:app` **9/9** (`RevisionDatosFlujoTest` 1/1 +
  `ProcesadoFlujoTest` 2/2 renombrado + `CapturaFlujoTest` 3/3 + `NavigationTest`
  3/3). Nota operativa: el emulador se quedó sin espacio (APK debug 180 MB); se
  redimensionó su partición de datos (6G→12G, `-wipe-data`, autorizado por el
  usuario) para poder re-ejecutar el E2E.

## Validator Checklist

- [x] Implementation stays within this feature's scope (sin edición de líneas, sin
      reintento/cambio de motor, sin listado real, sin módulos/rutas/dependencias
      nuevas).
- [x] Acceptance scenarios 1-6 pass.
- [x] Verification evidence is present (JUnit XML + capturas/uiautomator; `run-as`
      si hubo físico).
- [x] E2E enfocado `RevisionDatosFlujoTest` añadido/actualizado (feature con flujo
      de usuario y harness disponibles).
- [x] `feature_list.json` y `PROGRESS.md` actualizados correctamente.
- [x] Mapeo `TicketExtraido`→`Ticket`/`Linea` en dominio (no en la UI) y
      `@Transaction` sin cambios (regresión `TicketDaoTest`).
- [x] Sin decoración ficticia del mockup 07; 0 versiones literales fuera de
      `gradle/libs.versions.toml`; `init.sh`/`AGENTS.md`/`build-logic/` sin diffs.
- [x] No unrelated product behavior or extra feature work was added.

### Validator Verdict (ronda 2, 2026-10-17)

- Veredicto: **accept** (feature-validator). Validación 1 = `revise` (M1); tras el
  repair, la ronda 2 confirma **M1 resuelto** (probe empírico: al neutralizar el fix
  el test `reintentarTrasFalloGuardaSinEditarNadaPersisteElTicket` falla) y regenera
  los gates en verde (`EscanearViewModelTest` 23/23, `GuardarTicketTest` 5/5,
  `:framework:database` 22/22, `:app` 9/9). L1/L2/L3 resueltos.
- Findings Low residuales no bloqueantes: L4 (nombre de evento `onVolverAlResultado`
  vs el `onVolverALaFoto` citado en el spec; el del código es más correcto), L5
  (`Guardando` bloquea el back) y fragilidad de orden pre-existente en
  `CapturaFlujoTest.denegadoMuestraEstadoInAppSinCrash` (SKIP si otra clase ya
  concedió CAMERA; no es falso PASS).
