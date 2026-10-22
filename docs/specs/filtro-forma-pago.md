# Feature Implementation Spec: Filtrar Gastos por forma de pago (filtro-forma-pago)

## Source Feature

- `id`: filtro-forma-pago
- `area`: seguimiento
- `depends_on`: forma-pago (accepted, 50c521a). Contexto aceptado: listado-gastos, periodo-selector, rango-personalizado, gasto-manual (7693d98, en `passing` a la espera de validator).
- `status`: not_started
- `source`: `feature_list.json` (:624). Petición del usuario (2026-10-20): "que pueda filtrar tickets de esa forma también, pagos con tarjeta o efectivo".

## Goal

En Gastos, un control "Todos / Efectivo / Tarjeta" filtra el listado, combinado (AND) con el periodo (Día/Semana/Mes/Personalizado) y su rango. El filtro vive en `GastosViewModel`, así que sobrevive a ir al detalle / Nuevo gasto y volver. Con filtro activo y sin coincidencias, un estado vacío específico ofrece "Ver todos".

## Non-Goals

- Filtrar Resumen/donut (decisión 1), filtrar por categoría, comercio, motor u origen, o búsqueda de texto.
- Chip propio para "No indicada" (decisión 2). Persistir el filtro tras matar el proceso o entre sesiones (DataStore): no.
- Cambiar Room (sigue v3), entidades de dominio existentes, módulos Gradle, dependencias, `libs.versions.toml`, `build-logic/`, ADRs.
- Cambiar `ObservarListadoDelPeriodo`, DAOs o `TicketRepository`.

## Job Story

When quiero saber cuánto he pagado con tarjeta (o en efectivo) en un periodo,
I want to filtrar el listado de Gastos por forma de pago sin perder el periodo elegido,
so I can revisar solo esos pagos y volver a la lista tal como la dejé.

## Decisiones cerradas (defaults)

1. **Alcance**: el filtro afecta SOLO al listado de Gastos. Resumen/donut siguen siendo el total del periodo (el usuario no espera que un filtro de una pestaña cambie el total de otra, y entregado/cambio no suman). El filtro es estado de `GastosViewModel`, no compartido con `ResumenViewModel`.
2. **NO_INDICADA**: solo aparece en "Todos"; ni Efectivo ni Tarjeta la incluyen. Sin cuarto chip (los tickets antiguos y manuales sin forma de pago no son una categoría que el usuario quiera buscar; el texto del vacío lo aclara). Se registra como Unknown/limitación aceptada.
3. **Control**: fila de `FilterChip` de Material 3 (selección única, semántica de RadioButton: `Role.RadioButton` + `selected`), alto mínimo 48 dp, bajo el `PeriodoSelector`, mismo margen `PADDING_PANTALLA`. Icono de la forma (`iconoFormaPago`) + texto; el activo con check/color de contenedor (no solo color). Orden: Todos, Efectivo, Tarjeta. A fontScale > 1.1 (umbral del selector) cada chip ocupa su propia fila a ancho completo o usa `FlowRow` para que nunca se recorte; a 360 dp y fontScale 2.0 sin recortes ni solapes. Componente reutilizable `FiltroFormaPagoSelector` en `feature/common/` (nuevo fichero) dentro de la columna de 720 dp (`ColumnaCentrada` ya limita el ancho en tablet).
   Tags: `filtro_pago_todos`, `filtro_pago_efectivo`, `filtro_pago_tarjeta` (contenedor `filtro_pago`).
4. **Persistencia**: `MutableStateFlow<FiltroFormaPago>` en `GastosViewModel`, combinado con `seleccion`; se conserva al navegar a detalle/Nuevo gasto y volver (el ViewModel vive con la entrada del NavBackStack) y al cambiar de periodo o rango (cambiar periodo NO resetea el filtro). La rotación lo conserva por ser ViewModel. Se reinicia al matar el proceso (aceptado). Verificar también ida y vuelta por la barra inferior (`navigateTo` con saveState): si el ViewModel no sobrevive, el implementer lo documenta y lo corrige con la misma entrada del back stack, sin guardar en `SavedStateHandle` salvo necesidad.
5. **Vacío específico**: si el periodo SÍ tiene tickets pero ninguno cumple el filtro, se muestra `listado_vacio_filtro` en lugar de `listado_vacio`: título "Sin gastos en efectivo" / "Sin gastos con tarjeta" en este periodo, mensaje "Hay gastos en este periodo con otra forma de pago o sin indicar." y botón "Ver todos" (tag `filtro_vaciar`, >= 48 dp) que pone el filtro en Todos. Si el periodo no tiene ningún ticket (con cualquier filtro), se mantiene el vacío actual `listado_vacio` (Escanear / Añadir gasto). Para distinguir, el estado lleva `hayTicketsEnPeriodo: Boolean`.
6. **Cabecera**: el subtítulo (`gastos_rango`) sigue mostrando el periodo, sin cambios. El total por día (`CabeceraDia`) refleja los tickets filtrados (se recalcula). No hay total global de cabecera en Gastos hoy; no se añade. El estado de filtro activo se indica solo con el chip seleccionado.
7. **Dominio puro**: ver Technical Approach; sin Room ni módulos nuevos.

## Acceptance Scenarios

### Scenario 1: Filtrar por tarjeta
Given periodo Mes con tickets de efectivo, tarjeta y sin indicar
When pulso Tarjeta
Then solo se ven tickets con tarjeta, los días sin ninguno desaparecen y el total de cada día suma solo los visibles; el chip Tarjeta queda seleccionado (`selected`).

### Scenario 2: Efectivo excluye NO_INDICADA; Todos los incluye
Given tickets EFECTIVO, TARJETA y NO_INDICADA en el periodo
When elijo Efectivo, y después Todos
Then con Efectivo solo los EFECTIVO; con Todos reaparecen todos (incluidos NO_INDICADA).

### Scenario 3: Combinación con periodo y rango
Given filtro Tarjeta activo
When cambio a Día, Semana, Mes o elijo un rango personalizado
Then el filtro sigue activo y el listado es la intersección exacta periodo AND forma de pago; volver a Todos muestra todo el periodo.

### Scenario 4: Vacío por filtro
Given un periodo con tickets pero ninguno de tarjeta
When pulso Tarjeta
Then aparece `listado_vacio_filtro` con "Ver todos"; no aparecen Escanear ni Añadir gasto; al pulsar "Ver todos" vuelve el listado completo y el chip Todos queda seleccionado.

### Scenario 5: Vacío real intacto
Given un periodo sin ningún ticket
Then con cualquier filtro se muestra el vacío original (`listado_vacio`), no el específico.

### Scenario 6: Filtro conservado
Given filtro Efectivo activo y un ticket abierto en el detalle (o Nuevo gasto cancelado/guardado)
When vuelvo a Gastos
Then el chip Efectivo sigue seleccionado y el listado sigue filtrado (y el scroll se conserva como hoy).

### Scenario 7: Resumen no se ve afectado
Given filtro Tarjeta activo en Gastos
When abro Resumen
Then total y donut son los del periodo completo, sin filtrar.

### Scenario 8: Accesibilidad y layout
Given 360 dp y fontScale 2.0, o tablet
Then los tres chips se leen completos, mide cada uno >= 48 dp, no hay solape con el selector de periodo ni se tapan filas, y en tablet el control vive dentro de la columna de 720 dp.

## Repository Research (inspeccionado)

- `feature/gastos/.../GastosViewModel.kt`: `seleccion: MutableStateFlow<Seleccion(tipo, rango)>` -> `flatMapLatest` -> `observarListadoDelPeriodo(periodo).map { GastosUiState }`; constructor con `obtenerPeriodo` y `observarListadoDelPeriodo`. `onSeleccionarPeriodo`/`onSeleccionarRango`.
- `GastosScreen.kt`: `GastosContenido(ui, ...)` público (se pinta con estados fijos en tests); `EstadoVacio` con tag `listado_vacio`, `lista_gastos`, `listado_cargando`; `PeriodoSelector` con padding `PADDING_PANTALLA`/12 dp; `ColumnaCentrada`.
- `GastosState.kt`: solo `LazyListState` (el filtro NO va aquí: es estado de negocio, no visual).
- `feature/common/PeriodoSelector.kt` (píldora, umbral `fontScale > 1.1`, segmentos >= 48 dp, tags `periodo_*`), `FormaPagoUi.kt` (`etiquetaFormaPago`, `iconoFormaPago`, `contenedorFormaPago`), `LayoutAncho.kt` (`ANCHO_MAXIMO_CONTENIDO` 720 dp, `PADDING_PANTALLA` 20 dp).
- Dominio: `ObservarListadoDelPeriodo` (= `ObservarTicketsDelPeriodo` + `AgruparTicketsPorDia`), `GrupoDiaTickets(fecha, tickets, totalCentimos)`, `Ticket.formaPago: FormaPago` (EFECTIVO/TARJETA/NO_INDICADA).
- Tests existentes: `GastosViewModelTest` (construye el ViewModel con `ObtenerPeriodo(clock)` y `ObservarListadoDelPeriodo(...)`, hoy = 2026-10-15, `FakeTicketDataSource`), E2E `app/src/androidTest/.../gastos/` (`ListadoGastosFlujoTest`, `ListadoGastosVacioTest`, `FormaPagoFlujoTest`, `GastoManualFlujoTest` con comercio único + `@After` que borra por repositorio vía `TicketRepositoryEntryPoint`).
- No inspeccionado a fondo: `TallyApp.kt` `navigateTo` (saveState/restoreState de la barra inferior); el implementer debe leerlo (Scenario 6 / decisión 4).

## Technical Approach

- Dominio (`domain/expenses`, Kotlin puro):
  - `entities/FiltroFormaPago.kt`: `enum class FiltroFormaPago { TODOS, EFECTIVO, TARJETA }` con `fun admite(forma: FormaPago): Boolean` (TODOS = true; EFECTIVO/TARJETA solo su forma; NO_INDICADA solo con TODOS).
  - `usecases/FiltrarTicketsPorFormaPago.kt`: `@Inject constructor`, `operator fun invoke(tickets: List<Ticket>, filtro: FiltroFormaPago): List<Ticket>` (identidad con TODOS; conserva orden).
- ViewModel: `filtro = MutableStateFlow(TODOS)`; `uiState` = `combine(seleccion, filtro)` -> `flatMapLatest`: obtener tickets del periodo, filtrar y agrupar. Para no tocar `ObservarListadoDelPeriodo`, inyectar además `ObservarTicketsDelPeriodo`, `FiltrarTicketsPorFormaPago` y `AgruparTicketsPorDia` en lugar de (o junto a) `observarListadoDelPeriodo`; el implementer elige lo más simple y actualiza `GastosViewModelTest` en consecuencia. `GastosUiState` gana `filtro: FiltroFormaPago = TODOS` y `hayTicketsEnPeriodo: Boolean = false`. Nuevo `fun onSeleccionarFiltro(f: FiltroFormaPago)`. Sin `load()`; cadena de operadores (AGENTS.md).
- UI: `GastosContenido` gana `onSeleccionarFiltro: (FiltroFormaPago) -> Unit = {}` (default para tests existentes) y pinta `FiltroFormaPagoSelector` bajo `PeriodoSelector` (visible también en cargando y en vacíos). `EstadoVacioFiltro` privado junto a `EstadoVacio`. Textos en español, sin cadenas fuera de la pantalla.
- `GastosScreen` pasa `viewModel::onSeleccionarFiltro`. `:app` no cambia (salvo lo que revele `navigateTo`).

## Expected File Changes

- Nuevos: `domain/expenses/.../entities/FiltroFormaPago.kt`, `.../usecases/FiltrarTicketsPorFormaPago.kt`, `feature/common/.../FiltroFormaPagoSelector.kt`, tests `FiltroFormaPagoTest.kt`, `FiltrarTicketsPorFormaPagoTest.kt` (dominio), `app/src/androidTest/.../gastos/FiltroFormaPagoFlujoTest.kt`.
- Editados: `GastosViewModel.kt`, `GastosScreen.kt`, `GastosViewModelTest.kt` (constructor + casos del filtro), `feature_list.json`/`PROGRESS.md` (por el implementer). Un fichero `di.kt` por módulo no cambia (casos de uso con `@Inject`).

## Visual Design Impact

`DESIGN.md`: añadir (o confirmar) una nota en la sección "Forma de pago" de un filtro de tres chips de selección única bajo el selector de periodo, con los tokens existentes (`secondaryContainer` para el activo como `PeriodoSelector`, icono por forma). No hay mockup Stitch: se documenta como extensión coherente. Capturas en `docs/evidence/filtro-forma-pago/` (ligeras): listado en Todos, Efectivo, Tarjeta, vacío por filtro, 360 dp a fontScale 2.0, tablet 720 dp.

## Durable Documentation Impact

- `PROGRESS.md`: update (sesión, evidencia, siguiente paso). `feature_list.json`: update (status/evidence, por el orquestador/implementer).
- `DESIGN.md`: update (nota del filtro). `CONTEXT.md`: update (término "Filtro de forma de pago": solo afecta al listado; NO_INDICADA solo en Todos). `docs/domain-model.md`: not needed salvo que liste vistas; revisar. `AGENTS.md`, `ARCHITECTURE.md`/`docs/architecture.md`, ADRs: not needed (sin reglas ni capas nuevas).

## Implementation Plan / Tasks

1. Leer `TallyApp.kt` (`navigateTo`) y `PeriodoSelector` para igualar estilo.
2. Dominio: enum + caso de uso + tests JVM (T-D1).
3. ViewModel: filtro, `hayTicketsEnPeriodo`, `onSeleccionarFiltro`; ajustar `GastosViewModelTest` y añadir casos (T-V).
4. `FiltroFormaPagoSelector` (FilterChips >= 48 dp, FlowRow / apilado con fontScale, tags, semántica `selected`).
5. `GastosContenido`: control, `EstadoVacioFiltro` con "Ver todos".
6. E2E `FiltroFormaPagoFlujoTest` y capturas; docs.
7. Verificación completa y actualización de `PROGRESS.md`/`feature_list.json` con evidencia.

## Verification Plan

- JVM: `gradlew.bat assembleDebug testDebugUnitTest --console=plain` (o `bash init.sh`).
  - `FiltroFormaPagoTest`: `admite` para las 3 formas x 3 filtros (NO_INDICADA solo con TODOS).
  - `FiltrarTicketsPorFormaPagoTest`: lista vacía, conserva orden, TODOS identidad, solo EFECTIVO, solo TARJETA, NO_INDICADA excluida.
  - `GastosViewModelTest` (Turbine): por defecto TODOS; filtro + Día/Semana/Mes/Personalizado = intersección; cambiar periodo conserva filtro; totales por día recalculados; `hayTicketsEnPeriodo` true con filtro sin coincidencias y false con periodo vacío; filtro reactivo a altas/borrados.
- E2E (instrumentado, solo `emulator-5556`): `ANDROID_SERIAL=emulator-5556`, instalar con `:app:installDebug :app:installDebugAndroidTest` y ejecutar con `adb -s emulator-5556 shell am instrument -w -e class com.robertomr.tally.gastos.FiltroFormaPagoFlujoTest com.robertomr.tally.test/<runner>`. NUNCA `connected*AndroidTest`, NUNCA `pm clear`, no tocar dispositivos físicos ni `emulator-5554`. Hay tickets residuales en el emulador: el test crea su propia semilla vía `TicketRepositoryEntryPoint` (3 tickets de hoy con comercio único `E2E filtro ${UUID}`: EFECTIVO, TARJETA, NO_INDICADA) y `@After` los borra por comercio; afirma solo sobre tags `ticket_<id>` de su semilla, nunca totales globales. Casos: Scenarios 1-4, 6 (abrir detalle y volver con `Back`) y 8 (aserciones de alto >= 48 dp con `assertHeightIsAtLeast`). Para el vacío por filtro usar un periodo/rango personalizado con una semilla solo-NO_INDICADA/EFECTIVO en fecha distinta al resto. Scenario 7 con un `onNodeWithTag` del Resumen que no cambie al filtrar (total antes/después de volver).
- Visual: capturas a 360 dp y fontScale 2.0 (`adb -s emulator-5556 shell settings put system font_scale 2.0`, restaurar a 1.0 al final) y tablet.
- No usar ni leer el token de Hugging Face; no se necesita red.

## Evidence To Capture

Salida de gates JVM (conteo de tests nuevos), resultado de `am instrument` (OK (n tests)), 6 capturas en `docs/evidence/filtro-forma-pago/` con README, estado del emulador restaurado (font_scale 1.0, semilla borrada).

## Report Requirement (implementer)

El informe final DEBE decir explícitamente qué ve el usuario y dónde: p. ej. "En la pestaña Gastos, bajo Día/Semana/Mes/Personalizado, tres chips Todos/Efectivo/Tarjeta; al elegir Tarjeta el listado muestra solo...; vacío con 'Ver todos'; Resumen no cambia". Debe indicar también qué NO se verificó (p. ej. iOS no aplica; tablet física no usada).

## Validator Checklist

- [ ] Los 3 chips existen en Gastos con los tags indicados, >= 48 dp, sin recortes a 360 dp / fontScale 2.0.
- [ ] Efectivo/Tarjeta excluyen NO_INDICADA; Todos la incluye.
- [ ] Filtro AND periodo y rango; cambiar periodo no resetea el filtro.
- [ ] Vacío por filtro distinto del vacío real; "Ver todos" funciona.
- [ ] Filtro conservado tras detalle y Nuevo gasto; Resumen y donut sin filtrar.
- [ ] Lógica en dominio con tests JVM; sin cambios de Room, módulos ni dependencias.
- [ ] E2E con semilla propia y limpieza; verificación solo en emulator-5556 con `am instrument`, sin `pm clear` ni `connected*`.
- [ ] Docs actualizados y el informe indica qué ve el usuario y dónde.

## Unknowns / Risks

- Supervivencia del ViewModel con la barra inferior (`saveState`): a comprobar.
- NO_INDICADA invisible al filtrar: limitación aceptada; reabrir si el usuario pide un cuarto chip.
- Cambiar el constructor de `GastosViewModel` rompe `GastosViewModelTest` y cualquier fábrica en tests: actualizar ambos.
- Layout con fontScale 2.0: tres chips con icono + texto pueden desbordar; por eso `FlowRow`/apilado.
