# Feature Implementation Spec: Añadir un gasto sin ticket (gasto-manual)

## Source Feature

- `id`: gasto-manual
- `area`: revision
- `depends_on`: forma-pago (accepted, 50c521a). Contexto: galeria-escaneo accepted en ba71289.
- `status`: not_started
- `source`: `feature_list.json` (:638). Decisión del usuario (2026-10-20): el botón "Añadir gasto" vive en la pestaña Gastos, no en Escanear.

## Goal

Poder registrar un gasto sin tener el ticket. Desde Gastos, un botón abre una pantalla "Nuevo gasto" con el mismo papel editable de la revisión (fecha de hoy, comercio, categoría OTROS, forma de pago, total y líneas opcionales). Al guardar se persiste un `Ticket` **sin foto y sin motor** que aparece en listado, donut y resumen del periodo, y se edita y borra desde el detalle como cualquier otro. El detalle muestra "Sin foto" (distinto de "Foto no disponible") y ningún badge de IA.

## Non-Goals

- Entrada desde Escanear, atajos, widgets o gasto por voz.
- Añadir foto a un gasto manual después (ni convertirlo en escaneado), notas, gastos recurrentes, duplicar un gasto.
- Cambiar el esquema de Room (se mantiene v3), `Motor`, `EngineRouter`, log de extracciones, ADR 0001/0002.
- Filtrar el listado por origen, o marcar "manual" en Resumen/donut.
- Módulos Gradle, dependencias o `libs.versions.toml` nuevos; tocar `build-logic/`.

## Job Story

When pagué algo y no tengo (o no quiero escanear) el ticket,
I want to añadir el gasto a mano desde la lista de Gastos con el mismo formulario de siempre,
so I can mantener el total del periodo completo sin depender de la foto.

## Users And Permissions

Usuario único local. Sin permisos nuevos (ni cámara ni galería), sin red. Funciona en modo avión.

## Acceptance Scenarios

### Scenario 1: Alta mínima
Given la pestaña Gastos con o sin tickets
When pulso Añadir gasto, escribo total 12,50 y pulso Guardar
Then vuelvo a Gastos, aparece una fila de hoy con "Sin comercio", categoría Otros y 12,50 €; Resumen y donut del periodo suman 12,50 más.

### Scenario 2: Alta completa
Given Nuevo gasto
When relleno comercio, fecha anterior (dentro del periodo), categoría, forma de pago Efectivo con entregado, total y dos líneas
Then el gasto guardado conserva todo, con `formaPago/entregado/cambio` normalizados igual que en la revisión y total = el escrito (nunca la suma de líneas).

### Scenario 3: Validación mínima visible
Given Nuevo gasto con total 0,00 (valor inicial) o fecha futura
Then el botón Guardar está deshabilitado y, junto al dock, un texto visible (no solo color) dice el motivo ("Indica un total mayor que 0" / "La fecha no puede ser futura"); al corregir se habilita. Comercio vacío NO bloquea.

### Scenario 4: Detalle sin foto ni IA
Given un gasto manual guardado
When lo abro desde Gastos
Then no hay badge de motor (tag `revision_badge_motor` ausente), aparece la tarjeta "Sin foto" (tag `detalle_sin_foto`) y no `detalle_foto_ausente` ni `detalle_ver_foto`; sí aparece el chip "Manual" (tag `detalle_manual`) y el de forma de pago.

### Scenario 5: Editar y borrar
Given un gasto manual en el detalle
When cambio el total y pulso Guardar cambios, y luego lo elimino con confirmación
Then se actualiza (motor/foto siguen vacíos) y desaparece de listado/donut/resumen; el borrado no falla por ruta de foto vacía.

### Scenario 6: Un gasto manual no es una extracción
Given varios gastos manuales
Then Ajustes > Diagnóstico/log de extracciones no cambia, no cuenta como motor local ni nube y no se puede "reintentar con otro motor".

### Scenario 7: Descartar y salir
Given Nuevo gasto con datos escritos
When pulso Descartar, o atrás
Then Descartar restablece el formulario inicial sin escribir en Room; atrás vuelve a Gastos sin guardar y sin preguntar (ver Open Decisions).

### Scenario 8: Estado vacío, anchos y fuente
Given Gastos sin tickets
Then se ven "Escanear ticket" (`boton_escanear_vacio`, conservado) y "Añadir gasto" (`gastos_anadir_vacio`) apilados. A 360 dp, fontScale 2,0 y tablet horizontal (columna 720 dp) el botón de cabecera `gastos_anadir` se ve sin recorte y Nuevo gasto es usable (dock apilado).

## Repository Research

### Files Inspected

- Dominio: `entities/Ticket.kt` (`motor: Motor` y `rutaFoto: String` no nulos), `Motor.kt` (LOCAL, NUBE), `DatosRevision.kt` (`datosRevisionInicial`: fecha `hoy` con hora, total 0.0), `EdicionDatos.kt` (`conFechaTexto(conservarHora)`, `conTotalTexto`, reductores de pago), `usecases/GuardarTicket.kt` (:48-75 mapeo y `normalizada()`), `ActualizarTicket.kt` (no toca motor/rutaFoto), `BorrarTicket.kt`.
- Motor, grep completo de usos en `src/main`: `when` exhaustivos en `BadgeMotor.kt` (:49, :68), `ProcesadoContent.kt` (:413, :986), `EngineRouter.kt` (:35, :64, :100), `ObtenerOtroMotor.kt` (:12), y `Motor.entries` en `ObservarRegistroExtracciones.kt:35`; `RegistroExtraccion.motor: Motor`. El único uso de `Ticket.motor` en UI es `TicketDetalleScreen.kt:197`.
- Framework: `TicketLocalDataSource.kt` (:58 `motor.name`, :79 `Motor.valueOf(ticket.motor)` lanza con valores desconocidos), `TicketEntity` (`motor TEXT NOT NULL`, `ruta_foto TEXT NOT NULL`), Room v3.
- UI: `TicketDetalleScreen.kt` (:157-269 contenido, :330-361 `SeccionFoto`), `TicketDetalleViewModel.kt`, `GastosScreen.kt` (sin acción de añadir; `EstadoVacio` con `boton_escanear_vacio`), `CabeceraPantalla.kt` (param `accion: @Composable RowScope.() -> Unit`), `FotoOriginal.kt` (`TarjetaFotoAusente`), `FormularioTicketEditable.kt` (:106, `DockEdicion` :485 con tags `revision_descartar/guardar`), `ListadoTicketsUi.kt` (`TEXTO_SIN_COMERCIO = "Sin comercio"` ya cubre comercio en blanco), `app/navigation/TallyApp.kt` y `TallyRoute.kt`.
- Tests afectados: `DetalleTicketFlujoTest` (:303-316 usa una ruta inexistente no vacía), `ListadoGastosVacioTest`/`ListadoGastosFlujoTest` (`boton_escanear_vacio`), `NavigationTest`, `FormaPagoFlujoTest`, `GuardarTicketTest`, `TicketDetalleViewModel*Test`, `test:unit` `FakeTicketDataSource`/`FakePhotoFileStore`. No leído el token HF ni código de captura.

### Patrones a seguir

Reductores puros + `hayCambios`; ViewModel con estado como cadena sobre `MutableStateFlow`; pantalla sin NavController (callbacks); `ColumnaCentrada` (720 dp) + `CabeceraPantalla`; tags aditivos; `runCatchingLanzandoCancelacion` en escrituras.

### Gaps

- `Ticket` asume foto y motor siempre. `GuardarTicket` exige `TicketExtraido`.
- No hay ruta ni pantalla de alta. El `EstadoVacio` solo ofrece escanear.

## Technical Approach

### (a) Decisión de modelo: `Ticket.motor: Motor? = null` (sin `Motor.MANUAL`, sin migración)

Valoradas:
- `Motor.MANUAL`: obliga a ramas sin sentido en `EngineRouter`, `ObtenerOtroMotor`, `etiquetaMotorCorta`, iconos del badge y aparece en `Motor.entries` del log; un "motor" que no extrae contradice el significado de ADR 0001/0002 (Motor = quién extrajo).
- Campo `origen` nuevo: exige migración 3→4 y `4.json` sin necesidad.
- **Elegida**: `Motor` queda LOCAL/NUBE. `Ticket.motor` pasa a `Motor? = null`; `null` = "sin extracción" = gasto manual. Propiedad de dominio `val Ticket.esManual: Boolean get() = motor == null`. Los constructores existentes con `motor = Motor.X` siguen compilando.
- Persistencia sin cambiar esquema (columnas TEXT NOT NULL): constante `internal const val MOTOR_MANUAL = "MANUAL"` en `TicketLocalDataSource.kt`; al escribir `motor?.name ?: MOTOR_MANUAL`; al leer función tolerante `motorDesde(valor: String): Motor?` (`Motor.entries.firstOrNull { it.name == valor }`, desconocido o "MANUAL" = null; nunca lanza, igual que `FormaPago.desde`). `rutaFoto = ""` en los manuales.
- Consecuencia asumida: un valor desconocido de una versión futura se vería como manual (sin badge); es el mismo criterio tolerante de `forma-pago`.
- Nada de lo anterior toca `EngineRouter`, `EngineConRegistro`, `RegistroExtraccion`, prompts ni ajustes. `BadgeMotor.kt` y `ProcesadoContent.kt` no cambian (siguen con `Motor` no nulo; el detalle llama a `BadgeMotor` solo si `motor != null`).

### (b) Dominio (Kotlin puro, `domain/expenses`)

- `DatosRevision.kt`: `fun datosGastoManualInicial(hoy: LocalDate = LocalDate.now()): DatosRevision` = comercio "", `fechaEmision = hoy.atStartOfDay()`, total 0.0, categoria `OTROS`, lineas vacías, pago `NO_INDICADA`. Hora por defecto 00:00 (el listado la oculta a las 00:00); la fecha se edita con `conFechaTexto(texto, conservarHora = false)`.
- `entities/ErrorGastoManual.kt`: `enum class ErrorGastoManual { TOTAL_NO_POSITIVO, FECHA_FUTURA }` y `fun DatosRevision.errorGastoManual(hoy: LocalDate = LocalDate.now()): ErrorGastoManual?` (total no finito o <= 0 → TOTAL_NO_POSITIVO, prioritario; fecha > hoy → FECHA_FUTURA). Comercio vacío válido.
- `usecases/GuardarGastoManual.kt`: `@Inject constructor(ticketRepository)`, `suspend operator fun invoke(datos: DatosRevision): Long`. Si `errorGastoManual() != null` lanza `IllegalArgumentException` (defensa; la UI ya lo evita). Construye `Ticket(id = 0, ..., fechaCaptura = LocalDateTime.now(), motor = null, rutaFoto = "", nota = null, textoCrudoOcr = null, lineas = datos.lineas.aLineas(), formaPago/entregado/cambio de datos.normalizada())`. No relanza ni captura `CancellationException`.
- `ActualizarTicket`/`BorrarTicket`: sin cambios funcionales (verificar con test que `ActualizarTicket` conserva `motor = null`/`rutaFoto = ""` y que borrar un manual funciona con `FakePhotoFileStore.delete("")`).

### (c) Persistencia (`framework/database`)

Solo el mapper de `TicketLocalDataSource.kt` (ver (a)). Sin `TicketEntity`, `Migrations.kt`, `di.kt` ni esquemas. `PhotoFileStoreImpl.delete("")` ya devuelve false; confirmar con test unitario/instrumentado que no lanza ni borra `filesDir`.

### (d) UI

- `GastosScreen.kt`: nuevo callback `onAnadirGasto: () -> Unit = {}` (en `GastosScreen` y `GastosContenido`). Cabecera: `CabeceraPantalla(accion = { IconButton 48 dp, Icons.Outlined.Add, contentDescription "Añadir gasto", tint primary, tag gastos_anadir })`. Sin FAB: DESIGN.md reserva sombra para FAB/docks y un FAB competiría con la barra inferior y la lista; la cabecera es fija y siempre visible (también cargando/vacío). Verificar que el título "Gastos" y subtítulo no se recortan a 360 dp / fontScale 2,0 (el título puede ocupar `weight`).
- `EstadoVacio`: botones apilados a ancho completo: "Escanear ticket" (`boton_escanear_vacio`, primario, sin cambios) y "Añadir gasto manualmente" (`gastos_anadir_vacio`, `OutlinedButton`, alto >= 48 dp). Texto del mensaje: "Escanea un ticket o añade un gasto a mano y aparecerá aquí."
- Nueva pantalla en `:feature:gastos`: `NuevoGastoScreen.kt` (`onBack`, `onGastoGuardado`), `NuevoGastoViewModel.kt` (`@HiltViewModel`, `GuardarGastoManual`), `NuevoGastoState.kt` (scroll, `versionFormulario`/`recrearFormulario`). Estructura como el detalle: `ColumnaCentrada` + `CabeceraPantalla("Nuevo gasto", onBack, backTag = "nuevo_gasto_atras")` + `verticalScroll` + `FormularioTicketEditable` + texto de requisito (`nuevo_gasto_requisito`, `bodySmall`, con icono info, no solo color) + `DockEdicion(guardarHabilitado = error == null && !guardando, descartarHabilitado = hayCambios && !guardando)`. Reutiliza tags `revision_*`. Contenedor `pantalla_nuevo_gasto`. Sin badge, sin foto, sin sección de motor.
- ViewModel: `edicion: MutableStateFlow<DatosRevision>` inicial `datosGastoManualInicial()`; `uiState` = `combine` con `ejecucion`; campos `datos, error: ErrorGastoManual?, hayCambios (datos != inicial), guardando, mensajeError, hecho`. Handlers idénticos a los del detalle (reductores de `EdicionDatos.kt`; fecha con `conservarHora = false`). Guard contra doble toque (`AtomicBoolean`). Fallo de guardado: mensaje `nuevo_gasto_error` "No se pudo guardar el gasto" y reintento; `hecho` dispara `onGastoGuardado` una vez.
- `TicketDetalleScreen.kt`: si `ui.ticket.esManual`: chip "Manual" (icono `Icons.Outlined.EditNote`, estilo de `BadgeMotor`, tag `detalle_manual`, `contentDescription` "Gasto añadido a mano") en lugar de `BadgeMotor`; en lugar de `SeccionFoto` una tarjeta `TarjetaSinFoto` (tag `detalle_sin_foto`; icono `Icons.Outlined.ImageNotSupported`/`HideImage`, texto "Sin foto" + secundario "Este gasto se añadió a mano", mismo estilo que `TarjetaFotoAusente`; nuevo composable en `feature/common/FotoOriginal.kt` junto a `TarjetaFotoAusente`). El visor de foto no se muestra. Para escaneados nada cambia (incluida "Foto no disponible"). Título de barra del detalle: sin cambios ("Detalle del ticket"). Texto del diálogo de borrado: sin cambios.
- Listado y Resumen: `FilaTicket` ya funciona sin foto/motor; no mostrar chip de motor (no existe). Verificar que ninguna lista usa `Ticket.motor`.

### (e) Navegación (`:app`)

- `TallyRoute.kt`: `data object NuevoGasto : TallyRoute("gasto/nuevo")`.
- `TallyApp.kt`: `composable(TallyRoute.NuevoGasto.route) { NuevoGastoScreen(onBack = { navController.popBackStack() }, onGastoGuardado = { navController.popBackStack() }) }`; `GastosScreen(onAnadirGasto = { navController.navigate(TallyRoute.NuevoGasto.route) })`; ocultar la barra inferior también en esta ruta (como el detalle, su dock ocupa el borde); incluirla en `esSecundaria()` (transición deslizante). Tras guardar se vuelve a Gastos; el listado se actualiza solo por el Flow de Room.
- Aviso de periodo: ver Open Decisions (default sin aviso).

## Expected File Changes

- Crear: `domain/.../entities/ErrorGastoManual.kt`, `usecases/GuardarGastoManual.kt`; `feature/gastos/.../NuevoGastoScreen.kt`, `NuevoGastoViewModel.kt`, `NuevoGastoState.kt`; tests `GuardarGastoManualTest`, `GastoManualValidacionTest`, `NuevoGastoViewModelTest`, `TicketLocalDataSource` mapper test (Robolectric no; instrumentado en `framework/database/androidTest` si no hay test JVM), E2E `app/src/androidTest/.../gastos/GastoManualFlujoTest.kt`.
- Modificar: `Ticket.kt` (motor nullable + `esManual`), `DatosRevision.kt`, `TicketLocalDataSource.kt`, `GastosScreen.kt`, `TicketDetalleScreen.kt`, `FotoOriginal.kt` (`TarjetaSinFoto`), `TallyRoute.kt`, `TallyApp.kt`, `FakeTicketDataSource` si procede, `GuardarTicketTest`/otros solo si el compilador lo exige.
- Sin cambios: `Motor.kt`, `EngineRouter.kt`, `BadgeMotor.kt`, `ProcesadoContent.kt`, `Migrations.kt`, esquemas, `di.kt` de framework, gradle.
- `feature:gastos` ya depende de `feature:common` y `domain:expenses`; no hace falta `di.kt` nuevo (`GuardarGastoManual` con `@Inject constructor`).

## Visual Design Impact

UI involved: yes. Fuente: `DESIGN.md` (se actualiza). Tokens Material 3 existentes; sin sombras nuevas; objetivos >= 48 dp; icono + texto, no solo color. Estados a capturar: Gastos con botón de cabecera, Gastos vacío (dos botones), Nuevo gasto (inicial, con requisito, relleno), detalle manual, a 360 dp, fontScale 2,0 y tablet horizontal. Sin artefacto de diseño nuevo.

## Durable Documentation Impact

- `CONTEXT.md`: update. Glosario "Gasto manual"; matizar "Ticket" (deja de derivar siempre de una foto) y "Foto original" (opcional).
- `docs/domain-model.md`: update. "Ticket 1—1 Foto original" pasa a 0..1; `motor` nulo en gasto manual; reglas de validación mínima.
- `docs/build-brief.md`: update breve (MVP incluye alta manual; la vía manual deja de ser solo fallback del OCR).
- `DESIGN.md`: update (botón de cabecera, "Sin foto", chip "Manual").
- `docs/architecture.md`: update (`Ticket.motor` nullable, `GuardarGastoManual`, ruta `gasto/nuevo`); sin ADR nuevo (no contradice ADR 0001/0002; Motor sigue siendo "quién extrajo").
- `docs/risks-and-open-questions.md`: update (valor desconocido de motor leído como manual; gasto fuera del periodo visible).
- `AGENTS.md`, `CONSTRAINTS.md`: not needed.

## Implementation Plan

1. Dominio: `Ticket.motor?`, `esManual`, `datosGastoManualInicial`, validación, `GuardarGastoManual` + tests; compilar todos los módulos.
2. Mapper `motorDesde`/`MOTOR_MANUAL` + test de ida y vuelta.
3. `NuevoGastoViewModel`/`State`/`Screen`, `TarjetaSinFoto`, cambios del detalle.
4. Botón en Gastos (cabecera + vacío), ruta y barra oculta en `:app`.
5. E2E, docs, verificación en emulator-5556 con capturas.

## Implementation Tasks

- [ ] `Ticket.motor: Motor? = null` y `esManual`; arreglar compilación sin tocar aserciones existentes.
- [ ] `datosGastoManualInicial` + `errorGastoManual` + tests (0, negativo, NaN, fecha futura/hoy/ayer, comercio vacío OK).
- [ ] `GuardarGastoManual` + test (fechaCaptura ahora, `motor=null`, `rutaFoto=""`, pago normalizado, líneas filtradas, total no se deriva de líneas, error con datos inválidos).
- [ ] Mapper tolerante + test; confirmar que `Motor.valueOf` ya no queda en el mapper.
- [ ] `NuevoGastoViewModel` + test (inicial, ediciones, requisito, doble toque, fallo y reintento, descartar, `hecho`).
- [ ] Pantalla Nuevo gasto con dock y requisito visible; tags `pantalla_nuevo_gasto`, `nuevo_gasto_atras`, `nuevo_gasto_requisito`, `nuevo_gasto_error`.
- [ ] `TarjetaSinFoto` + chip "Manual" en el detalle; sin `BadgeMotor` para manuales.
- [ ] Botón `gastos_anadir` y `gastos_anadir_vacio`; ruta y navegación; barra oculta.
- [ ] E2E `GastoManualFlujoTest`; ajustar los E2E existentes solo si cambian (ver Verification).
- [ ] Docs (lista arriba), `PROGRESS.md` y `feature_list.json` con evidencia (orquestador/implementer).

## Verification Plan

- `bash init.sh` (assembleDebug + testDebugUnitTest) verde.
- Instrumentación solo en **emulator-5556** con `adb -s emulator-5556 shell am instrument -w -e class ...` (nunca `connected*`, nunca `pm clear`; la tablet y sus datos reales no se tocan; los E2E siembran y limpian sus gastos manuales por id).
- `GastoManualFlujoTest`: Gastos > `gastos_anadir` > total > Guardar > fila visible con "Sin comercio" > detalle: `detalle_sin_foto` y `detalle_manual` presentes, `revision_badge_motor`, `detalle_foto_ausente`, `detalle_ver_foto` ausentes > editar total > eliminar con confirmación. Caso total 0 con Guardar deshabilitado y `nuevo_gasto_requisito` visible. Caso vacío: ambos botones presentes. Comprobar que `RegistroExtracciones` no cambia.
- Existentes que deben seguir verdes sin cambios de aserción: `DetalleTicketFlujoTest` (foto ausente sigue siendo "Foto no disponible" para un escaneado), `ListadoGastosVacioTest` y `ListadoGastosFlujoTest` (`boton_escanear_vacio`), `NavigationTest`, `FormaPagoFlujoTest`, `LogExtraccionesFlujoTest`, `AjustesMotorFlujoTest`. Si `ListadoGastosVacioTest` mide posición/altura del botón, revisar.
- Sin cambio de esquema: `MigracionFormaPagoTest`/`MigrationTest` siguen pasando; no hay `4.json`.
- Capturas con `adb -s emulator-5556` a 360 dp, `font_scale 2.0` (restaurar a 1.0 al acabar) y tablet horizontal si hay AVD; si no, nota explícita.

## Evidence To Capture

Salida de `init.sh`; resultados de `am instrument` por clase; capturas (Gastos con botón, vacío, Nuevo gasto con requisito, detalle manual); nota de que la tabla `extraccion_log` no varió; diff de docs.

## Validator Checklist

- [ ] Escenarios 1-8 comprobados; alcance respetado (sin foto/nota/recurrentes).
- [ ] `Motor`, router, log y ADR sin tocar; `Motor.valueOf` eliminado del mapper; sin migración ni `4.json`.
- [ ] Gasto manual cuenta en listado, donut y resumen; detalle sin badge de IA y con "Sin foto" distinto de "Foto no disponible".
- [ ] Validación: total > 0 y fecha no futura, motivo visible, comercio opcional.
- [ ] Botón `gastos_anadir` 48 dp con `contentDescription`; `boton_escanear_vacio` conservado; ningún tag eliminado.
- [ ] Barra inferior oculta en Nuevo gasto; pantallas sin NavController.
- [ ] Verificación solo en emulator-5556; sin `pm clear` ni `connected*`.
- [ ] Docs actualizadas, `feature_list.json` y `PROGRESS.md` con evidencia.

## Open Decisions (con default)

1. Campos obligatorios: total > 0 y fecha no futura; comercio opcional (el listado ya muestra "Sin comercio"); categoría por defecto Otros. Guardar deshabilitado con motivo visible (default) vs. habilitado con error al pulsar.
2. Hora: 00:00 de la fecha elegida (default; el listado la oculta; queda al final del día) vs. hora actual.
3. Aviso de periodo: sin aviso ni cambio de periodo (default; se vuelve a Gastos con el periodo activo, el gasto de otro periodo no se ve) vs. snackbar "Guardado en <periodo>" o saltar al periodo de la fecha. Riesgo conocido, anotado en riesgos.
4. Salir con cambios: atrás/Descartar sin confirmar (default, como la revisión) vs. diálogo de confirmación.
5. Botón: icono "+" en la cabecera con etiqueta accesible, sin FAB (default); botón con texto solo en el estado vacío.
6. Copy: "Sin foto" / "Este gasto se añadió a mano" y chip "Manual" (default).
