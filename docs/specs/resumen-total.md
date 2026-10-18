# Feature Implementation Spec: Tarjeta de total del período con métricas

## Source Feature

- `id`: resumen-total
- `area`: seguimiento
- `depends_on`: periodo-selector (`accepted`)
- `status`: not_started (al planificar)
- `source`: `feature_list.json`
- Bloquea: `grafica-donut`, `listado-gastos` (comparten el contenedor de Resumen). NO se implementan aquí.

## Goal

Sustituir la cifra provisional (suma + nº de tickets en texto plano) de `ResumenScreen` por la **tarjeta de total** del mockup 02: etiqueta "GASTO TOTAL DEL PERIODO" con el rango, total en display con cifras tabulares (`1.428,50 €`), badge comparativo vs período anterior, y dos micro-métricas (promedio diario, nº de tickets). Introduce `Periodo.anterior()` (dejado fuera en periodo-selector) y el cálculo de métricas en dominio, testeado con datasets conocidos.

## Non-Goals

- Donut, leyenda, desglose por categoría (`grafica-donut`); listado de tickets (`listado-gastos`); rango personalizado (`rango-personalizado`, su `anterior()` queda indefinido: `PeriodoTipo` sigue siendo DIA/SEMANA/MES).
- Navegar a períodos pasados, refresco a medianoche (D7 de periodo-selector sigue vigente), persistir selección.
- Estado vacío formal de pantalla (sin tickets = tarjeta con 0,00 €; el estado vacío completo es de donut/listado).
- Cambios en Room/DAO/esquema, módulos Gradle nuevos, dependencias nuevas. Sin tocar `build-logic/`.
- Refactor de `formatoImporte` duplicado en `:feature:escanear` (no se unifica aquí).

## Job Story

Cuando miro el panel,
quiero ver cuánto he gastado en el período, a qué ritmo diario y si voy por encima o por debajo del período anterior,
para saber de un vistazo si controlo mi gasto.

## Acceptance Scenarios

### Scenario 1: Métricas con datos conocidos
Given tickets con totales 10,00 / 20,50 / 30,00 en el período y 40,00 en el período anterior
When se observa el resumen del período
Then total = 60,50; nº de tickets = 3; promedio diario según D1; comparativa = +51,25 % (60,50 vs 40,00).

### Scenario 2: Período anterior vacío
Given hay tickets en el período activo y ninguno en el anterior
When se calcula la comparativa
Then no hay división por cero ni "Infinity/NaN": la variación es "sin referencia" y la UI muestra un badge neutro "Sin datos del período anterior" (sin porcentaje ni flecha de tendencia).

### Scenario 3: Ambos períodos vacíos
Given ningún ticket ni en el período ni en el anterior
Then total 0,00 €, promedio 0,00 € / día, 0 tickets, badge neutro igual que en el escenario 2.

### Scenario 4: Período actual vacío con anterior con gasto
Then variación = -100 % (badge de bajada), total 0,00 €.

### Scenario 5: Promedio diario en período en curso
Given Mes con hoy = 18-oct-2026 y total 180,00
Then promedio = 180,00 / 18 días transcurridos = 10,00 € / día (no / 31). En un período ya terminado (p. ej. oct-2026 visto el 5-nov) se divide por los días totales. En DIA siempre = total. Nunca se divide por 0.

### Scenario 6: Formato y tabulares
Then el total se pinta como `1.428,50 €` (separador de miles `.` y decimales `,`), el promedio como `46,08 € / día` y la variación como `-4,2 %` (coma decimal, 1 decimal, signo explícito en positivos `+`); todos los textos numéricos de la tarjeta usan `fontFeatureSettings = "tnum"`.

### Scenario 7: Reactividad y selector
Given el selector alterna Día/Semana/Mes o se guarda/borra un ticket
Then tarjeta (total, promedio, nº, comparativa) se recalcula sola por Flow, sin `load()`; el rango activo sigue visible.

## Repository Research

### Files Inspected

- `AGENTS.md`, `CLAUDE.md`, `CONTEXT.md` (Período), `DESIGN.md` (regla-importe tnum; componente "Tarjeta de total"), `docs/specs/periodo-selector.md`, `feature_list.json` (resumen-total, periodo-selector con D1-D7 aprobadas).
- `docs/design/stitch/02-resumen-principal/code.html` líneas 166-200: sección `surface-container-low rounded-3xl p-5`, etiqueta uppercase `label-md` + pill con rango, `display-md` `tabular-nums` + `€` en `headline-sm`, badge `rounded-full secondary-container/60` con icono `trending_down` 16 px y texto "-4.2% vs periodo anterior", rejilla 2 columnas de micro-métricas (`surface-container`, `rounded-2xl`, label-sm + title-sm semibold tnum): "46,08 € / día" y "38 recibos OCR". (`screen.png` no abierto; se usó el HTML.)
- `domain/expenses/.../entities/Periodo.kt` (data class `tipo, desde, hasta`, `de(tipo, referencia)`), `PeriodoTipo.kt`, `usecases/ObservarTicketsDelPeriodo.kt` (`Flow<List<Ticket>>` por `observeByDateRange`, inclusivo), `ObtenerPeriodo.kt` (`Clock`), `Ticket.total: Double`.
- `feature/resumen/.../ResumenViewModel.kt` (`ResumenUiState(cargando, tipo, periodo, numTickets, suma)`, `flatMapLatest` sobre `tipoSeleccionado`), `ResumenScreen.kt` (cifra provisional con tags `resumen_suma`, `resumen_num_tickets`, `periodo_rango`), `EtiquetaPeriodo.kt` (`etiquetaPeriodo`, `formatoImporte` = `String.format(es-ES, "%,.2f €")`), `PeriodoSelector.kt`, `ResumenState.kt`, `build.gradle.kts` (ya con Hilt, dominio e `material-icons-extended`).
- `app/src/androidTest/.../resumen/PeriodoSelectorFlujoTest.kt`: usa `resumen_suma`, `resumen_num_tickets`, `periodo_rango`, `formatoImporte`, `TicketRepositoryEntryPoint`; patrón de siembra y limpieza a reutilizar/adaptar.

### Existing Patterns To Follow

- Un caso de uso por acción, `@Inject constructor`, `operator fun invoke`; dominio Kotlin puro (`java.time`, sin Android); `Clock` inyectado.
- `stateIn` directo (desviación conocida: `stateAsResultIn`/`AcScaffold` no existen). Pantalla = Screen + ViewModel + State.

### Current Gaps

- `Periodo.anterior()` no existe. No hay entidad de métricas. `ResumenUiState` solo lleva suma/recuento. `formatoImporte` no aplica tnum (la tipografía se aplica en el composable). Si la fuente Inter empaquetada no incluyera la feature `tnum`, el ajuste no tendría efecto visible: comprobar en emulador (ver riesgos).

## Technical Approach

**Dominio (`:domain:expenses`).**
- `Periodo.anterior(): Periodo` (mismo `tipo`):
  - DIA: `desde = hasta = desde.minusDays(1)`.
  - SEMANA: ambos extremos `minusDays(7)` (lunes-domingo anterior).
  - MES: `YearMonth.from(desde).minusMonths(1)` → `atDay(1)`..`atEndOfMonth()` (mes natural completo anterior; marzo → febrero 28/29, enero → diciembre del año previo). Nunca `minusDays(longitud)`.
  - Siempre el período anterior **completo**, aunque el actual esté en curso (ver D2).
- `entities/ResumenPeriodo.kt`: `data class ResumenPeriodo(val total: Double, val numTickets: Int, val promedioDiario: Double, val totalAnterior: Double, val variacion: Double?)`; `variacion = (total - totalAnterior) / totalAnterior` como fracción, `null` si `totalAnterior == 0.0` (cubre escenarios 2 y 3). Comparación `<= 0.0` por seguridad: totales negativos no existen (validado en dominio de ticket; no asumir, usar `totalAnterior <= 0.0`).
- `usecases/CalcularResumenPeriodo.kt`: función pura `operator fun invoke(periodo, hoy: LocalDate, tickets, ticketsAnterior): ResumenPeriodo`. Días para el promedio = `ChronoUnit.DAYS.between(periodo.desde, minOf(periodo.hasta, hoy)) + 1`, acotado a `>= 1` (si `hoy < desde` por reloj raro, 1 día). Sin `Clock` dentro: testeable con fechas.
- `usecases/ObservarResumenDelPeriodo.kt` (`@Inject constructor(observarTickets: ObservarTicketsDelPeriodo, calcular: CalcularResumenPeriodo, clock: Clock)`): `operator fun invoke(periodo): Flow<ResumenPeriodo>` = `combine(observarTickets(periodo), observarTickets(periodo.anterior())) { a, b -> calcular(periodo, LocalDate.now(clock), a, b) }`. Reutiliza el punto de entrada existente; sin cambios en repositorio.
- Redondeo: se suma en `Double` y se redondea solo al formatear (HALF_UP de `String.format`). Tests con tolerancia (`1e-9`) o con totales exactos en céntimos.

**Presentación (`:feature:resumen`).**
- `ResumenUiState`: sustituir `numTickets`/`suma` por `resumen: ResumenPeriodo? = null` (mantiene `cargando`, `tipo`, `periodo`).
- `ResumenViewModel`: inyecta `ObservarResumenDelPeriodo` en lugar de `ObservarTicketsDelPeriodo`; misma cadena `flatMapLatest`, sin `load()`.
- `TarjetaTotal.kt` (nuevo, composable `internal`, sin ViewModel): recibe `periodo`, `resumen` y renderiza lo del mockup 02: cabecera (etiqueta + pill de rango con tag `periodo_rango`), total (`resumen_suma`) con cifra display y `€` en `headlineSmall`, badge (`resumen_comparativa`), rejilla 2 columnas (`resumen_promedio`, `resumen_num_tickets`). Retirar el chip de rango y los textos sueltos de `ResumenScreen`.
- `FormatoResumen.kt` (o ampliar `EtiquetaPeriodo.kt`): funciones puras `formatoPromedio(Double) = "${formatoImporte(v)} / día"`, `formatoVariacion(Double?) : String` (`+51,3 %`, `-4,2 %`, `0,0 %`; null → "Sin datos del período anterior"), `textoTickets(n)` ("1 ticket"/"N tickets", se conserva el texto actual para no romper E2E). Estilo `TarjetaTextStyle.tnum = TextStyle(fontFeatureSettings = "tnum")`.
- Badge: icono `Icons.AutoMirrored.Filled.TrendingUp/TrendingDown/TrendingFlat` (extended ya disponible); contenedor `secondaryContainer` al 60 % (mockup); sin rojo/verde de semántica (gastar menos no es "bueno" ni "malo" para la app); con variación `null` badge neutro `surfaceContainer` sin icono de tendencia. Texto: "−4,2 % vs período anterior".
- Etiqueta de comparativa cuando período anterior es el "anterior" de cada tipo: no se añade texto extra ("vs período anterior" fijo).

## Expected File Changes

- `domain/expenses/.../entities/Periodo.kt` — modify (`anterior()`); `ResumenPeriodo.kt` — create.
- `domain/expenses/.../usecases/CalcularResumenPeriodo.kt`, `ObservarResumenDelPeriodo.kt` — create.
- `domain/expenses/src/test/.../entities/PeriodoTest.kt` — modify (casos `anterior()`); `usecases/CalcularResumenPeriodoTest.kt`, `ObservarResumenDelPeriodoTest.kt` — create (con `FakeTicketDataSource`/`Clock.fixed`).
- `feature/resumen/.../ResumenViewModel.kt`, `ResumenScreen.kt` — modify; `TarjetaTotal.kt`, `FormatoResumen.kt` — create.
- `feature/resumen/src/test/.../ResumenViewModelTest.kt` — modify; `FormatoResumenTest.kt` — create (formato `1.428,50 €`, `46,08 € / día`, variación, null).
- `app/src/androidTest/.../resumen/PeriodoSelectorFlujoTest.kt` — modify si cambia algún tag/texto; `ResumenTotalFlujoTest.kt` — create.
- `PROGRESS.md`, `feature_list.json`, `docs/architecture.md` (solo si lista casos de uso) — update por orquestador/implementador. No hay `di.kt` nuevo (los casos de uso son `@Inject`; `Clock` ya en `:app/di/di.kt`).

## Visual Design Impact

- UI: sí. Fuente: `DESIGN.md` (Tarjeta de total, regla-importe) y mockup 02 (líneas 166-200). Se traduce a M3: `surfaceContainerLow` + `RoundedCornerShape(24.dp)` + borde `outlineVariant` 40 %; micro-métricas en `surfaceContainer` con esquinas 16 dp. Texto de nº de tickets: "N tickets" (no el "recibos OCR" ficticio del mockup).
- Estados: normal, sin referencia anterior (badge neutro), vacío (0,00 €), cargando (`cargando = true`: tarjeta con placeholders "—" o sin pintar; sin spinner a pantalla completa).
- No se copia decoración ficticia del mockup. Sin artefacto de diseño nuevo.

## Durable Documentation Impact

- `ARCHITECTURE.md` / `CONSTRAINTS.md`: not needed (no existen). `AGENTS.md`: not needed.
- `CONTEXT.md`: update opcional (si se aprueba D2, añadir "Período anterior" al glosario con la regla). `docs/domain-model.md`: update opcional (métricas del resumen). `docs/architecture.md`: update solo si enumera casos de uso.

## Implementation Plan

1. TDD de dominio: `Periodo.anterior()` + tests de bordes.
2. `ResumenPeriodo`, `CalcularResumenPeriodo` + tests de datasets (escenarios 1-5).
3. `ObservarResumenDelPeriodo` + test con Turbine y reactividad.
4. ViewModel + `ResumenUiState`; adaptar `ResumenViewModelTest`.
5. `FormatoResumen` + tests; `TarjetaTotal` + `ResumenScreen`.
6. E2E, regresión, evidencia.

## Implementation Tasks

- [ ] T1 `Periodo.anterior()`; tests: DIA (1-mar → 28-feb y 29-feb en 2028; 1-ene → 31-dic), SEMANA (lun 27-abr → lun 20-abr; semana que cruza año), MES (mar→feb 28/29, ene→dic año previo, mayo→abril 30), conserva `tipo`, `anterior()` encadenado coherente, resultado sigue cumpliendo `desde <= hasta`.
- [ ] T2 `ResumenPeriodo` + `CalcularResumenPeriodo` + test: escenarios 1-5; promedio con hoy en medio de mes, hoy = último día, hoy posterior al fin, hoy = primer día (1 día), semana en curso (miércoles = 3 días), DIA; anterior vacío; ambos vacíos; actual vacío (-100 %); variación positiva y cero.
- [ ] T3 `ObservarResumenDelPeriodo` + test (Turbine): ticket nuevo en el período actualiza total; ticket en el período anterior actualiza solo la comparativa; Clock fijo en `Europe/Madrid` el 2026-10-25 (cambio de hora) da los mismos días.
- [ ] T4 `ResumenViewModel`/`UiState` + `ResumenViewModelTest` (alternar tipo cambia métricas y `anterior`; por defecto MES).
- [ ] T5 `FormatoResumen` + `FormatoResumenTest`: `1.428,50 €` (4 dígitos con separador de miles; ejecutar el test JVM y, además, comprobarlo en emulador: Android usa ICU), `0,00 €`, `-4,2 %`, `+51,3 %`, `0,0 %`, null.
- [ ] T6 `TarjetaTotal` + integrar en `ResumenScreen` (tags `resumen_suma`, `resumen_promedio`, `resumen_num_tickets`, `resumen_comparativa`, `periodo_rango`; tnum en todos los números); eliminar cifra provisional.
- [ ] T7 E2E `ResumenTotalFlujoTest` (ver verificación) y ajustar `PeriodoSelectorFlujoTest` si procede.
- [ ] T8 Verificación completa, revisión de reglas, `PROGRESS.md` y `feature_list.json` con evidencia.

## Verification Plan

- `.\gradlew.bat assembleDebug testDebugUnitTest --console=plain` (o `bash init.sh`): verde.
- `.\gradlew.bat :domain:expenses:test --rerun` y `:feature:resumen:testDebugUnitTest --rerun`: tests nuevos con los bordes de T1-T5 (cubre la verificación 1 de feature_list: datasets conocidos incl. anterior vacío).
- E2E en **emulator-5556** (5554 está offline) con `adb -s emulator-5556 shell am instrument -w -e class com.robertomr.tally.resumen.ResumenTotalFlujoTest <paquete>.test/androidx.test.runner.AndroidJUnitRunner` tras `:app:assembleDebug :app:assembleDebugAndroidTest` e instalación de ambos APK. **Nunca** `connected*`. Sembrar tickets (patrón de `PeriodoSelectorFlujoTest`: línea base leída del repositorio + sembrado, limpieza en `@After`) con total conocido en el mes actual y otro en el mes anterior; comprobar `resumen_suma`, `resumen_promedio`, `resumen_num_tickets`, `resumen_comparativa` por texto; caso sin datos del anterior solo si la línea base lo permite (si el emulador ya tiene tickets del mes anterior, cubrirlo en JVM). Regresión: `PeriodoSelectorFlujoTest`, `NavigationTest`, Ajustes (omitir `*RealTest`).
- Verificación 2 de feature_list (tnum): revisión de código (`fontFeatureSettings = "tnum"` en todos los textos numéricos) + captura en emulador con total de 4 dígitos (`1.428,50 €`) en `docs/evidence/resumen-total/`; la revisión visual de imagen queda a humano. Comprobar que Inter empaquetada soporta `tnum`; si no, anotar como deuda.
- Revisión: 0 imports de Android en `domain/**`; sin `LocalDate.now()` suelto (solo con `Clock`); sin módulos ni dependencias nuevas; sin versiones literales; sin `NavController`; `git diff` vacío en `build-logic/`.

## Evidence To Capture

- Conteo JUnit por clase (dominio y feature), resultado `am instrument` en emulator-5556, capturas por Día/Semana/Mes y caso sin referencia, decisiones D1-D5 resueltas en `PROGRESS.md`.

## Validator Checklist

- [ ] Alcance: sin donut, listado ni rango personalizado.
- [ ] `Periodo.anterior()` correcto en bordes (mes 28/29/30/31, año, semana).
- [ ] Anterior vacío sin NaN/Infinity y con badge neutro; actual vacío -100 %.
- [ ] Promedio según D1 verificado por test con `hoy` explícito.
- [ ] Formato `1.428,50 €`, `46,08 € / día`, variación con coma; tnum en todas las cifras.
- [ ] Dominio puro, Flow reactivo sin `load()`, `Clock` inyectado, sin módulos nuevos.
- [ ] Tests y E2E existentes en verde (tags `resumen_suma`, `resumen_num_tickets`, `periodo_rango` conservados o tests migrados).

## Open Decisions (defaults si no hay respuesta)

- D1 Promedio diario en período en curso: default **días transcurridos** (desde `desde` hasta `hoy` inclusive, mín. 1) y días totales si el período ya terminó. Alternativa: siempre días totales (promedio engañosamente bajo a principios de mes).
- D2 Comparativa en período en curso: default total actual vs período anterior **completo** (literal, simple). Alternativa like-for-like (anterior truncado al mismo nº de días transcurridos), más justa pero más compleja y distinta del promedio. Riesgo: a principios de mes el badge mostrará bajadas grandes.
- D3 Variación con anterior vacío: default `null` + badge neutro "Sin datos del período anterior". Alternativa: ocultar el badge.
- D4 Color del badge: default neutro (`secondary-container`) sin semántica verde/rojo, como el mockup.
- D5 Texto de nº de tickets: default "N tickets" (no "recibos OCR").
- D6 `Double` para importes: default seguir con `Double` (como `Ticket.total`) y redondear al formatear; migrar a `BigDecimal`/céntimos queda fuera de alcance.
