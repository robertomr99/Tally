# Feature Implementation Spec: Detalle de ticket guardado con foto y borrado en cascada

## Source Feature

- `id`: detalle-ticket · `area`: revision · `status`: not_started (al planificar, 2026-10-19)
- `depends_on`: revision-datos, edicion-lineas (ambas `accepted`); `listado-gastos` tambien `accepted` (deja la ruta `ticket/{ticketId}` y el placeholder).
- `user_visible_behavior` (fuente): desde el listado se abre el ticket guardado (mockup 07): ver foto original, editar (reutiliza edicion) y borrar con confirmacion; al borrar desaparecen ticket, lineas y foto.
- `verification` (fuente): (1) borrado confirmado elimina registro y fichero de foto (comprobado en filesDir); (2) editar un ticket guardado persiste los cambios; (3) test de borrado en cascada del repositorio.
- `docs/domain-model.md`: Guardado es editable y borrable; Borrar = ticket + lineas + foto en cascada, con confirmacion; Editar (Guardado) -> edicion en memoria -> Guardar cambios.

## Goal

Sustituir `TicketPendienteScreen` (`:app`) por la pantalla real de detalle, manteniendo la ruta `ticket/{ticketId}`: se llega desde Gastos y desde la vista previa de Resumen; muestra la foto original, la cabecera y las lineas **con la misma edicion de `revision-datos`/`edicion-lineas`** (D1), permite "Guardar cambios" sobre el ticket existente (`TicketRepository.save` con el `id`, que el DAO trata como update + reemplazo de lineas) y "Eliminar ticket" con dialogo de confirmacion que borra ticket, lineas y fichero de foto.

## Non-Goals

- Modulo Gradle nuevo, cambios de esquema Room, dependencias nuevas, `build-logic/`, `init.sh`, ADRs.
- Reeditar la foto, repetir extraccion, cambiar motor, editar hora de emision, nota, "Reanalizar", compartir/exportar.
- Insignias/decoracion ficticia de los mockups 07/08 (verified, NIF, IVA desglosado, hash, "Confianza %"); ya prohibidas por `DESIGN.md`.
- Deshacer el borrado (no hay papelera), borrado masivo, swipe-to-delete en el listado.
- Zoom/pellizco de la foto (visor sencillo basta; ver D4).
- Observacion reactiva del ticket abierto (`observeById`): ver D6.

## Job Story

Cuando veo un gasto en el listado y algo no cuadra con el papel,
quiero abrirlo, comprobar la foto original, corregir datos y lineas o eliminarlo,
para que el panel de gastos refleje la realidad.

## Acceptance Scenarios

### Scenario 1: abrir desde Gastos y desde Resumen
Given un ticket guardado con 2 lineas y foto en disco
When se toca su fila en Gastos o en la vista previa de Resumen
Then se abre `pantalla_detalle_ticket` con comercio, fecha (d/M/yyyy), categoria, total y las 2 lineas con sus valores, el badge del motor real (`ticket.motor`) y la foto original; sin crash y sin barra inferior (D5).

### Scenario 2: estados cargando, inexistente y foto ausente
- Cargando: mientras `getById` no responde, spinner `detalle_cargando` (nunca formulario vacio editable).
- Ticket inexistente (id borrado desde otro sitio o ruta obsoleta): `detalle_no_existe` con texto "Este ticket ya no existe" y boton "Volver" (`onBack`); sin formulario ni crash.
- Foto ausente (`rutaFoto` en blanco, fichero borrado o ilegible): en lugar de la imagen, tarjeta `detalle_foto_ausente` "Foto no disponible"; el resto (editar, guardar, eliminar) funciona igual.

### Scenario 3: editar persiste (verificacion 2)
Given el detalle abierto
When se cambia comercio, fecha, total, categoria, y se anade/edita/elimina una linea, y se pulsa "Guardar cambios"
Then se persiste el MISMO ticket (mismo `id`, conserva `fechaCaptura`, `motor`, `rutaFoto`, `textoCrudoOcr`, `nota` y la **hora** de emision), con exactamente el conjunto de lineas editado; se vuelve a la pantalla de origen (`onBack`) y esta muestra el cambio sola por Flow (nuevo total, nuevo grupo de dia si cambio la fecha). El total sigue siendo autoritativo: editar lineas no lo cambia (regla "Total vs lineas").

### Scenario 4: Guardar solo con cambios y Descartar
"Guardar cambios" esta deshabilitado mientras `datos == inicial`. "Descartar" restablece el formulario a los valores guardados (sin escribir en Room) y queda deshabilitado/inocuo si no hay cambios. Atras del sistema o flecha de la barra superior cierra el detalle sin guardar ni confirmar (D3); nada se escribe.

### Scenario 5: borrar con confirmacion (verificacion 1)
When se pulsa el icono "Eliminar" de la barra superior
Then `AlertDialog` `detalle_confirmar_borrado` ("Eliminar ticket", texto con comercio e importe, "Eliminar" en paleta `error` / "Cancelar"). Cancelar o tocar fuera no hace nada. Confirmar: se borran la fila del ticket, sus lineas (cascada FK) y el fichero `filesDir/<rutaFoto>`; se vuelve a la pantalla de origen y el ticket ha desaparecido del listado, del total y del donut sin accion manual (Flow). Un segundo borrado del mismo id es no-op.

### Scenario 6: fallos del borrado
- Falla el borrado en BD (excepcion): el ticket permanece intacto, el dialogo se cierra, `detalle_error` muestra "No se pudo eliminar el ticket" y se puede reintentar; la foto NO se toca (orden DB-antes-foto, `TicketRepository.delete`).
- BD borra pero el fichero no (`PhotoFileStore.delete` devuelve false o lanza): el borrado es **exito** para el usuario (el ticket ya no existe); queda un fichero huerfano en `filesDir/fotos`, aceptado y documentado (best-effort, ya previsto en `persistencia-room`). El repositorio captura la excepcion de la foto (salvo `CancellationException`) para que no convierta un borrado hecho en error.
- Pulsar "Eliminar" dos veces: la segunda es ignorada mientras el borrado esta en curso; y tras el borrado propio la UI no muestra "ya no existe" (flag `eliminado`, sale por `onBack` una sola vez).

### Scenario 7: periodo activo al volver
Al abrir el detalle la entrada de Gastos/Resumen permanece en la pila (`navigate` sin `popUpTo`), asi que su ViewModel conserva `seleccion` (tipo Dia/Semana/Mes y rango personalizado) y la lista su posicion. Al volver tras editar/borrar: mismo periodo y mismo selector; solo cambia el contenido (un ticket editado a fecha fuera del rango sale de la lista; es el comportamiento esperado, no se cambia el periodo). Sin estado nuevo de periodo en `:app` ni en el detalle.

### Scenario 8: reglas de arquitectura y regresion
`RevisionDatosFlujoTest`, `ProcesadoFlujoTest`, `NavigationTest`, `ListadoGastosFlujoTest` (actualizado, ver tareas) y los tests de `:feature:escanear` siguen verdes con los **mismos testTags** `revision_*` en el flujo de escaneo.

## Repository Research

### Files Inspected
`AGENTS.md`, `CLAUDE.md`, `PROGRESS.md` (primeras 664 de 2545 lineas), `feature_list.json` (entrada detalle-ticket y evidencia de listado-gastos), `docs/domain-model.md` (lineas 18-45), `DESIGN.md` (greps de dialogo/editor/dock/mockups), `docs/specs/listado-gastos.md` y `docs/specs/edicion-lineas.md` (completas). `docs/specs/revision-datos.md` NO releida (se asume lo que consta en codigo y en edicion-lineas). Mockup 07: solo `code.html` por grep (cabecera "Ticket digitalizado" con flecha atras, boton "Ver foto original", chip de categoria, dock refresh/Editar/Guardar); mockup 08 `code.html` via la spec de edicion-lineas; los `screen.png` no se ven. Codigo: `RevisionDatosContent.kt` (completo; `ContenedorRecibo`, `CampoEditable`, `ChipsCategoria`, `EditorLineas`, `FilaLinea`, `DockEdicion`, todo `private` y con tags `revision_*`), `EscanearViewModel.kt` (completo; `onFechaEditada` fija `atStartOfDay`, `parsearFecha/parsearTotal` privados), `EscanearScreen.kt`, `RevisionFotoContent.kt` (`FotoGuardada` privada, `BitmapFactory.decodeFile(File(filesDir, ruta))` sin muestreo), `feature/escanear/build.gradle.kts` (SIN dependencia a `:feature:common`), `feature/gastos/build.gradle.kts` (ya con `:feature:common`, Hilt, lifecycle compose), `CategoriaUi.kt`, `feature/common/build.gradle.kts`, dominio `Ticket`, `DatosRevision`, `LineaEditable` (helpers puros, parseo y `aLineas`), `GuardarTicket`, `TicketRepository` (`delete`: `getById` -> `dataSource.delete` -> `photoFileStore.delete`, sin `try`), `TicketDataSource`, `PhotoFileStore`, `TicketDao` (`upsert` = update si `id != 0` + `deleteLineasOfTicket` + insert; `deleteById` cascada por FK), `PhotoFileStoreImpl` (relativa bajo `filesDir`, `delete` devuelve false si no existe), `app/.../TallyApp.kt`, `TallyRoute.kt`, `TicketPendienteScreen.kt`, `TicketRepositoryEntryPoint.kt`, `ListadoGastosFlujoTest.kt` (primeras 120 lineas; lineas 225-238 afirman `detalle_ticket_id`). No leidos: `GastosViewModel`/`ResumenViewModel` completos (solo grep: `seleccion` es un `MutableStateFlow` del VM, por tanto sobrevive mientras la entrada siga en la pila), `FakeTicketDataSource`, tests de `:framework:database`.

### Current Gaps
- La edicion vive solo en `:feature:escanear` y es `private`; `:feature:gastos` y `:feature:escanear` no pueden compartirla (los features no se conocen).
- No hay caso de uso de borrado ni de actualizacion; no hay `DatosRevision` a partir de un `Ticket` guardado; `onFechaEditada` pierde la hora.
- `TicketRepository.delete` propaga una excepcion de la foto despues de haber borrado la BD.
- No hay visor de foto de un ticket guardado (la foto de 05 es privada de escanear).

## Technical Approach

**Dominio (`:domain:expenses`, Kotlin puro).**
- `entities/EdicionDatos.kt`: `fun datosRevisionDe(ticket: Ticket): DatosRevision` (lineas con `lineasEditablesIniciales`) y reductores puros extraidos del VM de escanear: `DatosRevision.conFechaTexto(texto, conservarHora: Boolean): DatosRevision?`, `conTotalTexto`, `conLineaAnadida(id)`, `conLineaConcepto/Cantidad/Precio(id, texto)`, `sinLinea(id)` (null = texto invalido, se ignora, como hoy). `EscanearViewModel` pasa a delegar (`conservarHora = false`, comportamiento y tests intactos); el detalle usa `conservarHora = true`.
- `usecases/ActualizarTicket.kt` (`@Inject`, `suspend operator fun invoke(original: Ticket, datos: DatosRevision): Long`): `repository.save(original.copy(comercio, fechaEmision, total, categoria, lineas = datos.lineas.aLineas()))`; nunca toca `rutaFoto`/`fechaCaptura`/`motor`/`textoCrudoOcr`/`nota`. `usecases/BorrarTicket.kt` (`suspend operator fun invoke(id: Long)` -> `repository.delete(id)`). `usecases/ObtenerTicket.kt` (`suspend invoke(id): Ticket?`).
- `TicketRepository.delete`: envolver `photoFileStore.delete(...)` en `try/catch` que relanza `CancellationException` y traga el resto (huerfano aceptado). El orden DB-antes-foto no cambia.

**UI compartida (`:feature:common`, sin modulo nuevo; precedente D3 de listado-gastos).** Mover de `RevisionDatosContent.kt` y hacer publicos: `FormularioTicketEditable(datos, callbacks de cabecera y lineas, modifier)` (= `ContenedorRecibo` + `EditorLineas` y sus privados `CampoEditable`, `ChipsCategoria`, `FilaLinea`, `BotonAnadirLinea`, `SeparadorDiscontinuo`) y `DockEdicion(lineas, textoGuardar, guardarHabilitado, onDescartar, onGuardar)`; `BadgeMotor(motor)` y `FotoOriginal(rutaFoto, modifier)` (decodifica en `Dispatchers.IO` con `inSampleSize` para no cargar el JPEG completo; devuelve estados `Cargando/Imagen/Ausente`). Tags `revision_*` se conservan literalmente. `:feature:escanear` gana `implementation(project(":feature:common"))`; `RevisionDatosContent` queda como composicion fina (badge + formulario + error + dock). `feature/common/build.gradle.kts` necesita `libs.androidx.lifecycle...` solo si el implementador lo exige (sin versiones literales).

**Detalle (`:feature:gastos`, mismo modulo del listado; sin modulo nuevo, D2).** `TicketDetalleScreen.kt` (`hiltViewModel()`, params `ticketId`, `onBack`), `TicketDetalleViewModel.kt` (`@HiltViewModel`; entradas `MutableStateFlow`: id (de `SavedStateHandle`/argumento), `datos` editados, `orden` Guardar/Borrar con intento; estado por cadena de operadores `flatMapLatest`, sin `load()`; `TicketDetalleUiState = Cargando | NoExiste | Datos(ticket, datos, hayCambios, guardando, borrando, error, hecho)`), `TicketDetalleState.kt` (visual: `mostrarConfirmarBorrado`, `mostrarFoto`, scroll). Lectura inicial con `ObtenerTicket` una sola vez (D6). Orden de operaciones: `ActualizarTicket`/`BorrarTicket` dentro de `runCatchingLanzandoCancelacion` (misma politica que escanear; `CancellationException` siempre relanzada). Al terminar bien: `hecho = true` -> `LaunchedEffect` llama `onBack()` una vez. Estructura: barra superior (flecha atras `detalle_atras`, titulo "Detalle del ticket", icono eliminar `detalle_eliminar`) + `BadgeMotor` + boton "Ver foto original" (`detalle_ver_foto`, mockup 07) que expande la `FotoOriginal` en un dialogo a pantalla completa (`detalle_foto`) o muestra `detalle_foto_ausente` + formulario + dock ("Descartar"/"Guardar cambios").

**Compatibilidad de la ruta.** `TallyApp`: el `composable(TallyRoute.Ticket.route)` llama a `TicketDetalleScreen(ticketId, onBack = { navController.popBackStack() })`; `TicketPendienteScreen.kt` se elimina. La barra inferior no se compone cuando la ruta activa es `ticket/{ticketId}` (D5). `ListadoGastosFlujoTest` pasa a afirmar `pantalla_detalle_ticket` (misma raiz) y un tag nuevo con el id en lugar de `detalle_ticket_id`.

**Hilt/di.** Sin `di.kt` nuevo (todo `@Inject`/`@HiltViewModel`); `framework/` sin cambios de codigo.

## Expected File Changes

- `domain/expenses/.../entities/EdicionDatos.kt`, `usecases/{ActualizarTicket,BorrarTicket,ObtenerTicket}.kt` — create; `data/TicketRepository.kt` — modify (try/catch de la foto).
- `domain/expenses/src/test/...`: `EdicionDatosTest`, `ActualizarTicketTest`, `BorrarTicketTest`, ampliar `TicketRepositoryTest` (cascada y foto que lanza) — create/modify. `FakePhotoFileStore` (`:test:unit`) — modify solo si hace falta modo "lanza"/"falla".
- `feature/common/.../FormularioTicketEditable.kt`, `FotoOriginal.kt`, `BadgeMotor.kt` (+ dock) — create (movido); `feature/escanear/.../RevisionDatosContent.kt`, `EscanearViewModel.kt` — modify (delegar); `feature/escanear/build.gradle.kts` — modify.
- `feature/gastos/.../TicketDetalleScreen.kt`, `TicketDetalleViewModel.kt`, `TicketDetalleState.kt`, `TicketDetalleViewModelTest.kt` — create.
- `app/.../navigation/TallyApp.kt` — modify; `TicketPendienteScreen.kt` — delete. `app/src/androidTest/.../gastos/DetalleTicketFlujoTest.kt` — create; `ListadoGastosFlujoTest.kt` — modify (lineas 225-238).
- `docs/architecture.md` (ruta `ticket/{id}`, formulario en common, borrado) y `DESIGN.md` (detalle + dialogo) — update; `docs/evidence/detalle-ticket/` — create. `PROGRESS.md`/`feature_list.json`: los actualiza el orquestador/implementador con evidencia. `CONTEXT.md`, `docs/domain-model.md`, `AGENTS.md`, `CONSTRAINTS.md`, `ARCHITECTURE.md`: not needed. `settings.gradle.kts`, `libs.versions.toml`, `framework/`, `schemas/`: sin cambios.

## Visual Design Impact

UI: si. Fuentes: mockup 07 (cabecera, "Ver foto original", recibo, dock) + mockup 08 (editor y dock) + `DESIGN.md`. Desviaciones deliberadas: sin boton "Editar" (la pantalla es siempre editable, como `revision-datos` U4), dock Descartar/Guardar cambios, icono eliminar en la barra superior en paleta `error`, dialogo M3 con forma 28 dp. Importes con `tnum`, tactil >= 44/48 dp, copy en espanol. Capturas en `docs/evidence/detalle-ticket/`: detalle con foto, dialogo de borrado, foto ausente, inexistente (revision visual humana pendiente: el agente no ve imagenes).

## Implementation Plan

1. TDD dominio (reductores, casos de uso, `TicketRepository.delete` con foto que lanza); `:test:unit`.
2. Mover el formulario/foto/badge a `:feature:common`, delegar en escanear; `:feature:escanear:testDebugUnitTest` y `RevisionDatosFlujoTest` verdes (sin cambio funcional).
3. `TicketDetalleViewModel` + tests JVM; luego la pantalla.
4. Cablear `:app`, quitar el placeholder, actualizar `ListadoGastosFlujoTest`.
5. E2E `DetalleTicketFlujoTest` en emulator-5556, regresion, capturas, docs.

## Implementation Tasks

- [ ] T1 `EdicionDatos` + test (texto invalido = null/ignorado, `conservarHora` true/false, `datosRevisionDe` con 0 y N lineas, ids estables).
- [ ] T2 `ActualizarTicket` + test (mismo id; conserva `rutaFoto/fechaCaptura/motor/textoCrudoOcr/nota`; lineas editadas con `aLineas`; total no derivado de lineas; hora conservada).
- [ ] T3 `BorrarTicket`/`ObtenerTicket` + `TicketRepositoryTest`: borra ticket, lineas y llama a `photoFileStore.delete(ruta)`; foto que devuelve false o lanza -> el ticket sigue borrado y no hay excepcion; id inexistente no-op; `CancellationException` de la foto se relanza.
- [ ] T4 Mover UI compartida a `:feature:common` y delegar en escanear (`EscanearViewModelTest` 100 % verde sin editar aserciones).
- [ ] T5 `TicketDetalleViewModel` + test (Cargando -> Datos; NoExiste; editar marca `hayCambios`; descartar; guardar ok -> `hecho`; guardar falla -> `error` y datos conservados; borrar ok -> `hecho` sin pasar por NoExiste; borrar falla -> `error`; doble borrado ignorado).
- [ ] T6 `TicketDetalleScreen` (barra, foto/ausente, formulario, dock, dialogo) + `TicketDetalleState`.
- [ ] T7 `TallyApp` (ruta real, bottom bar oculta en `ticket/..`), eliminar placeholder, ajustar `ListadoGastosFlujoTest`.
- [ ] T8 E2E `DetalleTicketFlujoTest` (ver Verificacion), `DESIGN.md`, `docs/architecture.md`.
- [ ] T9 Verificacion completa, revision de reglas, evidencia.

## Verification Plan

- `.\gradlew.bat assembleDebug testDebugUnitTest --console=plain` (o `bash init.sh`) verde; `--rerun` y conteo JUnit por clase en `:domain:expenses:test`, `:feature:gastos:testDebugUnitTest`, `:feature:escanear:testDebugUnitTest`, `:feature:common:testDebugUnitTest`, `:test:unit:test`.
- E2E en **emulator-5556** (5554 offline): `:app:assembleDebug :app:assembleDebugAndroidTest`, instalar ambos APK y `adb -s emulator-5556 shell am instrument -w -e class com.robertomr.tally.gastos.DetalleTicketFlujoTest <paquete>.test/androidx.test.runner.AndroidJUnitRunner`. **Nunca** `connected*` ni `pm clear`. No leer el token HF.
- El emulador tiene tickets residuales (0,00 EUR, 18+): el test usa linea base (como `ListadoGastosFlujoTest`), siembra con `TicketRepositoryEntryPoint` tickets de comercio unico (hoy) **y un JPEG real escrito en `filesDir/fotos/detalle_e2e_<uuid>.jpg`** desde `targetContext`, referenciado en `rutaFoto`; `@After` borra por repositorio lo que siga existiendo y el fichero si quedo.
  - A (abrir y mostrar): tocar `ticket_<id>` en Gastos y en Resumen; comprobar comercio/total/N lineas/badge y que existe la foto (`detalle_foto` tras `detalle_ver_foto`); Atras vuelve con el mismo periodo.
  - B (editar): cambiar comercio y total, anadir/borrar una linea, Guardar; en repositorio (`getById`) comprobar mismo id, hora conservada, lineas exactas; en Gastos aparece el nuevo comercio sin recargar.
  - C (borrar): `detalle_eliminar` -> dialogo -> Cancelar (no cambia nada) -> Eliminar; comprobar `getById == null`, ticket ausente en Gastos y fichero ausente (`File(filesDir, ruta).exists() == false`). Comprobacion adicional **fuera del test** con `adb -s emulator-5556 shell run-as com.robertomr.tally ls -l files/fotos` antes y despues de borrar (un fichero menos).
  - D (foto ausente e inexistente): ticket sembrado con `rutaFoto` de fichero inexistente muestra `detalle_foto_ausente`; borrar el ticket por repositorio antes de abrirlo (o ruta con id borrado) muestra `detalle_no_existe` y "Volver" sale.
  - Regresion: `ListadoGastosFlujoTest`, `ResumenTotalFlujoTest`, `GraficaDonutFlujoTest`, `PeriodoSelectorFlujoTest`, `RangoPersonalizadoFlujoTest`, `NavigationTest`, `RevisionDatosFlujoTest`, `ProcesadoFlujoTest`; omitir `*RealTest`. Los skips por `assumeTrue` ya conocidos se anotan, no se ocultan.
- Revision: 0 imports de Android en `domain/**`; `:feature:gastos` y `:feature:escanear` sin dependencia entre si (solo `:feature:common`); sin `NavController` en features; sin modulos nuevos ni versiones literales; `git diff` vacio en `framework/`, `schemas/`, `build-logic/`, `libs.versions.toml`, `init.sh`; tags `revision_*` intactos.

## Evidence To Capture

Conteo JUnit por clase; salida de `am instrument` en 5556; salida de `run-as ls -l files/fotos` antes/despues; capturas (detalle, dialogo, foto ausente, inexistente); decisiones D1-D7 resueltas en `PROGRESS.md`.

## Validator Checklist

- [ ] Detalle desde Gastos y Resumen muestra foto, campos y lineas del ticket correcto.
- [ ] Editar persiste el mismo ticket (id, hora, motor, foto intactos) y el listado se actualiza solo.
- [ ] Borrar exige confirmacion; elimina fila, lineas y fichero (filesDir verificado); Cancelar no borra.
- [ ] Fallo de foto no convierte el borrado en error; fallo de BD deja el ticket intacto y avisa.
- [ ] Estados cargando, inexistente y foto ausente sin crash.
- [ ] Periodo/rango del listado se conserva al volver.
- [ ] Edicion reutilizada (sin duplicar el formulario); features sin dependencias cruzadas; sin modulos/versiones nuevas; regresion verde.

## Open Decisions (defaults si no hay respuesta)

- **D1 Como reutilizar la edicion.** Default: mover `FormularioTicketEditable`/dock/foto/badge a `:feature:common` (precedente `PeriodoSelector`/`FilaTicket`) y extraer los reductores de edicion a dominio. Alternativa B: pantalla de detalle dentro de `:feature:escanear` con su propia ruta `ticket/{id}` (cero moverse de componentes, pero un modulo de camara aloja el detalle y `:app` sigue conectando). Alternativa C: duplicar (rechazada).
- **D2 Donde vive el detalle.** Default: `:feature:gastos` (junto al listado que lo abre). Alternativa: `:feature:escanear` (B). Un `:feature:detalle` nuevo exigiria confirmacion (no se crea).
- **D3 Atras con cambios sin guardar.** Default: descarta sin preguntar (igual que Descartar en `revision-datos`). Alternativa: dialogo "Descartar cambios?" si `hayCambios`.
- **D4 Visor de foto.** Default: dialogo a pantalla completa con la foto ajustada (`ContentScale.Fit`), sin zoom. Alternativa: foto fija en cabecera del detalle o zoom con gestos.
- **D5 Barra inferior en el detalle.** Default: oculta en `ticket/{id}` (el dock ya ocupa el borde y evita dos barras). Alternativa: mantenerla (resalta Resumen por el `else` actual aunque se venga de Gastos).
- **D6 Lectura del ticket.** Default: `getById` una vez (sin tocar `TicketDataSource`/Room, el formulario no debe reescribirse bajo los dedos). Alternativa: `observeById` Flow en DAO/DataSource/Fake, que permitiria detectar borrado externo en vivo.
- **D7 Fecha editada.** Default: la hora de emision se conserva al cambiar la fecha en el detalle (en escaneo se mantiene el comportamiento actual `atStartOfDay`).
- **D8 Tras Guardar cambios.** Default: `onBack()` a la pantalla de origen. Alternativa: quedarse en el detalle con confirmacion.

## Riesgos

- Mover ~500 lineas de Compose puede romper `RevisionDatosFlujoTest`: conservar tags y orden de composicion; ejecutar ese E2E antes de seguir.
- `CampoEditable` guarda su texto con `rememberSaveable(valorInicial)`; "Descartar" debe re-crear el formulario (por ejemplo `key(versionDescartar)`) para que los campos vuelvan al valor guardado.
- Primer E2E con foto real depende de que `BitmapFactory` decodifique el JPEG sembrado; usar un JPEG minimo valido, no bytes arbitrarios.
- Foto huerfana si falla el borrado del fichero: aceptado, sin limpieza posterior (fuera de alcance).
- Subir el id inexistente via deep link no existe; "inexistente" solo se alcanza por borrado previo o ruta obsoleta restaurada.
