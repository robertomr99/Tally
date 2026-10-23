# Spec: seed-demo — Datos de demo sembrados (solo build debug)

## Source Feature

- id: `seed-demo` (area: demo), status `not_started`
- depends_on: `persistencia-room`, `grafica-donut` (ambas accepted segun el historial; el implementer lo confirma en `feature_list.json`).
- Comportamiento: en build debug, un control siembra tickets de varias categorias y periodos (mas un ticket "malo" para demostrar la correccion) para la demo en directo.
- Verificacion de la feature: sembrado idempotente; la demo se hace sin red con los datos sembrados; no activo en release.
- Origen: `docs/build-brief.md` (Validation Plan: "Datos de demo sembrados por si la IA en directo falla" y ticket deliberadamente "malo" para reintento/correccion manual).

## Goal y Non-goals

Goal: un boton en la seccion de depuracion de Ajustes (debug) que inserta ~12 tickets falsos, reconocibles y reversibles, y otro que los quita sin tocar nada real.

Non-goals:
- NUNCA borrar ni modificar un ticket real. Solo se tocan filas con la marca de demo.
- Sin migracion de Room, sin tablas ni columnas nuevas, sin modulos Gradle nuevos, sin tocar `build-logic/` ni `gradle/libs.versions.toml`.
- Sin red, sin motores (no pasa por router ni deja rastro en el log de extracciones), sin fotos reales (`rutaFoto = ""`).
- Nada de esto existe en release: ni UI, ni generador, ni ViewModel.
- No es una pantalla de producto ni cambia el comportamiento de listado/resumen/donut.

## Job story

Como autor que hace la demo en directo, quiero poblar la app con gastos falsos de varias categorias y periodos con un toque (y quitarlos despues), para ensenar donut, listado, filtros y correccion manual aunque la IA o la red fallen, sin perder mis tickets reales.

## Acceptance scenarios

1. Sembrar: Given build debug y Ajustes abierto, When pulso "Sembrar datos de demo" y confirmo, Then se insertan los tickets de demo y la seccion muestra "N tickets sembrados" (N = tamano del conjunto, 13); Gastos muestra donut y listado con varias categorias en Hoy, Semana (anterior), Mes (anterior).
2. Idempotencia: Given ya sembrado, When repito, Then no se crea ninguno y el mensaje dice "Ya estaban los N tickets de demo" (si falta alguno, p. ej. el usuario borro uno, se repone solo ese y el mensaje es "k sembrados, resto ya estaban").
3. Quitar: When pulso "Quitar datos de demo" y confirmo, Then se borran exactamente los tickets con marca de demo; los tickets reales (sin marca) quedan intactos en numero y contenido; mensaje "N tickets de demo quitados" (o "No habia datos de demo").
4. Aislamiento: Given tickets reales en la BD (incl. mismo comercio/fecha que uno de demo), When siembro y quito, Then ningun ticket real cambia (id, campos y lineas iguales).
5. Variedad: el conjunto cubre las 6 categorias, formas EFECTIVO/TARJETA/NO_INDICADA, con y sin lineas, 2 gastos manuales (`motor = null`, `rutaFoto = ""`) y motores LOCAL y NUBE.
6. Coherencia de importes: con lineas, `total = totalDeLineas` (suma de cantidad x precio, redondeada a centimos); en EFECTIVO con entregado, `cambio = entregado - total` (misma regla que `normalizarPago`); TARJETA/NO_INDICADA sin entregado ni cambio.
7. Ticket "malo": hay un ticket (motor NUBE) con comercio "Comercio ilegible", total 0,00, categoria OTROS, sin lineas, `textoCrudoOcr` ilegible, sin foto; se abre en Detalle y se puede corregir con Editar (comercio, total, categoria, pago) y guardar.
8. Fechas relativas: las fechas se calculan desde "hoy" (no fijas): hoy, hoy-1, hoy-2/3, semana anterior, mes anterior; repetir otro dia da el mismo conjunto de claves (idempotencia por clave, no por fecha).
9. Release: en build release la seccion no existe y el codigo de demo no esta en el artefacto.
10. Accesibilidad/responsive: la seccion cabe a 360 dp, con fontScale 2,0 los botones y mensajes siguen legibles y desplazables; en tablet respeta `ColumnaCentrada`.

## Repository research (inspeccionado)

- `AGENTS.md` (reglas de modulos, `di.kt`, capa de presentacion, "no crear modulos sin preguntar").
- `feature_list.json` entrada `seed-demo` (lineas ~717-730); `docs/build-brief.md` lineas 70-78; `docs/specs/log-extracciones.md` (patron de seccion debug).
- `feature/ajustes/.../AjustesScreen.kt`: `esDebug = applicationInfo.flags and FLAG_DEBUGGABLE`; `AjustesContent(mostrarDiagnostico)` compone `SeccionDiagnostico` (tag `diagnostico_extracciones`) al final de la columna con scroll, dentro de `TarjetaSeccion`/`TituloSeccion`. `AjustesViewModel` (`@HiltViewModel`, `combine`), `AjustesState` (estado visual con setters privados), `AjustesViewModelTest` construye el VM a mano (no cambiar su constructor evita romperlo).
- `feature/ajustes/build.gradle.kts`: plugins `tally.android.library`, `tally.android.compose`, `tally.hilt`; depende de `:domain:expenses`, `:feature:common`, `:test:unit` en test.
- Debug vs release hoy: no hay `BuildConfig` en ningun modulo. Precedente de codigo debug-only por source set: `app/src/debug/java/.../appcheck/AppCheckDebugInitializer.kt` + `app/src/debug/AndroidManifest.xml`. Precedente de gate runtime: `FLAG_DEBUGGABLE` (Ajustes, `RevisionFotoContent`).
- Dominio: `Ticket` (campos `nota`, `textoCrudoOcr`, `motor?`, `rutaFoto`, `formaPago`, `entregado`, `cambio`), `Linea`, `LineaEditable` + `totalDeLineas`, `Pago.kt` (`normalizarPago`, `redondearCentimos` es `internal`), `Categoria` (6), `FormaPago` (3), `Motor` (LOCAL, NUBE), `GuardarTicket`/`GuardarGastoManual` (ponen `nota = null`, `fechaCaptura = now`), `TicketRepository` (`save`, `delete`, `getById`, `observeByDateRange`; NO hay "listar todos" ni "borrar por marca").
- Room: `TicketLocalDataSource` guarda `nota` y `textoCrudoOcr` tal cual; motor null -> columna "MANUAL"; `observeByDateRange(from,to)` es por fecha de emision en UTC. `TallyDatabase` ya esta en version con `extracciones_log`; no se toca.
- E2E existentes: `app/src/androidTest/.../ajustes/LogExtraccionesFlujoTest.kt` (abre Ajustes con `boton_ajustes`, comprueba `diagnostico_extracciones`).
- No inspeccionado: `ActualizarTicket` (si conserva `nota` al editar; el implementer lo verifica), `Periodo`/`ObtenerPeriodo` (inicio de semana), `TarjetaSeccion`, si Detalle muestra `nota`, ni `docs/architecture.md`.

## Decisiones (con default)

**D1 Marca de demo (la mas importante).** `nota = "tally-demo:v1:<clave>"` (p. ej. `tally-demo:v1:D03`). Razon: `nota` siempre es `null` en tickets reales (nota es non-goal), ya se persiste, no requiere migracion ni columna, no altera comercio/categoria (la demo se ve limpia) y es filtrable en memoria. Idempotencia = existe un ticket con esa `nota` exacta. Alternativas descartadas: sufijo en comercio (ensucia la demo y se pierde al editar), prefijo en `textoCrudoOcr` (los manuales no lo tienen), columna nueva (migracion). Verificar antes que Detalle/Editar no muestra ni pisa `nota`; si `ActualizarTicket` la borra al editar, el ticket corregido deja de ser "de demo" (aceptable: pasa a ser un dato del usuario, y "Quitar" no lo borrara; documentarlo en la UI: "los tickets de demo que edites se conservan").

**D2 Aislamiento en el borrado.** Nunca borrar por rango ni por comercio. "Quitar" lista tickets con `observeByDateRange(2000-01-01, 2100-12-31).first()` (sin cambiar DAO/repositorio), filtra `nota?.startsWith("tally-demo:v1:") == true` y llama `TicketRepository.delete(id)` por cada uno. `rutaFoto` vacio: confirmar que `PhotoFileStore.delete("")` es no-op (los gastos manuales ya se borran asi).

**D3 Donde vive el codigo: source set `debug` de `:feature:ajustes`, con stub en `release`.** Es el mecanismo ya usado (`app/src/debug`). Estructura: `feature/ajustes/src/debug/java/com/robertomr/tally/feature/ajustes/demo/` con generador puro, caso de uso, ViewModel y composable; `feature/ajustes/src/release/java/.../demo/` con la misma firma publica del composable vacio. `AjustesContent` llama a `SeccionDatosDemo()` sin parametros (no cambia la firma ni el VM de Ajustes ni `AjustesViewModelTest`). Desviacion a confirmar contra AGENTS.md ("casos de uso -> `domain/<area>/usecases/`"): aqui el caso de uso va en el source set debug del feature para que NO exista en release; la alternativa (dominio + gate `FLAG_DEBUGGABLE`) deja codigo de demo en el APK release. Si el usuario prefiere la alternativa, el resto del spec no cambia.
Hilt: `@HiltViewModel DatosDemoViewModel` en `src/debug` (recibe `TicketRepository`; no hay `di.kt` nuevo porque no hay modulo Hilt nuevo). La seccion obtiene el VM con `hiltViewModel()`. Se mantiene ademas el gate `mostrarDiagnostico` existente.

**D4 Reloj.** El generador recibe `hoy: LocalDate` (parametro); el VM pasa `LocalDate.now()`. `fechaCaptura = fechaEmision` del dia de siembra (`hoy.atTime(...)`) para ser determinista.

**D5 Conjunto (claves estables `D01..D12`, hora de emision fija, `hoy`=H, `L`=lunes de la semana actual segun `Periodo`, `M`=mes anterior).** Comercios ficticios.
- D01 Super Barrio, ALIMENTACION, H, TARJETA, NUBE, 4 lineas (total = suma).
- D02 Cafe La Esquina, ALIMENTACION, H-1, EFECTIVO, LOCAL, lineas cafe 1,60 + tostada 2,40 = 4,00; entregado 5,00, cambio 1,00.
- D03 Gasolinera Norte, TRANSPORTE, H-2, TARJETA, NUBE, sin lineas, 52,30.
- D04 Farmacia Central, SALUD, H-3, EFECTIVO, LOCAL, lineas 8,45 + 12,90 = 21,35; entregado 30,00, cambio 8,65.
- D05 Cine Plaza, OCIO, L-6 (semana anterior), TARJETA, NUBE, sin lineas, 17,50.
- D06 Ferreteria Hogar, HOGAR, L-4, NO_INDICADA, NUBE, 2 x bombilla 3,95 + cinta 4,50 + tornillos 2,85 = 15,25.
- D07 Supermercado Sur, ALIMENTACION, L-3, EFECTIVO, LOCAL, sin lineas, 63,18; entregado 70,00, cambio 6,82.
- D08 Taxi Ciudad, TRANSPORTE, M dia 12, TARJETA, NUBE, sin lineas, 14,60.
- D09 Restaurante El Parque, OCIO, M dia 20, EFECTIVO, LOCAL, 2 x menu 12,50 + 2 x bebida 2,20 + 2 x cafe 1,30 = 32,00; entregado 40,00, cambio 8,00.
- D10 Clinica Dental, SALUD, M dia 3, NO_INDICADA, NUBE, sin lineas, 45,00.
- D11 (manual) Trastero mensual, HOGAR, H-4, EFECTIVO, `motor=null`, sin lineas, 35,00, entregado 35,00, cambio 0,00.
- D12 (manual) Propina, OTROS, H-1, NO_INDICADA, `motor=null`, 5,00.
- D13 (malo) Comercio ilegible, OTROS, H-1, NO_INDICADA, NUBE, 0,00, sin lineas, `textoCrudoOcr` tipo "T1ENd@ ?? 0 ### 1?.4O  TOT4L ..." (sin datos personales). => el conjunto real es de 13; los mensajes usan `conjunto.size`, no un numero fijo.
Si H-k cae en la semana anterior (hoy lunes/martes) no importa: el generador no promete en que periodo cae cada uno, solo que hay datos en Hoy, Semana y Mes (D01 siempre en Hoy; M siempre en Mes anterior; L-x en Semana anterior). El implementer ajusta los offsets si `Periodo` define la semana distinta.

**D6 Pago.** El generador construye los pagos pasando por `normalizarPago` (o los valores ya coherentes) y un test JVM comprueba la invariante; no se reimplementa la regla.

**D7 UI.** `TarjetaSeccion` "Datos de demo" tras el diagnostico: texto de ayuda ("Solo en builds de depuracion. Crea gastos falsos para la demo; los tickets reales no se tocan."), contador "Tickets de demo: n" (observado), botones `Button` "Sembrar datos de demo" y `OutlinedButton` "Quitar datos de demo". Cada uno abre `AlertDialog` de confirmacion (el de quitar con boton en paleta `error`, como "Eliminar modelo"); mientras trabaja, botones deshabilitados. Mensaje de resultado en `Text` con `liveRegion` (anuncio TalkBack). Estado visual del dialogo en un `DatosDemoState`/`remember` local en el mismo paquete; sin `load()` imperativo en el VM: el contador es `Flow` de `observeByDateRange(...)` filtrado, y el resultado es un `MutableStateFlow` de entrada.
Tags: `seccion_datos_demo`, `demo_contador`, `demo_sembrar`, `demo_quitar`, `demo_confirmar_sembrar`, `demo_confirmar_quitar`, `demo_resultado`. Botones con `fillMaxWidth` o `FlowRow` para 360 dp y fontScale 2,0 (sin altura fija ni texto truncado).

## Expected file changes

- Nuevos (debug): `feature/ajustes/src/debug/java/com/robertomr/tally/feature/ajustes/demo/` -> `GeneradorDatosDemo.kt` (puro: `fun generarTicketsDemo(hoy: LocalDate): List<Ticket>`, `const MARCA_DEMO`), `SembrarDatosDemo.kt` + `QuitarDatosDemo.kt` (o un solo `DatosDemo.kt` con ambos casos de uso, `operator fun invoke`), `DatosDemoViewModel.kt`, `SeccionDatosDemo.kt`.
- Nuevo (release): `feature/ajustes/src/release/java/.../demo/SeccionDatosDemo.kt` (composable vacio, misma firma `internal fun SeccionDatosDemo()`).
- Editado: `AjustesScreen.kt` (una llamada `SeccionDatosDemo()` dentro de `if (mostrarDiagnostico)`, tras `SeccionDiagnostico`).
- Tests: `feature/ajustes/src/testDebug/java/.../demo/` (`GeneradorDatosDemoTest`, `DatosDemoUseCasesTest`, `DatosDemoViewModelTest`) con un `TicketRepository` sobre un `TicketDataSource` falso en memoria (reutilizar el de `:test:unit` si existe; si no, crearlo alli); `app/src/androidTest/java/com/robertomr/tally/ajustes/SembrarDemoFlujoTest.kt`.
- Sin cambios en `framework/*`, `build-logic/`, `libs.versions.toml`, `settings.gradle.kts`. Si Gradle exige declarar `debugImplementation`/`releaseImplementation` algo, avisar antes (no se prevee: usa lo ya declarado).

## Visual design impact

Solo debug: reutiliza `TarjetaSeccion`, `TituloSeccion`, botones y `AlertDialog` M3 de Ajustes; sin artefacto Stitch. No cambia DESIGN.md.

## Durable documentation impact

- `AGENTS.md`: not needed (si se acepta D3, basta una nota en el spec/PROGRESS de la desviacion).
- `docs/build-brief.md`: update minimo — marcar en Validation Plan que los datos de demo son el control de Ajustes (debug) y como usarlo.
- `docs/architecture.md`: update — pie sobre codigo debug-only por source set en `:feature:ajustes` y marca `nota = tally-demo:v1:*`.
- `docs/domain-model.md`: update de una linea — `nota` reservada por la marca de demo en builds debug.
- `docs/risks-and-open-questions.md`: not needed. `ARCHITECTURE.md`/`CONSTRAINTS.md` raiz: no existen.
- `docs/evidence/seed-demo/README.md`: crear (capturas y comandos). `PROGRESS.md` y `feature_list.json`: los actualiza el orquestador/implementer con evidencia (el planner no puede).

## Implementation plan / tasks

1. Verificar los puntos no inspeccionados (`ActualizarTicket` y `nota`, Detalle no pinta `nota`, `PhotoFileStore.delete("")`, inicio de semana de `Periodo`).
2. Generador puro + tests JVM (conjunto, claves unicas, coherencia de pagos, total = suma de lineas, fechas relativas con `hoy` inyectado, mismas claves otro dia).
3. Casos de uso sembrar/quitar + tests con repositorio en memoria (idempotencia, reposicion parcial, quitar no toca tickets reales con mismo comercio y fecha, resultado con contadores).
4. `DatosDemoViewModel` + tests (Turbine, `CoroutineTestRule`): contador, resultado, doble toque no duplica.
5. UI debug + stub release + enlace en `AjustesContent`.
6. Verificacion base y E2E en emulador; capturas.

## Verification plan

- Base: `gradlew.bat assembleDebug testDebugUnitTest --console=plain` (o `bash init.sh`).
- Release: `gradlew.bat :feature:ajustes:compileReleaseKotlin :app:assembleRelease` (sin firmar si el proyecto lo permite; si `assembleRelease` exige firma/Firebase, usar solo `compileReleaseKotlin` y dejarlo escrito). Comprobar con busqueda en `feature/ajustes/build/tmp/kotlin-classes/release` y, si hay APK, `unzip -l`/busqueda de cadenas, que no aparece `GeneradorDatosDemo`, `DatosDemoViewModel` ni la cadena `tally-demo`. Conservar la salida como evidencia.
- E2E (aditivo) `SembrarDemoFlujoTest` en `emulator-5556`, SOLO con `ANDROID_SERIAL=emulator-5556` o `adb -s emulator-5556`; `installDebug` NUNCA sin `ANDROID_SERIAL`; no dispositivos fisicos; nunca `connected*` ni `pm clear`: abre Ajustes, scroll a `seccion_datos_demo`, sembrar+confirmar (resultado "sembrados"), repetir (resultado "Ya estaban"), contador = N, abre Gastos y comprueba que hay tickets de demo; en `@After` ejecutar "Quitar datos de demo" (o el caso de uso via Hilt) para dejar la BD como estaba, y comprobar contador 0. El test NO debe borrar tickets reales ni depender de BD vacia (compara conteo de no-demo antes y despues).
- Manual con capturas (emulator-5556): Ajustes con la seccion a 360 dp y fontScale 2,0 (`adb -s emulator-5556 shell settings put system font_scale 2.0`, restaurar a 1.0), tablet (`wm size`/AVD tablet si existe, restaurar), Gastos con donut tras sembrar, Detalle del ticket malo y su correccion. Sin leer el token de Hugging Face; sin red (modo avion en el emulador) para el criterio "sin red".
- Unit: ver tareas 2-4.

## Evidence to capture

Salida de unit tests y de `am instrument` (E2E), salida/inspeccion de release sin simbolos de demo, capturas en `docs/evidence/seed-demo/`, y el informe del implementer DEBE decir explicitamente que ve el usuario y donde: "Ajustes -> al final, debajo de 'Diagnostico de extracciones', tarjeta 'Datos de demo' con contador y los botones 'Sembrar datos de demo' / 'Quitar datos de demo'; solo en build debug; tras sembrar, en Gastos se ven N tickets en varias categorias y periodos (cambiar Dia/Semana/Mes), y el ticket 'Comercio ilegible' se abre en Detalle para corregirlo". Tambien los pasos de uso en la demo.

## Validator checklist

- [ ] Marca `tally-demo:v1:<clave>` en `nota`; nada borra/modifica tickets sin esa marca (test explicito con tickets reales de mismo comercio/fecha).
- [ ] Idempotencia verificada (JVM y E2E) y reposicion parcial.
- [ ] Importes coherentes: total = suma de lineas, cambio = entregado - total, TARJETA/NO_INDICADA sin entregado/cambio.
- [ ] Cubre 6 categorias, 3 formas de pago, con/sin lineas, 2 manuales (`motor=null`, `rutaFoto=""`), ticket malo corregible.
- [ ] Codigo de demo solo en `src/debug` (+ stub en `src/release`); evidencia de release sin el control.
- [ ] Sin cambios en framework, Room, build-logic, libs, settings; sin modulos nuevos; sin secretos ni red.
- [ ] Dialogos de confirmacion, mensajes de resultado, tags, accesibilidad; capturas 360 dp, fontScale 2,0 y tablet.
- [ ] E2E en `emulator-5556` con limpieza en `@After`; nunca `connected*`/`pm clear`/dispositivos fisicos; informe del implementer indica que ve el usuario y donde.

## Unknowns

- Confirmar D3 (caso de uso en source set debug vs dominio con gate runtime) por la regla de ubicacion de casos de uso de AGENTS.md.
- Si `ActualizarTicket` conserva `nota` (afecta solo al mensaje de ayuda y a que "Quitar" no borre tickets editados).
- Si `assembleRelease` es viable sin firma/`google-services` en este entorno; si no, la evidencia es `compileReleaseKotlin` + inspeccion de clases.
- Inicio de semana en `Periodo` para fijar los offsets `L-x`.
- Tamano exacto del conjunto (13 en este spec; los mensajes usan `conjunto.size`).
