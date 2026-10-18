# Feature Implementation Spec: Selector de período Día/Semana/Mes con rangos correctos

## Source Feature

- `id`: periodo-selector
- `area`: seguimiento
- `depends_on`: persistencia-room (`accepted`)
- `status`: not_started (al planificar)
- `source`: `feature_list.json`
- Bloquea (features posteriores, NO se implementan aquí): `resumen-total`, `grafica-donut`, `listado-gastos`, `rango-personalizado`, y por transitividad `seed-demo`.

## Goal

Dar al panel (pestaña Resumen) un selector Día / Semana / Mes (segmented control del mockup 02) y la lógica de dominio que convierte la selección en un rango de fechas correcto, filtrado por **fecha de emisión** (no de captura; `docs/domain-model.md`). Entrega el cimiento que reutilizarán las cuatro features siguientes: el modelo `Periodo`, el flujo de tickets del período activo y el estado de selección en el ViewModel de Resumen.

Para que "el total cambie al alternar" sea verificable sin invadir `resumen-total`, la pantalla muestra solo el rango activo (etiqueta tipo `1 Oct – 31 Oct`) y una cifra provisional (suma de totales + nº de tickets) que `resumen-total` sustituirá por la tarjeta completa.

## Non-Goals

- Tarjeta de total con promedio diario, comparativa vs período anterior, formato tnum (`resumen-total`). Tampoco `Periodo.anterior()`.
- Donut, leyenda, desglose por categoría (`grafica-donut`); decisión de paleta de categorías.
- Listado de tickets y navegación a detalle (`listado-gastos`); la pestaña Gastos sigue siendo placeholder.
- Segmento "Personalizado", date range picker (mockup 09) y rango invertido (`rango-personalizado`). `PeriodoTipo` NO incluye `PERSONALIZADO` todavía.
- Refresco automático a medianoche, navegación a períodos pasados (flechas) y persistencia de la selección entre arranques (ver decisiones abiertas).
- Cambios en Room/DAO/esquema: `TicketRepository.observeByDateRange(from, to)` (inclusivo) ya sirve. Sin módulos Gradle nuevos.

## Job Story

Cuando abro el panel para ver cuánto he gastado,
quiero alternar entre Día, Semana y Mes,
para ver solo los tickets cuya fecha de emisión cae en esa ventana.

## Users And Permissions

- Usuario único local, sin cuentas ni permisos. Solo lectura de Room (sin escrituras).

## Acceptance Scenarios

### Scenario 1: Rangos correctos por tipo (dominio)
Given una fecha de referencia "hoy"
When se calcula el `Periodo` de tipo DIA, SEMANA o MES
Then DIA = [hoy, hoy]; SEMANA = lunes a domingo que contiene hoy; MES = día 1 al último día del mes natural (28/29/30/31); `desde` y `hasta` son `LocalDate` inclusivos.

### Scenario 2: Bordes de calendario
Given hoy es un domingo, un lunes, un 31, un 1, un 29-feb de año bisiesto, o una semana que cruza mes/año
When se calcula SEMANA y MES
Then la semana cruza correctamente mes (p. ej. lun 27-abr..dom 3-may-2026) y año; febrero da 28 o 29 días según el año; diciembre/enero no rompen el año.

### Scenario 3: Cambio de hora en España
Given un `Clock` fijo en `Europe/Madrid` el día del cambio de hora (2026-03-29 02:xx→03:xx y 2026-10-25 03:xx→02:xx), incluidos instantes 23:30 y 00:30
When se obtiene "hoy" y el período
Then "hoy" es la fecha local correcta, DIA/SEMANA/MES no se desplazan un día y la semana del cambio sigue siendo lun-dom.

### Scenario 4: El total cambia al alternar
Given tickets sembrados con fecha de emisión hoy, hace 3 días (misma semana o no) y hace 40 días
When el usuario alterna Día → Semana → Mes
Then el recuento y la suma mostrados cambian según el rango; un ticket cuya fecha de emisión está en el rango entra aunque su `fechaCaptura` sea de otro día, y uno capturado hoy con emisión fuera del rango no entra.

### Scenario 5: Selección por defecto y reactividad
Given la pantalla se abre
When no se ha tocado el selector
Then está activo "Mes" (como el mockup 02) con el pill `secondary-container`; al guardar un ticket nuevo del período activo, la cifra se actualiza sola (Flow de Room), sin función `load()`.

## Repository Research

### Files Inspected

- `AGENTS.md`, `PROGRESS.md` (hasta sesión 011), `feature_list.json` (entradas periodo-selector, resumen-total, grafica-donut, listado-gastos, rango-personalizado, seed-demo), `CONTEXT.md` (Período), `docs/domain-model.md`, `docs/build-brief.md`, `DESIGN.md` (selector de período), `docs/design/stitch/02-resumen-principal/code.html` (líneas 147-174).
- `domain/expenses/.../data/TicketRepository.kt` — `observeByDateRange(from: LocalDate, to: LocalDate): Flow<List<Ticket>>`, inclusivo en ambos extremos.
- `domain/expenses/.../entities/Ticket.kt` — `fechaEmision: LocalDateTime`, `total: Double`.
- `framework/database/.../source/TicketLocalDataSource.kt`, `dao/TicketDao.kt`, `Converters.kt` — rango `[from 00:00, to+1 00:00)` codificado como epoch millis con la fecha-hora local interpretada en UTC. No depende de zona ni DST: el cambio de hora solo afecta a calcular "hoy", no a la consulta.
- `test/unit/.../FakeTicketDataSource.kt` — `observeByDateRange` ya filtra por `toLocalDate()` inclusivo y ordena desc; reutilizable.
- `feature/resumen/.../ResumenScreen.kt` — hoy es el emblema de splash sobre fondo `primary` con `onAbrirAjustes` y tags `pantalla_resumen`/`boton_ajustes` (usados por tests existentes). `feature/resumen/build.gradle.kts` — sin Hilt ni dependencia de dominio.
- `feature/ajustes/` (`AjustesViewModel`, `build.gradle.kts`) — patrón a copiar: `@HiltViewModel`, `combine(...).stateIn(viewModelScope, WhileSubscribed(5_000), ...)`, plugin `tally.hilt`, `hilt.navigation.compose`, `lifecycle.runtime.compose`, tests con `:test:unit` + Turbine + `coroutines.test`.
- `app/.../navigation/TallyApp.kt`, `TallyRoute.kt` — Resumen ya es destino y start; `ResumenScreen(onAbrirAjustes)` cableado. `app/.../di/` solo tiene `TextEngineEntryPoint.kt` (no hay `di.kt` en `:app`).

### Existing Patterns To Follow

- Casos de uso: una acción por clase, `@Inject constructor`, `operator fun invoke` en `domain/expenses/usecases/` (ver `ObservarPreferenciaMotor`).
- Pantalla = `XScreen.kt` + `XViewModel.kt` + `XState.kt`; estado como cadena de operadores sobre `MutableStateFlow` de entrada.
- Textos UI y comentarios en español, identificadores en inglés/dominio como `Ticket`, `Categoria`, `Linea` (nombres de dominio en español existentes: `Periodo` según CONTEXT.md).

### Current Gaps

- `stateAsResultIn` y `AcScaffold` que cita AGENTS.md NO existen en el repo (grep sin resultados); `AjustesViewModel` usa `stateIn` directo. No crearlos aquí: seguir el patrón de Ajustes y reflejarlo como desviación conocida.
- No hay `Clock` inyectable ni `di.kt` en `:app`.
- No hay datos sembrados en la app (`seed-demo` es posterior): los tests siembran vía repositorio/fake.
- Mockup 02 usa 4 segmentos; aquí solo 3 (ver decisión D2).

## Technical Approach

**Dominio (`:domain:expenses`, Kotlin puro, `java.time`).**
- `entities/PeriodoTipo.kt`: `enum { DIA, SEMANA, MES }`.
- `entities/Periodo.kt`: `data class Periodo(val tipo: PeriodoTipo, val desde: LocalDate, val hasta: LocalDate)` con `require(desde <= hasta)` y `companion fun de(tipo, referencia: LocalDate)`. Semana: `referencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))` + 6 días (fijo lunes-domingo, no depende de `Locale`). Mes: `YearMonth.from(referencia)` → `atDay(1)`..`atEndOfMonth()`. Sin lógica de "anterior" ni de "personalizado".
- `usecases/ObtenerPeriodo.kt` (`@Inject constructor(clock: Clock)`): `operator fun invoke(tipo: PeriodoTipo): Periodo` = `Periodo.de(tipo, LocalDate.now(clock))`.
- `usecases/ObservarTicketsDelPeriodo.kt` (`@Inject constructor(repository: TicketRepository)`): `operator fun invoke(periodo: Periodo): Flow<List<Ticket>>` = `repository.observeByDateRange(periodo.desde, periodo.hasta)`. Es el punto de entrada que reutilizan `resumen-total`, `grafica-donut`, `listado-gastos` y `rango-personalizado` (este último añadirá un `Periodo` con tipo propio; `Periodo` ya acepta rangos arbitrarios por el constructor).
- `Clock`: `@Provides Clock = Clock.systemDefaultZone()` en un nuevo `app/.../di/di.kt` (módulo `SingletonComponent`). En tests se pasa `Clock.fixed(...)`.

**Presentación (`:feature:resumen`, módulo existente; NO se crea `:feature:panel`).**
- `build.gradle.kts`: añadir `id("tally.hilt")`, `implementation(project(":domain:expenses"))`, `libs.hilt.navigation.compose`, `libs.androidx.lifecycle.runtime.compose` y los `testImplementation` de Ajustes. Versiones solo en `libs.versions.toml` (ya existen todas las entradas; sin versiones literales).
- `ResumenViewModel.kt` (`@HiltViewModel`): `MutableStateFlow<PeriodoTipo>(MES)` de entrada; `uiState = tipo.flatMapLatest { t -> val p = obtenerPeriodo(t); observarTickets(p).map { ResumenUiState(tipo = t, periodo = p, numTickets, totalProvisional) } }.stateIn(...)`. `fun onSeleccionarPeriodo(tipo)` solo escribe el `MutableStateFlow`. `ResumenUiState` lleva `cargando` inicial, `tipo`, `periodo`, `numTickets`, `suma` (Double suma de `total`); campos pensados para ser ampliados por `resumen-total`.
- `ResumenState.kt` + `rememberResumenState()`: estado puramente visual (scroll de la columna).
- `ResumenScreen.kt`: `ResumenScreen(onAbrirAjustes, viewModel = hiltViewModel())`; sustituye el emblema por: TopAppBar plana "Tally" con el botón de Ajustes (mantener tag `boton_ajustes`), `PeriodoSelector` (segmented control en píldora), chip con el rango y cifra provisional. Mantener tag `pantalla_resumen`.
- `PeriodoSelector.kt` (en `:feature:resumen`, `internal`/privado al módulo): `Row` en `surface-container-low` con `RoundedCornerShape(50)`, segmento activo `secondary-container`/`on-secondary-container` con check; inactivos `on-surface-variant`; objetivo táctil ≥ 44 dp; semántica `selected` y tags `periodo_dia`, `periodo_semana`, `periodo_mes`.
- `EtiquetaPeriodo.kt`: función pura `Periodo -> String` en español (`18 Oct`, `12 Oct – 18 Oct`, `1 Oct – 31 Oct`, `Locale("es")`), con test JVM.

Navegación en `:app` no cambia (sin rutas nuevas, sin `NavController` en la feature).

## Expected File Changes

- `domain/expenses/src/main/java/com/robertomr/tally/domain/expenses/entities/PeriodoTipo.kt`, `Periodo.kt` — create.
- `domain/expenses/.../usecases/ObtenerPeriodo.kt`, `ObservarTicketsDelPeriodo.kt` — create.
- `domain/expenses/src/test/.../entities/PeriodoTest.kt`, `usecases/ObtenerPeriodoTest.kt`, `usecases/ObservarTicketsDelPeriodoTest.kt` — create.
- `app/src/main/java/com/robertomr/tally/di/di.kt` — create (provee `Clock`).
- `feature/resumen/build.gradle.kts` — modify.
- `feature/resumen/src/main/java/com/robertomr/tally/feature/resumen/ResumenScreen.kt` — modify; `ResumenViewModel.kt`, `ResumenState.kt`, `PeriodoSelector.kt`, `EtiquetaPeriodo.kt` — create.
- `feature/resumen/src/test/.../ResumenViewModelTest.kt`, `EtiquetaPeriodoTest.kt` — create.
- `app/src/androidTest/.../resumen/PeriodoSelectorFlujoTest.kt` — create (E2E; patrón de `RevisionDatosFlujoTest`/`AjustesMotorFlujoTest`).
- `docs/architecture.md` (si lista casos de uso/módulos), `PROGRESS.md`, `feature_list.json` — update por orquestador/implementador. Los fakes de `:test:unit` no deberían necesitar cambios.

## Visual Design Impact

- UI involved: yes.
- Design source: `DESIGN.md` (componente "Selector de período", elemento activo = `secondary-container`/`on-secondary-container`), `docs/design/stitch/02-resumen-principal/` (`screen.png`, `code.html` líneas 147-174: contenedor `p-1 gap-1.5 rounded-full surface-container-low`, segmentos `py-1.5 px-3 rounded-full label-md`, activo con icono check 14 sp).
- Screens or states affected: Resumen (cabecera, selector, rango, cifra provisional); estado vacío = 0 tickets y 0,00 € sin pantalla especial (el estado vacío formal es de `grafica-donut`/`listado-gastos`).
- New design artifact required: no. Solo el cuerpo del mockup 02 más allá del selector queda para features posteriores.

## Durable Documentation Impact

- `ARCHITECTURE.md`: not needed (no existe; la arquitectura vive en `docs/architecture.md`).
- `CONSTRAINTS.md`: not needed.
- `AGENTS.md`: not needed.
- `docs/architecture.md`: update solo si enumera casos de uso/dependencias de `:feature:resumen` (ahora depende de `:domain:expenses` y Hilt).
- `docs/domain-model.md`/`CONTEXT.md`: not needed (ya definen Período por fecha de emisión); anotar en el spec la regla "semana = lunes-domingo" y, si el usuario lo confirma, añadirla a `CONTEXT.md` > Período.

## Implementation Plan

1. Dominio: `PeriodoTipo`, `Periodo.de`, tests de bordes (primero, TDD).
2. Casos de uso `ObtenerPeriodo` (+ `Clock`) y `ObservarTicketsDelPeriodo` + tests con Turbine y `FakeTicketDataSource`.
3. `di.kt` de `:app` con `Clock`.
4. `:feature:resumen`: build, `ResumenViewModel`, `ResumenState`, `PeriodoSelector`, `EtiquetaPeriodo`, `ResumenScreen`; tests JVM.
5. E2E en `:app`: sembrar tickets y alternar el selector.
6. Regresión (`NavigationTest`, `AjustesModeloFlujoTest`) y evidencia.

## Implementation Tasks

- [ ] T1 `PeriodoTipo` + `Periodo` con `de()`; `PeriodoTest`: DIA; semana lun/dom; semana cruzando mes (27-abr-2026) y año (29-dic-2025 → 4-ene-2026); febrero 2026 (28) y 2028 (29); meses de 30 y 31 días; 31-dic y 1-ene.
- [ ] T2 `ObtenerPeriodo` + `ObtenerPeriodoTest` con `Clock.fixed` en `Europe/Madrid`: 2026-03-29 y 2026-10-25 (00:30, 02:30, 23:30), y noche del 31-dic.
- [ ] T3 `ObservarTicketsDelPeriodo` + test: bordes inclusivos (ticket a las 00:00 del `desde` y a las 23:59 del `hasta` entran; el día siguiente no), emisión vs captura, reactividad al guardar.
- [ ] T4 `di.kt` en `:app` con `@Provides Clock`.
- [ ] T5 Gradle de `:feature:resumen` + `ResumenViewModel` + `ResumenViewModelTest` (por defecto MES; alternar cambia periodo y cifra; `CoroutineTestRule`/dispatcher de test como en `AjustesViewModelTest`).
- [ ] T6 `PeriodoSelector`, `EtiquetaPeriodo` (+ test), `ResumenScreen` con tags y semántica `selected`; conservar `boton_ajustes` y `pantalla_resumen`.
- [ ] T7 E2E `PeriodoSelectorFlujoTest` en `:app` en `emulator-5554`: semilla (hoy, hace 3 días, hace 40 días) vía `TicketRepository`; asserts de recuento/cifra por Día/Semana/Mes; limpieza de los tickets sembrados.
- [ ] T8 Verificación completa, revisión de reglas AGENTS.md, actualizar `PROGRESS.md` y `feature_list.json` (`passing` con evidencia).

## Verification Plan

- `.\gradlew.bat assembleDebug testDebugUnitTest --console=plain` (o `bash init.sh`): verde.
- `.\gradlew.bat :domain:expenses:test --rerun` y `:feature:resumen:testDebugUnitTest --rerun`: `PeriodoTest`, `ObtenerPeriodoTest`, `ObservarTicketsDelPeriodoTest`, `ResumenViewModelTest`, `EtiquetaPeriodoTest` con los bordes de la verificación 1.
- `$env:ANDROID_SERIAL="emulator-5554"; .\gradlew.bat :app:connectedDebugAndroidTest`: `PeriodoSelectorFlujoTest` (verificación 2 con datos sembrados) + regresión de `NavigationTest`, `AjustesModeloFlujoTest`, `AjustesMotorFlujoTest` y flujos de captura que pasan por Resumen.
- Revisión de código: 0 imports de Android en `domain/**`; un `di.kt` por módulo (`:app` gana el suyo; `:feature:resumen` no necesita); 0 versiones literales en `**/*.gradle.kts`; sin Retrofit/OkHttp; sin `NavController` en la feature; sin módulos nuevos; `git diff` vacío en `build-logic/`, ADRs y docs de descubrimiento.
- Captura del emulador (`docs/evidence/periodo-selector-*.png`) de cada segmento; la revisión visual de imágenes queda a revisión humana como en features anteriores.
- `init.sh` sin cambios: sigue ejecutando `assembleDebug` + `testDebugUnitTest` sin lanzar procesos largos.

## Evidence To Capture

- Salida/JUnit XML de los tests de dominio y ViewModel (recuento por clase) y del E2E (`app/build/outputs/androidTest-results/connected/debug/`).
- Capturas de los 3 segmentos y confirmación `uiautomator` del atributo `selected`.
- Nota en `PROGRESS.md` sobre la desviación de `stateAsResultIn`/`AcScaffold` y las decisiones D1-D6 resueltas.

## Validator Checklist

- [ ] Alcance limitado: sin tarjeta de métricas, donut, listado ni "Personalizado".
- [ ] Escenarios 1-5 cumplidos; semana lunes-domingo; rangos inclusivos; filtro por fecha de emisión.
- [ ] Bordes de verificación 1 cubiertos por tests con nombre (mes cruzado, 28/29/30/31, DST de Madrid).
- [ ] Dominio Kotlin puro, `Clock` inyectado (no `LocalDate.now()` suelto), Flow reactivo sin `load()`.
- [ ] `:feature:resumen` no se conoce con otras features; sin módulos Gradle nuevos; versiones solo en catálogo.
- [ ] Tags/botón de Ajustes y tests existentes siguen en verde.
- [ ] E2E presente (o justificación) y evidencia en `feature_list.json`/`PROGRESS.md`.

## Open Decisions (defaults si el usuario no responde)

- D1 Persistir la selección entre arranques: default NO; vive en el ViewModel (sobrevive a rotación) y se abre en Mes. Si se pide, añadir `PreferenciaPeriodoStore` (patrón `PreferenciaMotorStore`/SharedPreferences) en otra feature.
- D2 Segmento "Personalizado": default ausente hasta `rango-personalizado` (evita UI muerta); alternativa: mostrarlo deshabilitado.
- D3 Primer día de semana: default lunes fijo (España); alternativa: según `Locale`.
- D4 Dónde provee `Clock`: default nuevo `di.kt` en `:app`; alternativa `:framework:database/di/di.kt`.
- D5 Módulo `:feature:panel`: default NO; se usa `:feature:resumen` (ya existe y es el destino Resumen). Crearlo exigiría confirmación.
- D6 Cifra provisional en pantalla: default suma + nº de tickets en texto plano, sustituida por `resumen-total`; alternativa: solo etiqueta de rango y verificar el total únicamente por tests de ViewModel/E2E.
- D7 Refresco a medianoche con la app abierta: default fuera de alcance (se recalcula al volver a componer/reabrir).
