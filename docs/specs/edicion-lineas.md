# Feature Implementation Spec: Edición completa de líneas con total recalculado

## Source Feature

- `id`: edicion-lineas
- `area`: revision
- `title`: Edición completa de líneas con total recalculado
- `depends_on`: [revision-datos] (accepted 2026-10-17) → satisfecha
- `status`: not_started (a fecha de planificación, 2026-10-17)
- `source`: `feature_list.json`
- `verification` (fuente):
  1. Añadir/quitar/editar líneas persiste al guardar cambios.
  2. El total del ticket no cambia al editar líneas salvo guardado explícito.
  3. Unit tests de la lógica de líneas (sumas, cantidad ≥ 1).
- `notes` (fuente): regla total vs líneas de `docs/domain-model.md`. Decisión del
  usuario: añadir/quitar líneas SÍ está en el MVP.

## Goal

Completar la edición del ticket digitalizado: sobre la pantalla ya aceptada de
`revision-datos` (mockup 07, cabecera editable + líneas en solo lectura), el
usuario ahora **añade, elimina y edita** líneas (concepto, cantidad, precio
unitario) y ve en el footer el **"Total recalculado"** (Σ cantidad × precio) como
**referencia**. Al pulsar "Guardar cambios" se persiste el agregado con las
líneas EDITADAS por `TicketRepository.save` (`@Transaction` ya existente, que
reemplaza el conjunto de líneas por ticket). El **total del ticket sigue siendo
el dato autoritativo**: editarlo es una acción explícita del usuario (campo
"Total (con IVA)", ya existente) y NUNCA se sobrescribe solo porque cambien las
líneas (`docs/domain-model.md`, "Total vs líneas": suma de líneas = referencia,
sin error ni sobrescritura automática).

## Non-Goals

- **"Usar total calculado" / sobrescritura automática del total**: no se
  implementa (no está en el mockup 08 y `DESIGN.md` prohíbe inventar). El total
  solo cambia si el usuario edita explícitamente el campo "Total (con IVA)"
  (patrón ya aceptado en `revision-datos`). Ver U1.
- **Desglose base/IVA**: prohibido por `DESIGN.md` ("NO implementar desglose de
  base imponible/IVA"). El "IVA incl. (2,25 €)" del mockup 08 es decoración
  ficticia → NO se copia.
- **Nota personal** ("Nota adicional (Opcional)" del mockup 08): non-goal
  (igual que en `revision-datos`; se persiste `nota = null`).
- **Sugerencias "Frecuentes" de comercio**, chips sugeridos, edición de hora,
  `DatePicker`/`TimePicker` completos: no.
- **Rutas nuevas en `:app`, estado/pantalla separada (mockup 08 aparte),
  módulos Gradle nuevos, dependencias nuevas, `init.sh`, `build-logic/`, ADRs**:
  no (ver Technical Approach).
- **Editar un ticket YA guardado / detalle / borrado** (`detalle-ticket`,
  `listado-gastos`): fuera de scope; aquí solo se edita el Borrador en el
  subflujo de escaneo.
- **Reintento de extracción / cambio de motor**: ya cubierto por
  `procesado-ui` (`reintento-otro-motor` es otra feature).

## Job Story

When reviso el ticket digitalizado y la extracción trajo líneas incompletas,
mal o de más,
I want añadir, borrar y corregir concepto/cantidad/precio de cada línea y ver
cuánto suman como referencia,
so I can dejar las líneas fieles al papel sin que se me cambie el total real
del ticket que ya había corregido.

## Users And Permissions

- Usuario único (sin cuentas ni permisos Android nuevos; esta feature no añade
  red ni OCR). El Badge de motor (DESIGN.md) se mantiene visible.
- El subflujo sigue dentro de `:feature:escanear`, sin rutas nuevas; las
  pantallas no reciben el `NavController` (AGENTS.md).

## Acceptance Scenarios

### Scenario 1: añadir/editar/eliminar líneas persiste al guardar cambios (verificación 1)

Given un Borrador en revisión con (p. ej.) 2 líneas extraídas
When el usuario añade una línea (concepto/cantidad/precio), edita el precio y la
cantidad de otra, elimina una tercera y pulsa "Guardar cambios"
Then se persiste UN `Ticket` cuyo conjunto de líneas es exactamente el editado
(la línea nueva con sus valores, la editada corregida, la eliminada ausente); la
escritura multi-tabla la cubre `TicketDao.upsert @Transaction` (reemplazo del
conjunto). Observable por `TicketRepository.observeByDateRange`.

### Scenario 2: el total del ticket no cambia al editar líneas (verificación 2)

Given un Borrador en revisión con total `T` y líneas cuya suma ≠ `T`
When el usuario añade/quita/edita líneas (sin tocar el campo "Total (con IVA)")
Then el footer "Total recalculado" refleja la nueva suma de líneas, pero el
`Ticket.total` persistido sigue siendo `T` (la edición de líneas no lo toca).
Solo si el usuario edita EXPLÍCITAMENTE el campo "Total (con IVA)" y guarda,
cambia el total persistido.

### Scenario 3: unit tests de la lógica de líneas (verificación 3)

Given `LineaEditable` (concepto, cantidad, precioUnitario) y las funciones puras
de dominio
When se calcula `totalRecalculado`, se normalizan líneas con cantidad < 1 o
precio < 0 y se filtran líneas vacías
Then `totalRecalculado` = Σ cantidad × precioUnitario (con 0 líneas = 0.0),
la cantidad queda ≥ 1, el precio ≥ 0 y las líneas sin concepto se descartan al
mapear a `Linea`; todo testeable en JVM sin Android.

### Scenario 4: una línea inválida no rompe la pantalla

Given una línea con cantidad o precio escritos como texto inválido ("abc", "0",
"-3")
When el usuario teclea
Then el valor inválido se ignora (se conserva el último valor válido / ≥ 1 /
≥ 0), la pantalla sigue viva y "Guardar cambios" sigue disponible; no hay crash
ni cierre del subflujo.

### Scenario 5: extracción vacía → añadir líneas a mano → guardar

Given una extracción vacía (0 líneas; OCR blank en el E2E determinista)
When el usuario abre la revisión, añade líneas con el editor y guarda
Then se persiste el ticket con las líneas introducidas a mano; el editor
funciona igual con 0 líneas iniciales.

### Scenario 6: "Descartar" no escribe y regresión del subflujo

Given la pantalla de edición con cambios sin guardar
When el usuario pulsa "Descartar" (o back)
Then no se escribe nada en Room (Borrador intacto, la foto sigue en disco) y se
vuelve al resultado del procesado; `RevisionDatosFlujoTest`, `ProcesadoFlujoTest`,
`CapturaFlujoTest` y `NavigationTest` siguen verdes y no hay rutas nuevas en
`:app`.

## Repository Research

### Files Inspected

- `domain/expenses/.../entities/Linea.kt` — `Linea(concepto: String, cantidad:
  Int = 1, precioUnitario: Double)`; NO tiene importe calculado (el importe de
  línea se deriva como `cantidad × precioUnitario`).
- `domain/expenses/.../entities/Ticket.kt` — `Ticket` con `total: Double`
  (autoritativo) y `lineas: List<Linea>`; KDoc ya dice que el total puede no
  coincidir con la suma de líneas.
- `domain/expenses/.../entities/DatosRevision.kt` — holder de la cabecera
  corregida (`comercio`, `fechaEmision`, `total`, `categoria`) + función pura
  `datosRevisionInicial(extraido, hoy)` (defaults: fecha null → hoy, total null →
  0.0). Punto de extensión para las líneas editables.
- `domain/expenses/.../entities/TicketExtraido.kt` — `lineas: List<Linea>` (puede
  ser vacía); `motor` registrado.
- `domain/expenses/.../entities/TicketExtraidoParser.kt` — schema/validación de
  líneas del motor: `concepto` no-blank, `cantidad` entera ≥ 1 (omitida = 1),
  `precio_unitario` numérico ≥ 0. Referencia de las reglas a replicar en la
  edición manual (más permisiva).
- `domain/expenses/.../usecases/GuardarTicket.kt` — hoy mapea a
  `Ticket(..., lineas = extraido.lineas)` (líneas sin editar). Aquí debe pasar a
  persistir las líneas EDITADas.
- `domain/expenses/.../data/TicketRepository.kt` — `save(ticket): Long`,
  `getById`, `observeByDateRange` (`Flow`).
- `framework/database/.../dao/TicketDao.kt` — `@Transaction upsert(ticket,
  lineas)` que **borra y reinserta el conjunto de líneas** (una edición reutiliza
  `save` sin dejar líneas viejas); ya aceptado (`persistencia-room`).
- `feature/escanear/.../EscanearViewModel.kt` — `CapturaUiState.RevisionDatos(
  rutaFoto, textoCrudo, extraido, datos, errorGuardado)`, `datosRevision:
  MutableStateFlow<DatosRevision?>`, `actualizarDatos`, `OrdenGuardado` (con
  `Guardar(intento)` para reintentos), `guardadoDe(...)` que llama a
  `GuardarTicket`; `runCatchingLanzandoCancelacion` re-lanza
  `CancellationException`. Punto de extensión: eventos de líneas + `datos.lineas`.
- `feature/escanear/.../RevisionDatosContent.kt` — pantalla 07 (recibo, campos
  editables comercio/fecha/total, chips de las 6 categorías, `LineasSoloLectura`
  (línea 402, a sustituir por el editor), dock con `revision_guardar`); helpers
  `formatoImporte`/`cifrasTabulares`/`iconoCategoria`/`etiquetaCategoria`.
- `feature/escanear/.../ProcesadoContent.kt` — `etiquetaCategoria`,
  `formatoImporte`, `camposExtraidos`, `etiquetaMotor` (reutilizables).
- `feature/escanear/.../EscanearScreen.kt` — `when (estado)` + `BackHandler`s;
  `RevisionDatos` → `RevisionDatosContent`; `Guardado` → `onGastoGuardado()` +
  `onGuardadoVisto()` (R-REV3).
- `feature/escanear/.../EscanearState.kt` — estado puramente visual (solo copy de
  permiso); no se espera tocar (DEFAULT).
- `app/.../navigation/TallyApp.kt` — 3 rutas; `EscanearScreen(onGastoGuardado)`;
  sin rutas de detalle.
- `app/src/androidTest/.../captura/RevisionDatosFlujoTest.kt` — E2E determinista
  (cámara virtual → OCR blank → extracción vacía → revisión manual → Guardar →
  `pantalla_gastos`), helpers `capturarFoto`/`esperarTag`/`concederPermisoCamara`.
- `docs/design/stitch/08-edicion-ticket/code.html` — mockup 08: cabecera
  Descartar/Guardar cambios; cards de datos + "Desglose de Artículos" (filas
  concepto/cantidad/precio con borrar), "Añadir producto o línea" (borde
  discontinuo); footer "Total recalculado" + "Calculado a partir de N artículos"
  + dock Descartar/Guardar cambios.
- `docs/domain-model.md` — regla "Total vs líneas" (total autoritativo; suma de
  líneas como referencia; sin error ni sobrescritura) y "Cantidad ilegible → 1
  por defecto"; `CONTEXT.md` (Línea: concepto/cantidad/precio final, editable);
  `DESIGN.md` (Editor de líneas, chip de categoría activo, dock en edición,
  prohibición de decoración ficticia e IVA desglosado).
- `domain/expenses/src/test/.../GuardarTicketTest.kt` y
  `feature/escanear/src/test/.../EscanearViewModelTest.kt` — patrones de test
  (dobles de `:test:unit`, Turbine, `CoroutineTestRule`).
- No inspeccionado: `docs/design/stitch/08-edicion-ticket/screen.png` (el agente
  no puede ver imágenes; el `code.html` sí se revisó).

### Existing Patterns To Follow

- Subflujo interno de `:feature:escanear` sin rutas nuevas; `:app` solo conoce
  los 3 destinos de la bottom bar.
- Flujo unidireccional: estado baja, eventos suben como lambdas; VM sin Compose;
  estado derivado con operadores sobre `MutableStateFlow`; `CancellationException`
  siempre re-lanzada.
- Casos de uso de dominio `@Inject constructor` + `operator fun invoke`; la
  lógica de negocio (sumas/validación/mapeo) es PURA y testeable en JVM; el VM
  solo orquesta.
- TestTags `revision_*`; E2E con `createAndroidComposeRule<MainActivity>()`;
  dobles de `:test:unit`; versiones solo en `gradle/libs.versions.toml`.

### Current Gaps

- `Linea` no tiene identidad de fila editable → hace falta un modelo de edición
  con id estable (Compose `key`) y funciones puras de suma/validación.
- `DatosRevision` no lleva líneas; `GuardarTicket` persiste `extraido.lineas` tal
  cual (las líneas editadas aún no se pueden guardar).
- `RevisionDatosContent` muestra las líneas en solo lectura
  (`LineasSoloLectura`) y no tiene footer "Total recalculado" ni "Añadir línea".
- No existe `totalRecalculado` ni validación/normalización de líneas editadas.

## Technical Approach

1. **Modelo de edición de líneas (dominio, puro)** — nuevo `LineaEditable(id:
   Long, concepto: String, cantidad: Int, precioUnitario: Double)` en
   `domain/expenses/entities/` (el `id` es un correlativo de cliente para keyear
   filas en Compose y direccionar ediciones/borrado; NO se persiste). Funciones
   puras en `domain/expenses/entities/LineasEdicion.kt`:
   - `lineasEditablesIniciales(lineas: List<Linea>): List<LineaEditable>` →
     ids 0..n-1.
   - `nuevaLineaEditable(existentes: List<LineaEditable>): LineaEditable` → id
     `maxId + 1`, concepto "", cantidad 1, precio 0.0.
   - `totalRecalculado(lineas: List<LineaEditable>): Double` =
     `sumOf { cantidad * precioUnitario }` (0 líneas → 0.0).
   - `List<LineaEditable>.aLineas(): List<Linea>` → filtra concepto en blanco,
     acota `cantidad.coerceAtLeast(1)` y `precioUnitario.coerceAtLeast(0.0)`.
   Toda la lógica de sumas/validación vive aquí (testeable en JVM, verificación 3).
2. **Holder de edición**: ampliar `DatosRevision` con `lineas: List<LineaEditable>`
   (sin valor por defecto, para forzar el mapeo explícito) y prefijarlo en
   `datosRevisionInicial(extraido)` con `lineasEditablesIniciales(extraido.lineas)`.
   Justificación: un único holder = una sola fuente de verdad de "lo que el
   usuario está editando" (cabecera + líneas), mínimo cableado y tests JVM
   directos (U2).
3. **Persistencia**: `GuardarTicket` sigue recibiendo `extraido` (para `motor`) +
   `datos`; pasa a construir `lineas = datos.lineas.aLineas()` en lugar de
   `extraido.lineas`. `TicketRepository.save`/`TicketDao.upsert @Transaction` ya
   reemplazan el conjunto de líneas: no se toca Room ni el esquema. El total
   persistido sigue siendo `datos.total` (nunca `totalRecalculado(...)`).
4. **ViewModel**: los eventos de línea mutan `datosRevision` vía `actualizarDatos`
   (que ya limpia un error de guardado previo):
   `onLineaAnadida()`, `onLineaConceptoEditado(id, texto)`,
   `onLineaCantidadEditada(id, texto)` (parsea Int; inválido o < 1 → ignora),
   `onLineaPrecioEditado(id, texto)` (parsea coma/punto; inválido o negativo →
   ignora), `onLineaEliminada(id)`. `onGuardar`/`onVolverAlResultado`/reintento M1
   sin cambios de mecanismo. El valor del footer lo calcula la UI con
   `totalRecalculado(actual.datos.lineas)` (función pura).
5. **UI (misma pantalla/estado; ver U3)**: `RevisionDatosContent` evoluciona al
   mockup 08 dentro del MISMO `CapturaUiState.RevisionDatos` y la MISMA
   `EscanearScreen`:
   - Se conserva el contenedor de recibo con comercio, fecha y chips de categoría
     y el campo "Total (con IVA)" (autoritativo; decisión de `revision-datos`).
   - `LineasSoloLectura` se sustituye por `EditorLineas`: cabecera "Desglose de
     artículos" + contador real "N artículos", filas editables con campos
     concepto/cantidad/precio (`tnum` en el precio) y botón borrar (paleta
     `error`), keyeadas por `LineaEditable.id`; botón "Añadir producto o línea"
     (borde discontinuo, ≥44dp).
   - Dock inferior: "Total recalculado" (etiqueta + "Calculado a partir de N
     artículos" + importe `tnum`) y botones "Descartar" (outlined →
     `onVolverAlResultado`, sin escribir) y "Guardar cambios" (filled →
     `onGuardar`). Sin "IVA incl." ni decoración ficticia.
   - TestTags nuevos: `revision_lineas`, `revision_linea_<id>_concepto|_cantidad|_precio|_borrar`,
     `revision_anadir_linea`, `revision_total_recalculado`, `revision_descartar`;
     se conservan `revision_pantalla`, `revision_guardar`, `revision_categoria_*`,
     `revision_comercio`, `revision_fecha`, `revision_total`, `revision_badge_motor`.
6. **Sin cambios de build**: Hilt/ksp de `:domain:expenses` y `tally.hilt` de
   `:feature:escanear` ya cubren lo nuevo; sin dependencias, módulos ni rutas.

## Expected File Changes

- `domain/expenses/src/main/java/.../entities/LineaEditable.kt` — create; modelo
  de línea editable (`id` de cliente) + helpers (`totalRecalculado`,
  `lineasEditablesIniciales`, `nuevaLineaEditable`, `List<LineaEditable>.aLineas`).
  *(Si el implementador prefiere separar helpers en `LineasEdicion.kt`, lo hace y
  lo registra en notes.)*
- `domain/expenses/src/main/java/.../entities/DatosRevision.kt` — modify; añade
  `lineas: List<LineaEditable>`; `datosRevisionInicial` prefija las líneas.
- `domain/expenses/src/main/java/.../usecases/GuardarTicket.kt` — modify;
  persiste `datos.lineas.aLineas()` (no `extraido.lineas`).
- `domain/expenses/src/test/java/.../entities/LineaEditableTest.kt` — create;
  sumas, cantidad ≥ 1, precio ≥ 0, filtrado de líneas vacías, `nuevaLineaEditable`.
- `domain/expenses/src/test/java/.../usecases/GuardarTicketTest.kt` — modify;
  líneas editadas persistidas, línea vacía descartada, total intacto.
- `feature/escanear/src/main/java/.../RevisionDatosContent.kt` — modify; editor
  de líneas + "Añadir producto o línea" + footer "Total recalculado" + dock
  Descartar/Guardar cambios (mockup 08 sobre la base del 07).
- `feature/escanear/src/main/java/.../EscanearViewModel.kt` — modify; eventos de
  línea y `datos.lineas`; `onGuardar` persiste las líneas editadas.
- `feature/escanear/src/main/java/.../EscanearScreen.kt` — modify; cableado de los
  callbacks nuevos a `RevisionDatosContent`.
- `feature/escanear/src/main/java/.../EscanearState.kt` — modify solo si hace
  falta estado visual (DEFAULT: no tocar).
- `feature/escanear/src/test/java/.../EscanearViewModelTest.kt` — modify; añadir/
  editar/eliminar líneas, total intacto, persistencia de líneas editadas, línea
  inválida no rompe.
- `app/src/androidTest/java/.../captura/RevisionDatosFlujoTest.kt` — modify;
  método E2E enfocado (añadir línea → editar → eliminar → Guardar cambios →
  `pantalla_gastos`), reutilizando los helpers existentes.
- `docs/evidence/edicion-lineas/` — create; JUnit XML, `uiautomator`, capturas.
- `docs/architecture.md` — modify (sección revisión/edición de líneas).
- `PROGRESS.md`, `feature_list.json` — update (evidencia; lo aplica el
  orquestador).

Sin cambios en `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`,
`framework/database/`, `schemas/`, ADRs ni `init.sh`. Sin rutas nuevas en `:app`.

## Visual Design Impact

- UI involved: **sí**. Design source: `docs/design/stitch/08-edicion-ticket/` +
  `docs/design/stitch/07-ticket-generado/` (base ya implementada) + `DESIGN.md`.
- Normativo del mockup 08: contenedor de recibo (surface
  `surface-container-lowest`, acento `primary`), filas de línea editables
  (concepto/cantidad/precio + borrar con paleta `error`), botón "Añadir producto
  o línea" con **borde discontinuo** `primary`, footer con **"Total recalculado"**
  (`tnum`) y dock **"Descartar" (outlined) / "Guardar cambios" (filled pill)**;
  importes SIEMPRE con `tnum`; etiquetas/copy en español; táctil ≥ 44dp.
- **Desviación deliberada y justificada**: se conserva el campo editable
  "Total (con IVA)" de la pantalla 07 (el mockup 08 solo muestra el recalculado)
  porque el total es el dato autoritativo del dominio y `revision-datos` ya lo
  dejó editable (decisión U4). El footer "Total recalculado" es una **referencia**.
- Decoración ficticia PROHIBIDA (DESIGN.md): "Lectura OCR completada",
  "Confianza del 98.4% · Ticket #MC-8942", "VERIFICADO", "NIF", "Operación
  #8294", "HASH", "Tarjeta Débito", "Deducción IRPF", "Subtotal/Cuota IVA",
  "IVA incl. (2,25 €)", "Registro contable", "Almacenamiento cifrado",
  "Tally Vision", "6 artículos" cuando no hay 6 (el contador debe ser REAL).
- Estados: edición con 0..N líneas, edición con una línea inválida tecleada
  (se ignora, sin crash), guardando (breve) y error de guardado con paleta
  `error` (ya existente).

## Durable Documentation Impact

- `docs/architecture.md`: **update** — en "Revisión y guardado (pantalla 07)"
  añadir que la misma pantalla/estado incorpora el editor de líneas (mockup 08):
  `LineaEditable` + `totalRecalculado` (dominio, pura), `DatosRevision.lineas`
  editadas y persistidas por `GuardarTicket` (`TicketDao.upsert @Transaction`
  reemplaza el conjunto); el total del ticket NO se deriva de las líneas (regla
  "Total vs líneas").
- `docs/domain-model.md`: **not needed** — la regla "Total vs líneas" y "cantidad
  ilegible → 1" ya están; esta feature las implementa, no las cambia.
- `CONSTRAINTS.md` / `ARCHITECTURE.md` (raíz): **not needed** — no hay MUST/MUST
  NOT nuevos (ya en `AGENTS.md`/`docs/architecture.md`).
- `AGENTS.md`: **not needed** — no cambia el workflow ni la ruta de arranque.
- `docs/risks-and-open-questions.md`: **update opcional** — nota de que "las
  líneas se editan y el total es autoritativo" queda implementado; sin riesgo
  nuevo.
- `CONSTRAINTS.md`/otros: no.

## Implementation Plan

1. Dominio: `LineaEditable` + helpers puros + `DatosRevision.lineas` +
   `datosRevisionInicial`; `GuardarTicket` persiste las líneas editadas.
2. Tests JVM de dominio: `LineaEditableTest` y `GuardarTicketTest` ampliado.
3. VM: eventos de línea (`onLineaAnadida`, `...Concepto/Cantidad/Precio`,
   `onLineaEliminada`) sobre `datosRevision`; tests de `EscanearViewModelTest`.
4. UI: `RevisionDatosContent` → mockup 08 (editor de líneas, "Añadir",
   footer "Total recalculado", dock Descartar/Guardar cambios);
   `EscanearScreen` cablea los callbacks.
5. E2E enfocado en `RevisionDatosFlujoTest` + regresión completa, búsquedas de
   decoración ficticia/versiones, `docs/architecture.md`, evidencia.

## Implementation Tasks

- [ ] T1 Dominio: `LineaEditable` + `LineasEdicion` helpers
      (`totalRecalculado`, `lineasEditablesIniciales`, `nuevaLineaEditable`,
      `aLineas`) en `domain/expenses/entities/`.
- [ ] T2 Dominio: `DatosRevision.lineas` + `datosRevisionInicial` prefijado +
      `GuardarTicket` persiste las líneas editadas (sin tocar Room).
- [ ] T3 Tests JVM: `LineaEditableTest` (sumas, cantidad ≥ 1, precio ≥ 0,
      filtrado) y `GuardarTicketTest` (líneas editadas/ vacías descartadas/
      total intacto).
- [ ] T4 VM: eventos de línea mutando `datosRevision` (parseo tolerante de
      cantidad/precio; inválido → ignora; `CancellationException` intacta).
- [ ] T5 Tests JVM: `EscanearViewModelTest` (añadir/editar/eliminar, total NO
      cambia al editar líneas, guardado persiste las líneas editadas, línea
      inválida no rompe, extracción vacía → añadir a mano → guardar).
- [ ] T6 UI: `RevisionDatosContent` mockup 08 (editor de líneas con keys por
      `id`, "Añadir producto o línea", footer "Total recalculado", dock
      Descartar/Guardar cambios) + cableado en `EscanearScreen`; sin decoración
      ficticia ni IVA.
- [ ] T7 E2E: método enfocado en `RevisionDatosFlujoTest` (añadir → editar →
      eliminar → Guardar cambios → `pantalla_gastos`) con la cámara virtual
      (OCR blank → extracción vacía, determinista, sin red).
- [ ] T8 Cierre: regresión completa, búsquedas (0 decoración ficticia del
      mockup 08 / 0 versiones literales fuera del toml / `git diff init.sh`
      vacío / `schemas/` sin diff), `docs/architecture.md` y evidencia en
      `docs/evidence/edicion-lineas/`.

## Verification Plan

- `.\gradlew.bat assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL (gate
  estándar; equivalencia de `init.sh` en esta máquina sin `bash`).
- `.\gradlew.bat :test:unit:test --rerun :domain:expenses:test --rerun
  :feature:escanear:testDebugUnitTest --rerun` → BUILD SUCCESSFUL; JUnit XML:
  `LineaEditableTest` (nuevo), `GuardarTicketTest` y `EscanearViewModelTest`
  (verificación 3 + escenarios 1, 2, 4 y 5).
- `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat
  :framework:database:connectedDebugAndroidTest` → regresión (`TicketDaoTest`
  `@Transaction` 5/5, `PhotoFileStoreTest` y `CapturedPhotoProcessorTest`) en
  verde; sin cambio de esquema (`schemas/` sin diff).
- `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat :app:connectedDebugAndroidTest`
  → suite completa en verde (`RevisionDatosFlujoTest` con el método nuevo +
  `ProcesadoFlujoTest` + `CapturaFlujoTest` + `NavigationTest` +
  `ExtraccionNubeRealTest`). Emulador `emulator-5554` con 12G de datos (sesión
  026).
- Focused: `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat
  :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.robertomr.tally.captura.RevisionDatosFlujoTest"`.
- E2E obligatorio (cambia el flujo de usuario y hay harness): método enfocado en
  `RevisionDatosFlujoTest`. NO se usa `@TestInstallIn` de `TextEngine` (rompería
  `ExtraccionNubeRealTest`); con la cámara virtual el OCR es blank → extracción
  vacía → se añaden líneas a mano: determinista y sin red. La **persistencia de
  las líneas editadas** se verifica en JVM (observable con
  `TicketRepository.observeByDateRange`) porque aún no hay listado real
  (`listado-gastos`).
- Físico opcional (DEFAULT Tab S9+ `SM-X810`): captura real de ticket de papel →
  Continuar → Revisar → editar líneas → Guardar cambios → `run-as
  com.robertomr.tally ls -l files/fotos` (foto vinculada) y comprobación de que el
  ticket persiste.
- `init.sh`: sin cambios (`git diff init.sh` vacío); su contenido
  (`assembleDebug` + `testDebugUnitTest`) ya se ejecuta arriba.

## Evidence To Capture

- JUnit XML (JVM + instrumentados) copiados a `docs/evidence/edicion-lineas/`.
- `uiautomator dump` + screencaps de: edición con líneas extraídas, una línea
  nueva añadida, con una línea eliminada y el footer "Total recalculado"; pantalla
  Gastos tras Guardar cambios.
- Comandos citables de las búsquedas: 0 decoración ficticia del mockup 08
  (IVA incl./confianza/VERIFICADO/hash) y 0 versiones literales fuera de
  `gradle/libs.versions.toml`.
- `run-as com.robertomr.tally ls -l files/fotos` si se hace la verificación
  física.
- Todo registrado en `feature_list.json` (`evidence`) y `PROGRESS.md`, con
  dispositivos.

## Riesgos

- **R-EDI1 — Sobrescritura del total**: riesgo de que editar líneas cambie
  `Ticket.total` por accidente. Mitigación: la UI calcula el recalculado con una
  función pura y NUNCA escribe `datos.total`; test JVM explícito (escenario 2).
- **R-EDI2 — Identidad de fila en Compose**: sin id estable, borrar/añadir filas
  puede "migrar" el texto tecleado. Mitigación: `LineaEditable.id` de cliente,
  `key(id)` por fila, direccionamiento por id en los eventos.
- **R-EDI3 — Líneas inválidas**: cantidad/precio no parseables o fuera de rango.
  Mitigación: parseo tolerante (ignora el keystroke inválido) + normalización
  defensiva en `aLineas()` (cantidad ≥ 1, precio ≥ 0) + filtrado de vacías.
- **R-EDI4 — Tests de `revision-datos` desincronizados**: `DatosRevision` y
  `GuardarTicket` cambian; sus tests se actualizan a propósito (comportamiento
  correcto), sin tocar Room.
- **R-EDI5 — `CancellationException`** (finding Low recurrente): los eventos de
  línea son síncronos; el guardado sigue con `runCatchingLanzandoCancelacion`.
- **R-EDI6 — Alcance de "edición"**: no arrastrar edición de tickets guardados
  (`detalle-ticket`), nota, IVA ni "Frecuentes"; quedan como non-goals.

## Open Questions (con DEFAULT)

- **U1 — Actualización explícita del total**: ¿botón "Usar total calculado"? DEFAULT
  = **no** (no está en el mockup 08; `DESIGN.md` prohíbe inventar). El total solo
  cambia si el usuario edita el campo "Total (con IVA)" existente. Alternativa:
  añadir un botón en el footer; requiere justificarlo fuera del mockup.
- **U2 — Dónde viven las líneas editadas**: DEFAULT = ampliar `DatosRevision` con
  `lineas: List<LineaEditable>` (un solo holder). Alternativa: parámetro aparte en
  `GuardarTicket` o `DatosEdicion` nuevo; más plomería, mismo comportamiento.
- **U3 — Pantalla/estado de edición**: DEFAULT = **misma pantalla y mismo estado**
  (`CapturaUiState.RevisionDatos`, sin estado ni ruta nueva): `revision-datos` ya
  decidió (U4) que la pantalla 07 es siempre editable y sin toggle "Editar", así
  que un estado 08 aparte duplicaría la edición de cabecera y añadiría un paso
  redundante. Alternativa: nuevo estado `EdicionLineas`/`EdicionTicket` (mockup 08
  como pantalla propia); se descarta por duplicación salvo que el usuario lo pida.
- **U4 — Línea con concepto en blanco al guardar**: DEFAULT = **descartar** esa
  línea (evita persistir filas vacías; `Linea.concepto` no es opcional). Alternativa
  = persistir con concepto vacío.
- **U5 — Cantidad no entera tecleada** (p. ej. "2,5"): DEFAULT = ignorar el
  keystroke (cantidad es `Int`); alternativa = truncar.
- **U6 — Precio negativo / no numérico**: DEFAULT = ignorar. Alternativa = forzar 0.
- **U7 — Línea nueva por defecto**: DEFAULT = concepto "", cantidad 1, precio 0.0
  (fila vacía lista para escribir). Alternativa = no crearla hasta que haya texto.
- **U8 — Hora de emisión**: DEFAULT = no editable (fuera de scope; la fecha sí,
  como en `revision-datos`).
- **U9 — Dispositivo físico**: DEFAULT = Tab S9+ `SM-X810` (precedente sesiones
  012-013/018/021/026) si se hace verificación física; `emulator-5554` (12G) para
  el E2E.

## Validator Checklist

- [ ] Implementation stays within this feature's scope (sin sobrescritura
      automática del total, sin IVA desglosado, sin nota, sin "Frecuentes", sin
      edición de tickets guardados, sin módulos/rutas/dependencias nuevas).
- [ ] Acceptance scenarios 1-6 pass.
- [ ] Verification evidence is present (JUnit XML + uiautomator/capturas; `run-as`
      si hubo físico).
- [ ] E2E enfocado añadido/actualizado (feature con flujo de usuario y harness
      disponibles); persistencia de líneas editadas cubierta en JVM y justificado.
- [ ] `feature_list.json` y `PROGRESS.md` actualizados correctamente.
- [ ] Lógica de líneas (sumas, cantidad ≥ 1, precio ≥ 0, filtrado) en dominio puro
      y testeada en JVM; `@Transaction` de Room sin cambios (`schemas/` sin diff).
- [ ] `Ticket.total` no se deriva de las líneas; el recalculado es solo referencia.
- [ ] Sin decoración ficticia del mockup 08; 0 versiones literales fuera de
      `gradle/libs.versions.toml`; `init.sh`/`AGENTS.md`/`build-logic/` sin diffs.
- [ ] No unrelated product behavior or extra feature work was added.
