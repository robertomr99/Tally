# Spec: log-extracciones — Registro local de extracciones

## Source Feature

- id: `log-extracciones` (area: observabilidad), status `not_started`
- depends_on: `extraccion-nube` (accepted). Router, reintento con otro motor y selector de motor ya existen.
- Comportamiento: cada extraccion queda registrada localmente (motor, duracion, resultado, longitud del texto) y es consultable en debug para la memoria del curso.
- Verificacion de la feature: tras varias extracciones el log registra motor/duracion/resultado correctos; unit test del registrador con dobles.

## Goal y Non-goals

Goal: metadatos de cada llamada real a un motor de extraccion, persistidos en Room, con consulta/exportacion solo en builds debuggables.

Non-goals:
- NO se guarda texto crudo, campos del ticket (comercio, total, lineas), foto, prompt, respuesta del modelo, mensajes de excepcion ni token (privacidad, ver D3).
- Sin telemetria remota, sin analitica, sin Crashlytics.
- Sin modulos Gradle nuevos, sin red, sin cambios de comportamiento de la extraccion (el decorador es transparente).
- No es una pantalla de producto: UI solo en debug, sin diseno nuevo.
- No cambia `EscanearViewModel`, ni los casos de uso `ExtraerTicket`/`ExtraerTicketConMotor`, ni `EngineRouter` (salvo el comentario obsoleto "punto de extension futuro" de `ExtraerTicket`).

## Job story

Como autor de la memoria del curso, quiero ver y exportar cuanto tarda cada motor, con que resultado y con que tamano de entrada, para comparar Local vs Nube con datos reales (ADR 0002: misma entrada) sin exponer datos de los tickets.

## Acceptance scenarios

1. Exito por router: Given preferencia Auto y motor elegido X, When se extrae un texto no vacio, Then se anade 1 registro con `motor=X`, `origen=ROUTER`, `resultado=EXITO`, `longitudTexto=texto.length`, `duracionMs` = tiempo medido con el reloj inyectado y `marcaTiempo` = inicio (epoch ms).
2. Reintento explicito: Given un resultado/error en pantalla, When el usuario pulsa "Probar con {otro}" (o Reintentar, que usa el mismo motor por la ruta explicita), Then 1 registro con `origen=REINTENTO` y el motor pedido.
3. Fallo del motor: When el motor lanza una excepcion (red, cuota, `InvalidExtractionException`), Then 1 registro `resultado=FALLO` con `tipoFallo` (`RESPUESTA_INVALIDA` para `InvalidExtractionException`, `MOTOR_NO_DISPONIBLE` para `MotorNoDisponibleException`, `OTRO` resto) y `claseError` = `simpleName` de la clase (nunca el `message`); la excepcion original se relanza intacta.
4. Motor no disponible (Local forzado sin modelo, o reintento a motor no disponible): se registra como FALLO `MOTOR_NO_DISPONIBLE` con `motor` = el de la excepcion y duracion ~0 (el motor no se invoca; documenta la intencion del usuario).
5. Cancelacion: When la corrutina se cancela durante la extraccion (Cancelar, back, `flatMapLatest`), Then se registra `resultado=CANCELADA` con la duracion hasta la cancelacion (escritura en `NonCancellable`) y la `CancellationException` se relanza SIEMPRE.
6. Texto blank: los casos de uso cortocircuitan sin invocar al motor -> NO se registra nada (no hubo extraccion ni cuota).
7. Fallo del registro: si escribir el log falla (excepcion no de cancelacion), se ignora: nunca altera ni oculta el resultado/error de la extraccion.
8. Retencion: tras cada insercion se conservan como maximo las 500 filas mas recientes (las mas antiguas se borran en la misma transaccion).
9. Consulta debug: En build debuggable, Ajustes muestra la seccion "Diagnostico de extracciones" con resumen por motor (n, exitos, duracion media), las ultimas 20 entradas, "Copiar CSV" y "Borrar registro". En build no debuggable la seccion no se compone.
10. Migracion: Given una BD v1 con tickets, When la app actualiza a v2, Then los tickets/lineas se conservan y existe la tabla vacia del log (sin perdida de datos; sin `fallbackToDestructiveMigration`).

## Repository research (inspeccionado)

- `AGENTS.md`, `docs/technical-discovery.md` (Observability: registro por extraccion con motor, duracion, longitud del texto crudo y resultado; "log o tabla de debug"), `feature_list.json` (entrada log-extracciones).
- `domain/expenses/.../data/TextEngine.kt` (`val motor`, `suspend extract(textoCrudo)`), `ExtractorPorMotor.kt` (`disponibilidad`, `extract(motor, texto)`), `EngineRouter.kt` (implementa ambas; sin fallback; Local forzado no disponible lanza `MotorNoDisponibleException` sin invocar motor).
- `usecases/ExtraerTicket.kt` (texto blank -> TicketExtraido vacio sin motor; comentario "Punto de extension futuro de log-extracciones"), `ExtraerTicketConMotor.kt` (idem).
- `feature/escanear/.../EscanearViewModel.kt`: `Continuar` -> `extraerTicket(texto)`; `ReintentarCon` -> `extraerTicketConMotor(texto, motor)`; ambas dentro de `flatMapLatest` (cancelacion estructurada; CancellationException relanzada).
- `framework/ai/.../di/di.kt`: `provideTextEngine(router): TextEngine` y `provideExtractorPorMotor(router): ExtractorPorMotor` son la misma instancia `EngineRouter` (punto unico de cableado). `GeminiCloudEngine` usa `@Named("nube")`, local `@Named("local")`.
- `framework/database`: `TallyDatabase` version **1**, `exportSchema=true`, entities `TicketEntity`,`LineaEntity`; `schemas/.../1.json` existe; `DatabaseModule` (`di/di.kt`) construye `"tally.db"` sin migraciones; `TicketDao` con Flow y `@Transaction`; `build.gradle.kts` con `ksp room.schemaLocation`. No hay `room-testing` en `gradle/libs.versions.toml` (room = 2.7.2) ni migraciones previas.
- `feature/ajustes`: `AjustesViewModel` (estado por `combine`), `AjustesScreen`/`AjustesContent` (Scaffold + Column con secciones Motor y Modelo local). Sin BuildConfig en ningun modulo; precedente de deteccion debug en runtime: `RevisionFotoContent.kt` usa `ApplicationInfo.FLAG_DEBUGGABLE`.
- `domain/expenses/build.gradle.kts`: Kotlin JVM, `api(coroutines.core)`, `javax.inject`. Dobles compartidos en `:test:unit`.
- No inspeccionado: `:test:unit` por dentro (el implementer comprueba que depende de `:domain:expenses`), `docs/architecture.md`, tests androidTest de `:app` (`ProcesadoFlujoTest`, `RevisionDatosFlujoTest`) mas alla de su existencia.

## Decisiones (con default) — la primera es la destacada

**D1 (ABIERTA, DESTACADA) Almacenamiento: migracion de Room.**
Default: tabla `extracciones_log` en la MISMA `TallyDatabase` (Room fuente de verdad, AGENTS.md), con **migracion explicita 1 -> 2**:
- `TallyDatabase.version = 2`, nueva entidad en `entities`, `abstract fun extraccionLogDao()`.
- `Migration(1, 2)` con `CREATE TABLE extracciones_log (...)` + indice por `marca_tiempo`, registrada con `.addMigrations(MIGRATION_1_2)` en `DatabaseModule`. Prohibido `fallbackToDestructiveMigration` (destruiria los tickets del usuario/emulador).
- Esquema exportado: nuevo `schemas/com.robertomr.tally.framework.database.TallyDatabase/2.json` (commit con el codigo); `1.json` no se toca.
- Test de migracion instrumentado (`MigrationTestHelper`): crear v1, insertar ticket+lineas, migrar a v2, validar esquema y que los datos siguen. Requiere (a) entrada `room-testing` en `gradle/libs.versions.toml` con `version.ref = "room"`, (b) `androidTestImplementation(libs.room.testing)` y `sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")` en `framework/database/build.gradle.kts`. Es ampliacion de catalogo/build de modulo existente, NO modulo nuevo; confirmar con el usuario.
- Riesgo: es la primera migracion del proyecto; un error borra o bloquea datos de emulador/dispositivo. Mitigacion: test de migracion + E2E con `adb install -r` sobre un build previo con datos (nunca `pm clear`, nunca `connected*`).
- Alternativa sin migracion (si el usuario prefiere cero riesgo para los tickets): segunda BD Room `tally_log.db` con su propio `@Database`, sin tocar `tally.db`. Se descarta por defecto (dos BD, mas DI) pero es valida.
- Alternativa descartada: fichero/logcat (no consultable por UI, rompe "Room unica fuente de verdad").

**D2 Donde se registra.** Default: decorador `EngineConRegistro` en `domain/expenses/data/` (Kotlin puro) que implementa `TextEngine` y `ExtractorPorMotor`, envuelve al router y registra en ambas rutas en un solo sitio (sin tocar casos de uso ni VM, sin duplicar). Constructor: `(delegado: TextEngine, porMotor: ExtractorPorMotor, registro: RegistroExtracciones, reloj: Reloj)`; en produccion ambos delegados son el mismo `EngineRouter`. `val motor` y `disponibilidad` delegan sin registrar. Cableado: `framework/ai/.../di/di.kt` -> `provideTextEngine` y `provideExtractorPorMotor` devuelven el mismo singleton `EngineConRegistro` (cuentan como Singleton para no duplicar). Alternativa descartada: envolver los casos de uso (dos sitios, y el blank-text pasaria por ahi).
Origen: `extract(texto)` = `ROUTER`; `extract(motor, texto)` = `REINTENTO`. Motor registrado en ruta router = `delegado.motor` leido antes de llamar (mismo que el badge); si se lanza `MotorNoDisponibleException`, el de la excepcion.

**D3 Privacidad.** Solo metadatos: `marcaTiempo`, `motor`, `origen`, `resultado`, `tipoFallo?`, `claseError?` (simpleName), `duracionMs`, `longitudTexto`. Nunca texto, mensajes de excepcion (pueden incluir fragmentos de respuesta), datos del ticket ni token. Retencion: 500 filas (constante en el DataSource, `@Transaction` insertar + recortar) + "Borrar registro" en debug. Se registra en todos los builds (son metadatos); solo se consulta en debug.

**D4 Consulta debug.** Default: seccion en `:feature:ajustes` visible solo si `applicationInfo.flags and FLAG_DEBUGGABLE != 0` (mismo patron que `RevisionFotoContent`; sin BuildConfig ni modulo nuevo). Copiar CSV via `LocalClipboardManager`; el CSV se genera en dominio (funcion pura testeable). Descartada: pantalla oculta (ruta extra en `:app`).

**D5 Reloj.** Interfaz `Reloj` en `domain/expenses/data/` con `monotonicNanos(): Long` (duracion) y `epochMillis(): Long` (marca). Impl `RelojSistema` (`System.nanoTime`/`currentTimeMillis`) internal en `:framework:database` (`source/`), provista desde su `di.kt`. Los tests usan `FakeReloj` con avance manual.

**D6 Cancelacion/errores.** `catch (e: CancellationException)` -> registrar `CANCELADA` en `withContext(NonCancellable)` y `throw e`. `catch (e: Exception)` -> registrar `FALLO` y relanzar. Escritura del registro envuelta en try/catch que traga solo excepciones no-cancelacion (escenario 7).

## Technical approach / expected file changes

Dominio (`domain/expenses/src/main/java/com/robertomr/tally/domain/expenses/`):
- `entities/RegistroExtraccion.kt` (data class + enums `OrigenExtraccion{ROUTER,REINTENTO}`, `ResultadoExtraccion{EXITO,FALLO,CANCELADA}`, `TipoFallo{RESPUESTA_INVALIDA,MOTOR_NO_DISPONIBLE,OTRO}`), con `id: Long = 0`.
- `data/RegistroExtracciones.kt` (interfaz DataSource: `suspend registrar(r)`, `observarRecientes(limite): Flow<List<...>>`, `suspend borrarTodo()`), `data/Reloj.kt`, `data/EngineConRegistro.kt`.
- `usecases/`: `ObservarRegistroExtracciones` (recientes + resumen por motor), `BorrarRegistroExtracciones`, `ExportarRegistroExtracciones` (CSV: `id,instante_iso,motor,origen,resultado,tipo_fallo,clase_error,duracion_ms,longitud_texto`; escapado CSV).
- Actualizar comentario de `usecases/ExtraerTicket.kt`.

Framework `:framework:database` (todo `internal`, un solo `di.kt`):
- `entity/ExtraccionLogEntity.kt` (`@Entity(tableName="extracciones_log")`, enums como `String`), `dao/ExtraccionLogDao.kt` (insert, `@Query` Flow ordenado por `marca_tiempo DESC LIMIT`, `DELETE` de excedentes, borrar todo; `@Transaction` insertar+recortar), `source/RegistroExtraccionesLocalDataSource.kt`, `source/RelojSistema.kt`, `Migrations.kt` (`MIGRATION_1_2`), `TallyDatabase.kt` (v2), `di/di.kt` (provee dao, `RegistroExtracciones`, `Reloj`, `addMigrations`), `schemas/.../2.json` generado, `build.gradle.kts` (room-testing + assets de schemas), `gradle/libs.versions.toml` (`room-testing`).

Framework `:framework:ai`: `di/di.kt` -> wrap con `EngineConRegistro` (depende de `RegistroExtracciones` y `Reloj` de dominio, ya hay `implementation(project(":domain:expenses"))`).

Feature `:feature:ajustes`: `AjustesViewModel` (+ casos de uso nuevos, estado `registro` en `AjustesUiState` por `combine`), `AjustesScreen.kt` (seccion debug con testTags `diagnostico_extracciones`, `diagnostico_copiar_csv`, `diagnostico_borrar`), `AjustesState` si hace falta para el snackbar "CSV copiado". Sin cambios en `:app` ni en `feature:escanear`.

Dobles en `:test:unit`: `FakeRegistroExtracciones`, `FakeReloj` (si ya existe un fake de `TextEngine`/`ExtractorPorMotor`, reutilizarlo; si no, crearlo aqui).

## Visual design impact

Minima y solo debug: tarjeta con el estilo de las secciones existentes de Ajustes (Material 3), texto monoespaciado compacto para las filas. Sin artefacto de diseno nuevo. Debe caber a 360 dp. No hay Stitch asociado.

## Durable documentation impact

- `AGENTS.md`: not needed (reglas intactas; se cumplen).
- `ARCHITECTURE.md` / `CONSTRAINTS.md` raiz: not needed (no existen).
- `docs/architecture.md`: update — decorador `EngineConRegistro` y tabla `extracciones_log`.
- `docs/technical-discovery.md` (Observability): update — decision de tabla Room + metadatos, retencion 500, consulta solo debug.
- `docs/domain-model.md`: not needed (log tecnico, no entidad de negocio).
- `docs/risks-and-open-questions.md`: update minimo — primera migracion de Room y regla de privacidad del log.
- `PROGRESS.md` y `feature_list.json`: los actualiza el orquestador/implementer con evidencia (el planner no puede).

## Implementation plan / tasks (orden)

1. Dominio: entidades, `Reloj`, `RegistroExtracciones`, `EngineConRegistro` + tests (T-unit abajo). Verde antes de seguir.
2. Casos de uso observar/borrar/exportar CSV + tests.
3. Room: entidad, DAO, `TallyDatabase` v2, `MIGRATION_1_2`, exportar `2.json` (build), DataSource, `RelojSistema`, `di.kt`.
4. Catalogo `room-testing` + build.gradle.kts + test de migracion y test DAO (retencion 500, orden, Room in-memory) en androidTest.
5. Cableado en `:framework:ai` `di.kt`.
6. Ajustes: VM + UI debug + tests de VM.
7. Verificacion base y E2E; capturas en `docs/evidence/log-extracciones/`.

## Verification plan

- Unit `EngineConRegistroTest` (dominio, con fakes y `FakeReloj`): exito ROUTER; exito REINTENTO; fallo generico; `InvalidExtractionException`; `MotorNoDisponibleException`; duracion exacta con reloj fake; cancelacion (registra CANCELADA y relanza `CancellationException`, usar `runTest` + `cancel()`); registro que falla no altera el resultado; el `message` de la excepcion no aparece en ningun campo; el texto no se almacena (solo longitud).
- Unit CSV (escapado, cabecera, orden) y resumen por motor; `AjustesViewModelTest` ampliado.
- Instrumentado `:framework:database`: `MigrationTest` 1->2 (datos conservados, esquema = 2.json), `ExtraccionLogDaoTest` (retencion 500, orden descendente, borrar todo).
- Base: `gradlew.bat assembleDebug testDebugUnitTest --console=plain` (o `bash init.sh`).
- Instrumentado en `emulator-5556` con `adb -s emulator-5556 shell am instrument -w -e class <clase> <paquete.test>/androidx.test.runner.AndroidJUnitRunner` (nunca `connected*`, nunca `pm clear`; actualizar app con `install -r`). Ampliar el flujo E2E existente de `:app` (`ProcesadoFlujoTest`, que ya ejerce la extraccion con dobles) para: hacer 2-3 extracciones (una por router, un reintento con otro motor, una cancelada), abrir Ajustes y comprobar filas con motor/origen/resultado. Si el E2E usa motores falsos, el decorador debe seguir en el grafo (no sustituirlo en `@TestInstallIn`; sustituir solo los motores).
- E2E de actualizacion: instalar el APK anterior (HEAD previo), crear un ticket, `install -r` del nuevo, comprobar que el ticket sigue.

## Evidence to capture

Salida de unit tests; salida de `am instrument` (migracion, DAO, E2E); `2.json` en el commit; captura de la seccion de Ajustes (360 dp) y CSV copiado pegado en un fichero de ejemplo con metadatos solamente en `docs/evidence/log-extracciones/README.md`; comprobacion `grep` de que el CSV no contiene texto del ticket.

## Validator checklist

- [ ] Un solo punto de registro (`EngineConRegistro`), ambas rutas cubiertas, sin cambios en `EscanearViewModel`/casos de uso.
- [ ] Dominio sin imports de Android; implementaciones y modulos Hilt `internal`; un `di.kt` por modulo; sin modulos Gradle nuevos; versiones solo en `libs.versions.toml`.
- [ ] Version 2, `Migration(1,2)` presente, `2.json` exportado, sin `fallbackToDestructiveMigration`, test de migracion verde.
- [ ] Sin texto crudo/datos de ticket/mensaje de excepcion/token en entidad, CSV ni logs.
- [ ] `CancellationException` siempre relanzada; CANCELADA registrada con `NonCancellable`.
- [ ] Duracion medida con reloj inyectable (tests deterministas).
- [ ] Seccion de diagnostico solo en build debuggable; retencion 500 comprobada.
- [ ] Verificacion base y E2E en `emulator-5556` ejecutados con evidencia; datos del emulador conservados.

## Unknowns

- Confirmar D1 (misma BD con migracion vs segunda BD) y la adicion de `room-testing` al catalogo.
- Que `:test:unit` ya expone fakes de `TextEngine`/`ExtractorPorMotor` (no inspeccionado).
- Nombre del paquete de test para `am instrument` (comprobar con `adb shell pm list instrumentation`).
