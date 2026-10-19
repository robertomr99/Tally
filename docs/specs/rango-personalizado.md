# Feature Implementation Spec: Rango de fechas personalizado

## Source Feature

- `id`: rango-personalizado (area seguimiento)
- `depends_on`: periodo-selector (`accepted`). Ya aceptadas y commiteadas: resumen-total, grafica-donut, listado-gastos.
- `status`: not_started (al planificar)
- `verification` (feature_list.json): (1) rango marcado a mano se refleja en total, gráfica y listado; (2) validación de rango invertido (inicio > fin).
- Nota de la feature: "Requisito explícito del usuario; Material 3 date range picker."

## Goal

Añadir el cuarto segmento "Personalizado" al selector de período (Día / Semana / Mes / Personalizado). Al pulsarlo se abre el Material 3 date range picker; al confirmar, el rango elegido filtra por fecha de emisión la tarjeta de total (con comparativa y promedio), el donut con su leyenda, la vista previa de recientes (Resumen) y el listado completo (Gastos).

## Non-Goals

- Chips de atajos del mockup 09 ("Últimos 7 días", "Este mes", "Mes anterior"), tarjeta "Rango activo de auditoría", nota de días laborables, "Restablecer": no pertenecen al requisito (D3).
- Persistir el rango entre arranques (coherente con D1 de periodo-selector), flechas de navegación a períodos pasados, refresco a medianoche.
- Compartir la selección entre Resumen y Gastos: cada pantalla mantiene la suya, como hasta ahora (D5).
- Cambios en Room/DAO/esquema (`observeByDateRange` inclusivo ya sirve), módulos Gradle nuevos, dependencias nuevas.

## Job Story

Cuando quiero saber cuánto gasté en un tramo que no es un día, semana o mes natural (p. ej. un viaje del 12 al 20),
quiero marcar ese rango en un calendario,
para ver total, gráfica y tickets solo de ese tramo.

## Acceptance Scenarios

### Scenario 1: Elegir rango
Given Resumen (o Gastos) con el selector en Mes
When pulso "Personalizado", marco inicio y fin en el picker y confirmo
Then el segmento Personalizado queda `selected`, el chip de rango muestra el rango (`12 Oct – 20 Oct`), y total, nº de tickets, promedio, donut y recientes/listado reflejan solo tickets con fecha de emisión en [desde, hasta] inclusive.

### Scenario 2: Cancelar
Given el picker abierto
When lo cierro sin confirmar
Then la selección anterior no cambia (si nunca hubo rango, el segmento activo sigue siendo el previo). El botón de confirmar está deshabilitado hasta que haya inicio Y fin.

### Scenario 3: Rango de un solo día y límites
Given que marco el mismo día como inicio y fin
Then es un rango válido de 1 día (`desde == hasta`, etiqueta `18 Oct`). Los días posteriores a hoy no son seleccionables (D6).

### Scenario 4: Rango invertido
Given `desde > hasta` (inalcanzable desde el picker M3, que ordena la selección)
When se intenta crear `Periodo.personalizado(desde, hasta)` o llamar a `onSeleccionarRango`
Then el dominio lanza `IllegalArgumentException` (el `require` existente) y el ViewModel ignora la petición sin cambiar el estado ni fallar (D4).

### Scenario 5: Comparativa y promedio
Given un rango personalizado de N días
Then "periodo anterior" = los N días inmediatamente anteriores (`hasta' = desde-1`, `desde' = desde-N`), incluso cruzando mes, año o 29-feb. Promedio diario = total / días transcurridos (`desde`..min(`hasta`, hoy)), mínimo 1; rango totalmente futuro (solo por código, no por el picker) = 1 día, sin dividir por cero.

### Scenario 6: Persistencia en sesión
Given un rango elegido en Resumen
When cambio a Gastos y vuelvo, o roto la pantalla
Then Resumen conserva el rango (estado en ViewModel; la bottom bar usa `saveState/restoreState`). Si cambio a Día y vuelvo a pulsar Personalizado, el picker se abre inicializado con el último rango.

### Scenario 7: Conversión de fechas del picker
Given el picker devuelve epoch millis a medianoche UTC
When se convierten a `LocalDate`
Then se usa `Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()` (nunca la zona del dispositivo; en Madrid daría un día de error en el borde) y el viaje de ida y vuelta es exacto.

## Repository Research

### Files Inspected
`AGENTS.md`, `CONTEXT.md` (Período = Día/Semana/Mes/Rango personalizado), `DESIGN.md` (selector con "Personalizado"; mockup 09 mapeado), `feature_list.json`, mockup `docs/design/stitch/09-selector-rango-fechas/screen.png`, specs `periodo-selector.md` (obsoleta en partes: ya no existe `:feature:resumen/PeriodoSelector`), y el código:
- `domain/expenses/.../entities/PeriodoTipo.kt` (DIA, SEMANA, MES), `Periodo.kt` (`data class(tipo, desde, hasta)` con `require(desde <= hasta)`; `anterior()` con `when` exhaustivo; `de(tipo, referencia)` con `when` exhaustivo).
- `usecases/ObtenerPeriodo.kt` (`invoke(tipo)` con `Clock`), `CalcularResumenPeriodo.kt` (promedio con `min(hasta, hoy)` y `coerceAtLeast(1)`: ya robusto ante futuro), `ObservarResumenDelPeriodo.kt` (usa `periodo.anterior()`), `ObservarTicketsDelPeriodo`, `ObservarDistribucionDelPeriodo`, `ObservarListadoDelPeriodo` (todos reciben `Periodo`, sin `when` por tipo: no cambian).
- `feature/common/.../PeriodoSelector.kt` (3 segmentos, tags `periodo_dia|semana|mes`, `semantics.selected`), `EtiquetaPeriodo.kt` (sin año).
- `feature/resumen/.../ResumenViewModel.kt` y `feature/gastos/.../GastosViewModel.kt`: cada uno con su `MutableStateFlow<PeriodoTipo>(MES)` -> `flatMapLatest { obtenerPeriodo(tipo) ... }`, `stateIn`. `ResumenScreen.kt`, `GastosScreen.kt`, `TarjetaTotal.kt` (tags `periodo_rango`, `resumen_*`; Gastos: `gastos_rango`).
- `app/.../navigation/TallyApp.kt` (bottom bar con `popUpTo(start){saveState}`, `launchSingleTop`, `restoreState`).
- `app/src/androidTest/.../resumen/PeriodoSelectorFlujoTest.kt` (patrón de siembra + línea base + `@After` de limpieza; usa `PeriodoTipo.entries` con `Periodo.de`: se rompería con el nuevo valor).
- `gradle/libs.versions.toml`: BOM `composeBom = "2026.02.01"`, `androidx-compose-material3` sin versión propia (la da el BOM). `feature/common/build.gradle.kts` aplica `tally.android.compose` (Material3 ya disponible, no inspeccionado el plugin).

### Hallazgos relevantes
- `Periodo` ya admite rangos arbitrarios; no hace falta tipo aparte.
- Resumen y Gastos tienen selecciones independientes hoy; no se tocan sus contratos de módulo (no se conocen entre sí).
- `DateRangePicker`/`DatePickerDialog`: el implementador debe confirmar en el BOM 2026.02.01 (Material3 1.4.x) si `DateRangePicker`, `rememberDateRangePickerState` y `SelectableDates` son estables o requieren `@OptIn(ExperimentalMaterial3Api::class)`; no se verificó contra el artefacto. Si hay opt-in, usar `@OptIn` local, sin subir versiones. Si hiciera falta cambiar la versión del BOM, parar y preguntar.

## Technical Approach

**Dominio (`:domain:expenses`)**
- `PeriodoTipo`: añadir `PERSONALIZADO`.
- `Periodo`: nueva `companion fun personalizado(desde, hasta) = Periodo(PERSONALIZADO, desde, hasta)`. `de(PERSONALIZADO, ref)` lanza `IllegalArgumentException("un período personalizado necesita desde y hasta")`. `anterior()` para PERSONALIZADO: `n = ChronoUnit.DAYS.between(desde, hasta) + 1`; `Periodo(tipo, desde.minusDays(n), desde.minusDays(1))`.
- `ObtenerPeriodo.invoke(tipo, personalizado: Periodo? = null)`: PERSONALIZADO devuelve `personalizado` (error si es `null` o de otro tipo); el resto igual. Los ViewModels llaman a `obtenerPeriodo(PeriodoTipo.DIA).desde` para "hoy" sin cambios.
- Sin cambios en `CalcularResumenPeriodo` ni en los casos de uso de observación (solo tests nuevos).

**Presentación**
- ViewModels (`ResumenViewModel`, `GastosViewModel`): la entrada pasa de `MutableStateFlow<PeriodoTipo>` a un `MutableStateFlow` de una pequeña data class privada/interna `Seleccion(tipo, rango: Periodo?)` (o par de flujos con `combine`). `onSeleccionarPeriodo(tipo)` sigue existiendo para Día/Semana/Mes (con PERSONALIZADO y rango previo lo reaplica; sin rango previo se ignora: el selector abre el picker antes). Nueva `onSeleccionarRango(desde: LocalDate, hasta: LocalDate)`: si `desde > hasta` no hace nada; si no, fija `Seleccion(PERSONALIZADO, Periodo.personalizado(...))`. Sigue sin `load()`. Los `UiState` exponen además `rango: Periodo?` (último rango elegido, para inicializar el picker).
- `feature/common`: nuevo `RangoFechasDialog.kt` (público): `DatePickerDialog` + `DateRangePicker` (mockup 09 reducido: título "Seleccionar rango de fechas", confirmar "Aplicar rango" deshabilitado hasta tener inicio y fin, "Cancelar"), con `SelectableDates` `<= hoy` y `initialSelectedStartDateMillis/EndDateMillis` desde el último rango. Helpers puros `LocalDate.aMillisUtc()` / `Long.aLocalDateUtc()` con test JVM. Colores de tema, sin literales.
- `PeriodoSelector` pasa a 4 segmentos: añade "Personalizado" (tag `periodo_personalizado`, `maxLines = 1`, ajustar padding horizontal para que quepa en 360 dp). Nueva API: `PeriodoSelector(seleccionado, rango, hoy, onSeleccionarTipo, onSeleccionarRango)` que mantiene internamente `rememberSaveable` el "picker abierto" (evita duplicarlo en ambas pantallas); pulsar Personalizado abre el picker siempre (con rango previo inicializado). Los features siguen sin conocerse.
- `EtiquetaPeriodo.kt`: `etiquetaPeriodo(periodo, hoy: LocalDate? = null)`; para PERSONALIZADO añade año (`28 Dic 2025 – 4 Ene 2026`) si `desde.year != hasta.year` o su año no es el de `hoy`. Día/Semana/Mes sin cambios (los E2E existentes comparan esas etiquetas).
- Cambio en `GastosContenido`/`ResumenContenido`: pasar los nuevos parámetros; sin `NavController`.

## Expected File Changes

- modify `domain/expenses/.../entities/PeriodoTipo.kt`, `Periodo.kt`, `usecases/ObtenerPeriodo.kt`.
- modify tests: `PeriodoTest.kt` (la línea 128 itera `PeriodoTipo.entries`: excluir PERSONALIZADO), `ObtenerPeriodoTest.kt`, `CalcularResumenPeriodoTest.kt`, `ObservarResumenDelPeriodoTest.kt`.
- modify `feature/common/.../PeriodoSelector.kt`, `EtiquetaPeriodo.kt` (+ `EtiquetaPeriodoTest.kt`); create `RangoFechasDialog.kt`, `FechasPicker.kt` (helpers UTC) + `FechasPickerTest.kt`.
- modify `feature/resumen/.../ResumenViewModel.kt`, `ResumenScreen.kt`, `ResumenViewModelTest.kt`; `feature/gastos/.../GastosViewModel.kt`, `GastosScreen.kt`, `GastosViewModelTest.kt`.
- modify `app/src/androidTest/.../resumen/PeriodoSelectorFlujoTest.kt` (filtrar `PeriodoTipo.entries` a los 3 tipos); create `app/src/androidTest/.../resumen/RangoPersonalizadoFlujoTest.kt`.
- `PROGRESS.md`, `feature_list.json`: actualizar por orquestador/implementador. Sin `di.kt` nuevos ni módulos nuevos.

## Visual Design Impact

- UI: sí. Fuente: `DESIGN.md` (selector con 4 segmentos, activo `secondary-container`), mockup 02/03 (selector) y 09 (picker; solo calendario de rango, cabecera y acción de aplicar; el resto en D3).
- Pantallas afectadas: Resumen y Gastos (selector y chip de rango), diálogo de rango nuevo. Estado vacío de Gastos sin cambios.
- Sin artefacto de diseño nuevo.

## Durable Documentation Impact

- `AGENTS.md`, `CONSTRAINTS.md`, `ARCHITECTURE.md`: not needed. `docs/architecture.md`: not needed salvo que liste `PeriodoTipo`. `CONTEXT.md`/`docs/domain-model.md`: opcional añadir una línea (rango personalizado inclusivo; anterior = N días previos) si el usuario aprueba D1/D2.

## Implementation Plan

1. Dominio (TDD): `PERSONALIZADO`, `personalizado()`, `anterior()`, `ObtenerPeriodo`; arreglar los `when` exhaustivos y tests que iteran `entries`.
2. Tests de dominio de promedio/comparativa con rangos personalizados.
3. Helpers UTC + test; `EtiquetaPeriodo` con año + test.
4. `RangoFechasDialog` y `PeriodoSelector` de 4 segmentos.
5. ViewModels y pantallas de Resumen y Gastos; tests de ViewModel.
6. E2E y regresión.

## Implementation Tasks

- [ ] T1 `PeriodoTest`: `anterior()` personalizado de 1 día, 7 días, cruzando mes, año y 29-feb-2028; `de(PERSONALIZADO, x)` lanza; `personalizado(desde>hasta)` lanza; `desde==hasta` válido.
- [ ] T2 `ObtenerPeriodoTest`: PERSONALIZADO con y sin rango; el resto sin regresión (Clock fijo Madrid).
- [ ] T3 `CalcularResumenPeriodoTest`: rango pasado completo (días totales), rango en curso (hasta hoy), rango totalmente futuro (1 día, sin NaN/infinito), rango de 1 día hoy; comparativa contra anterior de N días y `variacion == null` si anterior vacío.
- [ ] T4 `FechasPickerTest`: ida y vuelta `LocalDate`<->millis UTC (incl. 29-feb, 31-dic); no depende de `TimeZone.getDefault()` (probar con zona por defecto Madrid y Pacific/Auckland).
- [ ] T5 `EtiquetaPeriodoTest`: año en cruce de año y en año distinto del actual; sin cambios para Día/Semana/Mes.
- [ ] T6 `RangoFechasDialog` + `PeriodoSelector` (4º segmento, tag `periodo_personalizado`, confirmar deshabilitado sin fin, límite `<= hoy`, `@OptIn` solo si el BOM lo exige).
- [ ] T7 ViewModels: `onSeleccionarRango` (válido, 1 día, invertido ignorado), el rango alimenta resumen, distribución y listado a la vez, reaplicar rango al volver a PERSONALIZADO, cambio Personalizado -> Día -> Personalizado conserva rango; tests con Turbine y `FakeTicketDataSource`.
- [ ] T8 E2E `RangoPersonalizadoFlujoTest` (ver Verification) y regresión de `PeriodoSelectorFlujoTest`, `ResumenTotalFlujoTest`, `GraficaDonutFlujoTest`, `ListadoGastosFlujoTest`, `ListadoGastosVacioTest`, `NavigationTest`.
- [ ] T9 Revisión de reglas, `PROGRESS.md` y `feature_list.json` con evidencia.

## Verification Plan

- `.\gradlew.bat assembleDebug testDebugUnitTest --console=plain` (o `bash init.sh`): verde.
- Unit: `:domain:expenses:test --rerun`, `:feature:common:testDebugUnitTest`, `:feature:resumen:testDebugUnitTest`, `:feature:gastos:testDebugUnitTest`.
- E2E en `emulator-5556` (5554 offline) con `adb -s emulator-5556 ... am instrument -w -e class <clase>` (comando completo de `PROGRESS.md`); NUNCA `connected*`. El emulador tiene tickets residuales de 0,00 EUR: expectativas = línea base leída del repositorio en el rango + lo sembrado; limpiar lo sembrado en `@After` (patrón de `PeriodoSelectorFlujoTest`). Escenario: sembrar tickets dentro, en los bordes (primer y último día) y fuera del rango, con emisión distinta de captura; abrir Resumen, pulsar `periodo_personalizado`, marcar el rango, confirmar; comprobar `periodo_rango`, `resumen_suma`, `resumen_num_tickets`, segmento `selected`; abrir Gastos, marcar el mismo rango y comprobar que `lista_gastos` contiene solo los tickets esperados (`ticket_<id>`); cancelar el picker no cambia nada; volver a Resumen conserva el rango. Marcar días en el picker: probar por `onNodeWithText`/descripción del día y, si es frágil, por modo entrada de texto; documentar la elección. Si ni así es estable, justificar y cubrir la selección en un test de Compose del diálogo.
- Revisión: 0 imports Android en `domain/**`, sin versiones literales en `**/*.gradle.kts`, sin módulos nuevos, `git diff` vacío en `build-logic/` y ADRs, features sin dependencias entre sí.
- Capturas en `docs/evidence/rango-personalizado-*.png` (picker, Resumen con rango, Gastos con rango).

## Evidence To Capture

JUnit por clase (dominio, common, resumen, gastos), salida de `am instrument` en emulator-5556, capturas, versión efectiva de Material3 y si requirió opt-in, decisiones D1-D7 resueltas en `PROGRESS.md`.

## Validator Checklist

- [ ] Scope: solo Personalizado + picker; sin chips de atajos ni persistencia entre arranques.
- [ ] `PERSONALIZADO` en dominio; `anterior()` = N días inmediatamente previos con tests de cruces.
- [ ] Conversión UTC correcta, sin zona del dispositivo.
- [ ] Rango invertido rechazado en dominio y ignorado en ViewModel, con tests.
- [ ] Total, donut, recientes y listado filtran igual por el rango, en ambas pestañas.
- [ ] Rango persiste al cambiar de pestaña y al rotar; cancelar no altera la selección.
- [ ] Tests existentes (incl. `PeriodoTipo.entries`) adaptados y en verde; E2E con línea base y limpieza.
- [ ] Reglas de AGENTS.md (dominio puro, un `di.kt`, versiones en catálogo, features aisladas).

## Open Decisions (defaults si no hay respuesta)

- D1 Modelado: default `PeriodoTipo.PERSONALIZADO` + `Periodo(desde, hasta)` existente con factory `personalizado()`; `de(PERSONALIZADO)` lanza. Alternativa: sealed class aparte (más invasiva: toca todos los casos de uso).
- D2 Período anterior: default N días inmediatamente anteriores (mismo nº de días). Texto del badge sin cambios ("vs período anterior"). Alternativa: texto "vs N días anteriores".
- D3 Alcance del picker: default `DatePickerDialog` + `DateRangePicker` M3 con cabecera y "Aplicar rango"; sin chips de atajos ni tarjetas informativas del mockup 09. Alternativa: pantalla completa fiel al mockup (mucho mayor).
- D4 Rango invertido: default rechazar (dominio lanza, ViewModel ignora, el picker no permite confirmarlo). Alternativa: intercambiar extremos automáticamente.
- D5 Selección compartida Resumen/Gastos: default independiente por pantalla (como hoy). Alternativa: un holder singleton en dominio con la selección (cambia el comportamiento ya aceptado de ambas pantallas).
- D6 Límites del picker: default no seleccionables días posteriores a hoy, sin límite inferior ni rango máximo (Room lo soporta). Esto evita rangos futuros desde la UI; el dominio sigue siendo robusto ante ellos (promedio mínimo 1 día). Alternativa: permitir futuro.
- D7 Etiqueta con año: default añadirlo solo en rangos personalizados que cruzan año o no son del año actual.
- D8 Persistencia: default solo en memoria del ViewModel (sobrevive a rotación y a cambio de pestaña, no a matar la app), coherente con D1 de periodo-selector.
