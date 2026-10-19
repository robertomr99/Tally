# Feature Implementation Spec: Listado de tickets del período

## Source Feature

- `id`: listado-gastos
- `area`: seguimiento
- `depends_on`: periodo-selector (`accepted`); resumen-total y grafica-donut también `accepted` (ya en `:feature:resumen`).
- `status`: not_started (al planificar)
- `source`: `feature_list.json` (user_visible_behavior: "Debajo de la gráfica (y en la pestaña Gastos) se listan los tickets del período con comercio, fecha, total y categoría; al tocar se abre el detalle"); mockup `docs/design/stitch/03-listado-gastos/code.html`.
- `detalle-ticket` (not_started) NO se implementa aquí; solo se deja el punto de entrada (D2).

## Goal

1. Pestaña **Gastos** (hoy placeholder `GastosScreen`): listado cronológico de los tickets del período activo, agrupado por día (cabecera con fecha y total del día, como mockup 03), con `LazyColumn`.
2. Panel **Resumen**: bajo la tarjeta de distribución, sección "Gastos del periodo" con los últimos tickets (máx. 5) y botón "Ver todos" hacia la pestaña Gastos.
3. Cada fila muestra comercio, fecha/hora, categoría y total; se actualiza sola al guardar/borrar (Flow de Room vía `ObservarTicketsDelPeriodo`).
4. Tocar una fila invoca `onTicketClick(ticketId)`; `:app` navega a un destino mínimo `ticket/{ticketId}` (D2).
5. Estado vacío coherente con el donut.

## Non-Goals

- Detalle real del ticket, edición, borrado, foto (`detalle-ticket`). Solo un placeholder en `:app`.
- Búsqueda, chips de filtro por categoría, botón "tune" y "Visualizador de vistas M3" del mockup 03 (decoración de demo o interacción no cubierta por esta feature). Rango personalizado.
- Insignias "verified", "Auto-extraído", "Revisado", "Manual" (no hay dato en el modelo con esa semántica; `motor` no equivale), avatar/perfil de la cabecera, "Modo Contable Activo".
- Paginación, swipe-to-delete, animaciones, módulos Gradle nuevos, cambios en Room/DAO/esquema, `build-logic/`, dependencias nuevas, tema oscuro.

## Job Story

Cuando reviso mis gastos del mes,
quiero ver la lista de tickets con comercio, fecha, categoría e importe,
para localizar un gasto concreto y abrirlo.

## Acceptance Scenarios

### Scenario 1: Listado refleja exactamente el período (dominio)
Given tickets con fechas de emisión dentro y fuera del `Periodo` (incluidos los bordes `desde` 00:00 y `hasta` 23:59)
When se observa el listado
Then contiene exactamente los de dentro, ordenados por `fechaEmision` descendente (empate: `id` descendente), sin duplicados.

### Scenario 2: Agrupación por día
Given tickets el 24-oct (14:32, 11:15) y el 23-oct (20:45)
Then 2 grupos en orden descendente de fecha; cada grupo lleva sus tickets en orden descendente de hora y `totalCentimos` = suma de sus tickets (mismo redondeo HALF_UP a céntimos que la distribución; total <= 0 o no finito suma 0 pero el ticket SÍ aparece).

### Scenario 3: Fila de ticket
Then cada fila muestra: icono de categoría en círculo 40 dp, comercio (1 línea, elipsis; si está en blanco, "Sin comercio"), hora `HH:mm` y chip con `etiquetaCategoria`, y a la derecha el total `1.428,50 €` (`tnum`, bold). Si la hora es 00:00 no se muestra hora (fecha sin hora). La fila entera es clicable (>= 48 dp de alto) con `onClickLabel` accesible.

### Scenario 4: Reactividad
Given la pestaña Gastos (o Resumen) abierta
When se guarda un ticket cuya fecha cae en el período
Then aparece solo (grupo del día correcto, orden correcto, total del día actualizado) sin acción manual; al alternar Día/Semana/Mes cambia el contenido.

### Scenario 5: Estado vacío coherente
Given un período sin tickets
Then en Gastos: icono `receipt_long`, título "Aún no hay tickets en este periodo", texto de apoyo y botón "Escanear ticket" (`onEscanear`); en Resumen: la sección muestra "Sin tickets en este periodo." (sin botón) y el donut muestra `donut_vacio`. Ambos estados aparecen siempre a la vez: la condición del listado vacío es `tickets.isEmpty()` y la del donut `distribucion.estaVacia`; el único caso en que difieren (solo tickets con total <= 0) muestra la lista con sus filas y el donut vacío, y se documenta como esperado.

### Scenario 6: Navegar al tocar
When se toca una fila (en Resumen o en Gastos)
Then se abre el destino `ticket/{id}` con el id del ticket tocado, sin crash; Atrás vuelve a la pestaña de origen con el mismo período y posición de scroll aproximada (el estado de `rememberLazyListState` se restaura con `restoreState`).

### Scenario 7: Resumen con límite y "Ver todos"
Given 8 tickets en el período
Then Resumen pinta 5 filas (las más recientes) y un botón "Ver todos (8)" que lleva a la pestaña Gastos; con <= 5 no hay botón. Resumen sigue siendo una única `Column` con `verticalScroll` sin `LazyColumn` anidado.

### Scenario 8: Rendimiento y scroll
Then Gastos usa una sola `LazyColumn` con `key` estable (`"d_<fecha>"`, `"t_<id>"`) y `contentType`; el selector de período va en la propia lista como primer `item` o en una `Column` superior no desplazable con la lista en `Modifier.weight(1f)`; nunca `LazyColumn` dentro de `verticalScroll`. Con 500 tickets sembrados el scroll no lanza excepciones (E2E opcional).

## Repository Research

### Files Inspected
- Docs: `AGENTS.md`, `CLAUDE.md`, `feature_list.json` (listado-gastos, detalle-ticket), `DESIGN.md` (menciones a listado y mapa de mockups), `docs/build-brief.md` (líneas 34, 55), `docs/specs/grafica-donut.md` (completa; patrón de spec y decisiones D1-D7 vigentes). `docs/specs/periodo-selector.md` y `resumen-total.md` NO releídos en esta sesión: se asume lo que consta en el código y en `grafica-donut.md`. `CONTEXT.md` y `docs/domain-model.md` no releídos; la entidad `Ticket` se tomó del código.
- Mockup 03 (`code.html` completo): TopAppBar, buscador, chips, switcher de demo, secciones por día ("Hoy, 24 de Octubre" / "Ayer" / "Lunes, 21 de Octubre") con "Total: X €", filas `p-3.5` en tarjeta `surface-container-lowest` `rounded-xl` borde `outline-variant/40` con `divide-y`, avatar circular 40 dp, comercio `title-sm` semibold, hora + punto + chip de categoría, importe `title-md` bold; estado vacío con marco OCR, `headline-sm`, texto y botón primario pill "Escanear ticket"; bottom bar con Gastos activo.
- Dominio: `entities/Ticket.kt` (`id`, `comercio`, `fechaEmision: LocalDateTime`, `total: Double`, `categoria`), `entities/Periodo.kt` (rango inclusivo en `LocalDate`), `usecases/ObservarTicketsDelPeriodo.kt` ("total, donut y listado lo reutilizan"), `ObtenerPeriodo.kt` (`Clock` inyectado), carpeta `usecases/` (`CalcularDistribucionCategorias`, `ObservarDistribucionDelPeriodo` ya existen).
- Datos: `framework/database/.../TicketDao.kt` línea 53: la query ya ordena por `fecha_emision DESC`; `FakeTicketDataSource` en `:test:unit` implementa `observeByDateRange`.
- Presentación: `feature/resumen/.../ResumenScreen.kt` (`Column` + `verticalScroll`, `TarjetaTotal`, `TarjetaDistribucion`, sin parámetros de navegación salvo `onAbrirAjustes`), `ResumenViewModel.kt` (`ResumenUiState`, `flatMapLatest` + `combine` + `stateIn`), `PeriodoSelector.kt` (`internal`), `EtiquetaPeriodo.kt` (`formatoImporte`, `etiquetaPeriodo`), `FormatoResumen.kt` (`EstiloTabular`, `textoTickets`), `TarjetaDistribucion.kt` (copy vacío "Aún no hay gastos en este periodo.", cabecera), `build.gradle.kts` (ya depende de `:feature:common` y `:domain:expenses`); `feature/common/.../CategoriaUi.kt` (`etiquetaCategoria`, `iconoCategoria`, `colorCategoria`, públicos), `common/build.gradle.kts`; `feature/gastos/.../GastosScreen.kt` (placeholder sin VM, tag `pantalla_gastos`) y `gastos/build.gradle.kts` (sin deps de proyecto ni Hilt aún).
- Navegación: `app/.../navigation/TallyApp.kt` (un `NavHost`, start Resumen, `navigateTo` con `popUpTo/saveState/restoreState`, `onGastoGuardado` ya navega a Gastos), `TallyRoute.kt` (sealed class de rutas string; Ajustes como pantalla secundaria sin pestaña). `app/src/androidTest/...` (patrón de siembra de `GraficaDonutFlujoTest`/`ResumenTotalFlujoTest`; `NavigationTest` usa el placeholder de Gastos: no se leyó, ver Riesgos).

### Current Gaps
- No existe modelo de listado ni agrupación por día; `GastosScreen` es placeholder; no hay ruta de detalle; `formatoImporte`/`EstiloTabular`/`PeriodoSelector` son `internal` en `:feature:resumen` y `:feature:gastos` no puede usarlos (los features no se conocen).

## Technical Approach

**Dominio (`:domain:expenses`, Kotlin puro).**
- `entities/GrupoDiaTickets.kt`: `data class GrupoDiaTickets(fecha: LocalDate, tickets: List<Ticket>, totalCentimos: Long)`.
- `usecases/AgruparTicketsPorDia.kt` (`@Inject constructor()`, `invoke(tickets: List<Ticket>): List<GrupoDiaTickets>`): ordena por `fechaEmision` desc y `id` desc, agrupa por `fechaEmision.toLocalDate()` conservando el orden, suma céntimos con la MISMA conversión que `CalcularDistribucionCategorias` (si hoy es `private`, extraerla a una función `internal` compartida del paquete; sin duplicarla).
- `usecases/ObservarListadoDelPeriodo.kt` (`@Inject constructor(observarTickets: ObservarTicketsDelPeriodo, agrupar: AgruparTicketsPorDia)`): `invoke(periodo): Flow<List<GrupoDiaTickets>> = observarTickets(periodo).map(agrupar::invoke)`. Sin repositorio ni DAO nuevos.

**Reubicación mínima de UI compartida (sin módulo nuevo).** A `:feature:common`: `PeriodoSelector` (pasa a público; tags `periodo_*` sin cambios), `formatoImporte` y `EstiloTabular` (nuevo `FormatoUi.kt`), y los composables `FilaTicket`/`CabeceraDia` (nuevo `ListadoTicketsUi.kt`, parámetros primitivos/dominio, sin ViewModel). `:feature:resumen` pasa a importarlos; `etiquetaPeriodo` y el resto de `FormatoResumen` permanecen en resumen salvo que gastos los necesite. Los tests existentes siguen verdes (solo cambia el paquete de importación).

**`:feature:gastos`.** Pasa a ser feature completa (AGENTS.md): `GastosScreen.kt` (`hiltViewModel()`, `collectAsStateWithLifecycle`, parámetros `onTicketClick: (Long) -> Unit`, `onEscanear: () -> Unit`), `GastosViewModel.kt` (`@HiltViewModel`; `tipoSeleccionado: MutableStateFlow(MES)` → `flatMapLatest { obtenerPeriodo; observarListado }` → `stateIn`, igual que `ResumenViewModel`; sin `load()`), `GastosState.kt` (`rememberGastosState()` con `LazyListState`). `GastosUiState(cargando, tipo, periodo, grupos: List<GrupoDiaTickets>?, hoy: LocalDate)`; `hoy` sale de `ObtenerPeriodo(DIA).desde` (evita inyectar `Clock` en el VM). Dependencias en `gastos/build.gradle.kts`: plugin `tally.hilt`, `:domain:expenses`, `:feature:common`, `libs.hilt.navigation.compose`, `libs.androidx.lifecycle.runtime.compose`, iconos extendidos, y de test `:test:unit`, junit, turbine, coroutines-test (mismas entradas del catálogo que resumen; sin versiones literales). Es un cambio de módulo existente, no un módulo nuevo.

**`:feature:resumen`.** `ResumenUiState` gana `recientes: List<Ticket>? = null` y `totalTickets: Int = 0` (primeros 5 del orden de dominio, sin agrupar), calculados en el `combine` del VM con un caso de uso `ObservarTicketsDelPeriodo` ya existente más el orden de `AgruparTicketsPorDia`; alternativa aceptable: `ObservarListadoDelPeriodo` y aplanar. `ResumenScreen` gana `onTicketClick: (Long) -> Unit = {}` y `onVerTodos: () -> Unit = {}`; sección `SeccionRecientes.kt` bajo `TarjetaDistribucion` dentro de la misma `Column` (filas con `forEach`, sin lazy).

**Presentación común.** Etiquetas de día en español (sin `Locale` del dispositivo, como `etiquetaPeriodo`): función pura `etiquetaDia(fecha, hoy)` → "Hoy, 24 de octubre" / "Ayer, 23 de octubre" / "Lunes, 21 de octubre" (año solo si difiere del actual), testeable en JVM. Chip de categoría: fondo `colorCategoria(cat)` al ~12 % alfa con texto del color de categoría (>= 4,5:1 comprobable con el test de contraste existente; si no llega, texto `onSurfaceVariant`), nunca solo color: lleva nombre. Fila con `Surface` `rounded-xl` y separadores `outlineVariant` 30 %; tarjeta por grupo `surfaceContainerLowest`. Máx. ancho `widthIn(max = 720.dp)` centrado (tablet, mockup `max-w-3xl`).

**`:app`.** `TallyRoute.Ticket : TallyRoute("ticket/{ticketId}")` con `fun crear(id: Long) = "ticket/$id"`, `navArgument("ticketId") { type = NavType.LongType }`, composable de un `TicketPendienteScreen` mínimo definido en `:app` (texto "Detalle del ticket #<id>", botón Atrás, tag `pantalla_detalle_ticket`; marcado TODO para `detalle-ticket`, que lo reemplaza por la pantalla real manteniendo la ruta). `GastosScreen(onTicketClick = { navController.navigate(TallyRoute.Ticket.crear(it)) }, onEscanear = { navController.navigateTo(BottomBarDestino.Escanear) })`; `ResumenScreen(onTicketClick = ..., onVerTodos = { navController.navigateTo(BottomBarDestino.Gastos) })`. La pestaña activa de la barra mientras se ve `ticket/..` queda en Resumen por el `else` actual (D2). Sin `NavController` en features.

Tags E2E: `lista_gastos`, `grupo_dia_<yyyy-MM-dd>`, `grupo_dia_<fecha>_total`, `ticket_<id>`, `ticket_<id>_comercio|_total|_categoria`, `listado_vacio`, `boton_escanear_vacio`, `resumen_recientes`, `resumen_ticket_<id>`, `boton_ver_todos`, `pantalla_detalle_ticket`.

## Expected File Changes

- `domain/expenses/.../entities/GrupoDiaTickets.kt`, `usecases/AgruparTicketsPorDia.kt`, `usecases/ObservarListadoDelPeriodo.kt` — create (y extracción de la conversión a céntimos si procede, en `usecases/CalcularDistribucionCategorias.kt`).
- `domain/expenses/src/test/.../AgruparTicketsPorDiaTest.kt`, `ObservarListadoDelPeriodoTest.kt` — create (`FakeTicketDataSource`, Turbine).
- `feature/common/.../PeriodoSelector.kt` (movido), `FormatoUi.kt`, `ListadoTicketsUi.kt`, `EtiquetaDia.kt` — create/move; test `EtiquetaDiaTest.kt`. `feature/resumen`: `PeriodoSelector.kt`, `EtiquetaPeriodo.kt` (solo `formatoImporte`) y `FormatoResumen.kt` (`EstiloTabular`) — ajustar imports/mover; `ResumenViewModel.kt`, `ResumenScreen.kt` — modify; `SeccionRecientes.kt` — create; `ResumenViewModelTest.kt` — modify.
- `feature/gastos/...`: `GastosScreen.kt` (reescribir), `GastosViewModel.kt`, `GastosState.kt`, `GastosViewModelTest.kt` — create; `build.gradle.kts` — modify.
- `app/.../navigation/TallyRoute.kt`, `TallyApp.kt` — modify; `TicketPendienteScreen.kt` — create. `app/src/androidTest/.../gastos/ListadoGastosFlujoTest.kt` — create; `NavigationTest.kt` — ajustar solo si afirma el texto del placeholder.
- `gradle/libs.versions.toml`, `settings.gradle.kts`, `build-logic/`: sin cambios. Un `di.kt` por módulo: sin `di.kt` nuevo (casos de uso `@Inject`).

## Visual Design Impact

UI: sí. Fuente: mockup 03 + `DESIGN.md` (rejilla 4 dp para listas densas, `tnum` en importes). Estados: cargando (`grupos == null`: cabecera y selector sin filas, sin spinner), datos, vacío. Cabecera de Gastos: título "Gastos" + `PeriodoSelector` + etiqueta del rango (`etiquetaPeriodo`). Capturas en `docs/evidence/listado-gastos/` (Gastos con 3 días, Resumen con "Ver todos", vacío, tablet si hay Tab).

## Durable Documentation Impact

- `DESIGN.md`: update breve (patrón de fila/grupo del listado y qué se omite del mockup 03). `docs/architecture.md`: update solo si lista casos de uso/rutas. `CONTEXT.md`, `docs/domain-model.md`, `AGENTS.md`, `ARCHITECTURE.md`, `CONSTRAINTS.md`: not needed. `PROGRESS.md` y `feature_list.json`: los actualiza el orquestador/implementador con evidencia.

## Implementation Plan

1. TDD dominio: `AgruparTicketsPorDia`, `ObservarListadoDelPeriodo`.
2. Mover a `:feature:common` el selector y el formato; ejecutar tests de resumen (sin regresión).
3. `EtiquetaDia` + `FilaTicket`/`CabeceraDia`.
4. `GastosViewModel`/`GastosScreen` con `LazyColumn`.
5. Sección de recientes en Resumen.
6. Ruta `ticket/{id}` + placeholder en `:app`; cablear callbacks.
7. E2E, regresión, capturas, evidencia.

## Implementation Tasks

- [ ] T1 `AgruparTicketsPorDia` + test: escenarios 1-2; empate por `id`; mismo día con horas distintas; medianoche (23:59 vs 00:00 del día siguiente); lista vacía; total 0/NaN/negativo (aparece, suma 0); suma de céntimos 0,10+0,20 = 30.
- [ ] T2 `ObservarListadoDelPeriodo` + test Turbine: guardar un ticket lo añade en su grupo; ticket fuera del período no entra; borrar lo quita.
- [ ] T3 Reubicación en `:feature:common` (selector, `formatoImporte`, `EstiloTabular`); `:feature:resumen:testDebugUnitTest` verde.
- [ ] T4 `etiquetaDia` + test (hoy, ayer, día de la semana, otro año, meses en español).
- [ ] T5 `GastosViewModel` + test: alterna tipo, vacío (`grupos` vacío, no `null`), `cargando` solo hasta la primera emisión.
- [ ] T6 `GastosScreen` (LazyColumn, claves, vacío con CTA) y `ResumenViewModel`/`SeccionRecientes` (límite 5, `totalTickets`, "Ver todos (N)") + test del VM.
- [ ] T7 `:app`: ruta, placeholder, callbacks; `NavigationTest` verde.
- [ ] T8 E2E `ListadoGastosFlujoTest` (ver verificación) y `DESIGN.md`.
- [ ] T9 Verificación completa, revisión de reglas, `PROGRESS.md` y `feature_list.json` con evidencia.

## Verification Plan

- `.\gradlew.bat assembleDebug testDebugUnitTest --console=plain` (o `bash init.sh`): verde. Conteo JUnit por clase con `--rerun` en `:domain:expenses:test`, `:feature:resumen:testDebugUnitTest`, `:feature:gastos:testDebugUnitTest`, `:feature:common:testDebugUnitTest`.
- E2E en **emulator-5556** (5554 está offline): `:app:assembleDebug :app:assembleDebugAndroidTest`, instalar ambos APK y `adb -s emulator-5556 shell am instrument -w -e class com.robertomr.tally.gastos.ListadoGastosFlujoTest <paquete>.test/androidx.test.runner.AndroidJUnitRunner`. **Nunca** `connected*`. No leer el token HF.
  - Caso A (semilla): sembrar con el repositorio real >= 3 tickets hoy y 1 ayer/otro mes con comercios únicos (patrón de `GraficaDonutFlujoTest`: línea base, siembra y limpieza en `@After`); en Gastos comprobar por tag que existen exactamente los esperados del período (contraste con lista calculada en el test), orden descendente (posiciones Y), y el total del grupo.
  - Caso B (reactividad): con Gastos abierta, guardar otro ticket y comprobar que aparece sin recargar; alternar Día/Semana/Mes.
  - Caso C (tocar): tocar `ticket_<id>` en Gastos y en Resumen -> `pantalla_detalle_ticket` con el id; Atrás vuelve sin crash.
  - Caso D (vacío): si la línea base del período Día no está vacía, `assumeTrue` y cubrir el vacío en JVM; comprobar `listado_vacio` y `donut_vacio` a la vez cuando proceda.
  - Regresión: `ResumenTotalFlujoTest`, `PeriodoSelectorFlujoTest`, `GraficaDonutFlujoTest`, `NavigationTest`, `RevisionDatosFlujoTest` (el Guardar navega a Gastos); omitir `*RealTest`.
- Revisión: 0 imports de Android en `domain/**`; features sin dependencias entre sí (`gastos` y `resumen` solo dependen de `common` y dominio); sin `NavController` en features; sin módulos ni versiones literales; `git diff` vacío en `build-logic/`, `libs.versions.toml`, `framework/`; sin `LazyColumn` dentro de `verticalScroll`.

## Evidence To Capture

Conteo JUnit por clase; salida de `am instrument` en emulator-5556; capturas (Gastos, Resumen con "Ver todos", vacío); decisiones D1-D6 resueltas en `PROGRESS.md`.

## Validator Checklist

- [ ] Listado = exactamente los tickets del rango, orden fecha desc y grupos por día con total correcto.
- [ ] Reactivo vía Flow, sin `load()`; cambia con el selector.
- [ ] Fila con comercio, fecha/hora, categoría (texto + icono) y total con `tnum`.
- [ ] Vacío coherente con el donut; sin crash con lista vacía/`null`.
- [ ] Tocar abre `ticket/{id}` sin crash; Atrás vuelve; sin invadir `detalle-ticket`.
- [ ] Sin scroll anidado; `LazyColumn` con claves en Gastos.
- [ ] Alcance: sin búsqueda/chips/insignias; sin módulos nuevos ni versiones literales; regresión verde.

## Open Decisions (defaults si no hay respuesta)

- **D1 Dónde vive el listado.** Default: ambos sitios. Pestaña Gastos = listado completo del período con su propio `PeriodoSelector` (Mes por defecto) y agrupado por día; Resumen = vista previa de 5 con "Ver todos". Motivo: Resumen es una columna con scroll (un `LazyColumn` anidado se rompe) y el mockup 03 es una pantalla propia con bottom bar en Gastos. Limitación aceptada: el período elegido en Resumen NO se arrastra a Gastos (cada pantalla tiene su selección, ambas arrancan en Mes). Alternativa: pasar el tipo por argumento de ruta opcional (`gastos?periodo=`), más fiel pero toca `navigateTo` y el mapeo de pestaña activa. Alternativa 2: solo Gastos, sin preview.
- **D2 Destino al tocar.** Default: ruta `ticket/{ticketId}` + placeholder en `:app` (`pantalla_detalle_ticket`); `detalle-ticket` lo sustituye. Barra inferior resaltando Resumen mientras se ve el detalle (comportamiento del `else`). Alternativa: no navegar y solo exponer `onTicketClick` (incumple "Navegación a detalle sin crash").
- **D3 Reubicación en `:feature:common`** de `PeriodoSelector`, `formatoImporte`, `EstiloTabular` (refactor sin cambio de comportamiento). Alternativa: duplicarlos en `:feature:gastos` (deuda); no recomendada. Si hay objeción a tocar `:feature:resumen`, confirmar antes.
- **D4 Tickets con total <= 0.** Default: aparecen en el listado (son tickets guardados) con su total tal cual y no suman al total del día/donut.
- **D5 Hora 00:00.** Default: se oculta la hora (se asume fecha sin hora). Alternativa: mostrar siempre `HH:mm`.
- **D6 Límite de la vista previa.** Default 5 filas. Alternativa: 3 o 10.

## Riesgos

- `NavigationTest` y `RevisionDatosFlujoTest` pueden afirmar texto/tag del placeholder de Gastos: mantener el tag `pantalla_gastos` en la raíz del nuevo `GastosScreen`.
- `FakeTicketDataSource` puede no ordenar como Room (DAO ya ordena `fecha_emision DESC`): el orden definitivo lo fija `AgruparTicketsPorDia`.
- El spec de `periodo-selector`/`resumen-total` no se releyó; si fijan tags o contratos de `PeriodoSelector` que cambien al hacerlo público, conservar tags tal cual.
