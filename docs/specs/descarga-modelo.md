# Feature Implementation Spec: Descarga del modelo local con progreso y estado

## Source Feature

- `id`: descarga-modelo (`area`: ia)
- `depends_on`: [spike-gemma-mediapipe] (accepted 2026-10-13) -> satisfecha
- `status`: not_started (a fecha de planificacion, 2026-10-18)
- `verification` (fuente):
  1. Descarga completa probada en la Tab S9+ con reanudacion basica.
  2. Con red movil, aviso y sin descarga (politica WiFi).
  3. Estado del modelo visible en Ajustes.
- `notes` (fuente): el archivo NO va en el APK; fuente Hugging Face; la carga del
  modelo nunca en hilo principal (la carga es de `extraccion-local`, no de aqui).

## Goal

El usuario descarga desde la app el modelo local validado en el spike
(`gemma-3n-E2B-it-int4.litertlm`, 3.655.827.456 bytes = 3,4 GB, NO "2-3 GB":
corrige la estimacion de la doc duradera) solo por red no medida (WiFi), con
progreso, aviso de espacio, reanudacion tras corte y estado visible
(No descargado / Descargando / En pausa / Descargado / Error) en una pantalla
**Ajustes** minima. Deja el fichero en una ruta estable que `extraccion-local` y
`router-automatico` consumiran via una interfaz de dominio.

## Non-Goals

- Cargar/ejecutar el modelo, `GemmaLocalEngine`, router, forzar motor
  (`extraccion-local`, `router-automatico`, `ajustes-motor`). Esta feature solo
  expone `estado` y `rutaModelo`.
- Selector de modelo, varios modelos, E4B, actualizaciones/versionado del modelo.
- Importar el modelo desde un fichero (SAF) o descarga por red movil con
  "permitir de todos modos".
- Hash criptografico fijado en codigo (ver R-DM3). Sin Retrofit/OkHttp/REST:
  solo `HttpURLConnection` de la plataforma. Sin backend propio.
- Seccion "Forzar motor" de Ajustes (es `ajustes-motor`; aqui solo la seccion
  "Modelo local"). Sin ADRs nuevos ni cambios en `init.sh`/`build-logic/`.

## Job Story

When quiero usar el motor Local (privacidad/offline) y aun no tengo el modelo,
I want descargarlo una vez con WiFi, ver cuanto falta y que sobreviva a un corte,
so I can extraer tickets sin red y sin que el ticket salga del dispositivo.

## Acceptance Scenarios

### Scenario 1: descarga completa con progreso (verificacion 1)
- Given WiFi, espacio suficiente y token de HF valido, modelo ausente
- When pulso "Descargar modelo" en Ajustes
- Then el estado pasa a Descargando con barra y "X,X / 3,4 GB" que crece, y al
  terminar a Descargado con el tamano; el fichero final existe en
  `getExternalFilesDir(null)/models/gemma-3n-E2B-it-int4.litertlm` y no queda `.part`.

### Scenario 2: reanudacion basica
- Given una descarga en curso o cortada (WiFi off, proceso matado)
- When vuelve la red no medida / se reabre la app
- Then continua desde el tamano del `.part` con cabecera `Range: bytes=N-`
  (sin reiniciar), y termina en Descargado. Si el servidor responde 200 a un
  Range, se descarta el `.part` y se reinicia limpio.

### Scenario 3: politica WiFi (verificacion 2)
- Given red movil (o WiFi medida) y modelo ausente
- When pulso "Descargar modelo"
- Then aparece el aviso "Solo se descarga con WiFi", NO se encola trabajo ni se
  abre conexion y el estado sigue No descargado. Si se pierde la WiFi a mitad,
  pasa a En pausa ("Esperando WiFi") sin consumir datos moviles.

### Scenario 4: aviso de espacio
- Given espacio libre < (tamano restante + margen 500 MB)
- When pulso "Descargar modelo"
- Then aviso "Necesitas X GB libres (tienes Y)" y no se descarga.

### Scenario 5: estado en Ajustes y errores (verificacion 3)
- Given cualquier estado
- Then Ajustes (alcanzable desde Resumen) muestra el estado de forma honesta;
  401/403 -> Error "Acepta la licencia en Hugging Face y revisa tu token";
  tamano final distinto del anunciado -> Error de integridad con `.part`
  descartado; "Cancelar" borra el `.part` y vuelve a No descargado;
  "Eliminar modelo" (con confirmacion) borra el fichero.

### Scenario 6: reglas de arquitectura
- `domain` sin imports de Android; implementaciones `internal` en
  `framework/ai`; sin claves en APK/BuildConfig (el token lo introduce el usuario
  y vive en almacenamiento privado no respaldado); sin Retrofit/OkHttp.

## Repository Research

Inspeccionado: `AGENTS.md`, `PROGRESS.md` (ses. 005-007 spike, 028-029),
`feature_list.json`, `CONTEXT.md` (Ajustes = forzar motor + estado del modelo),
`docs/technical-discovery.md`, `docs/risks-and-open-questions.md` (R1, punto 2),
`docs/adr/0001`, `docs/specs/spike-gemma-mediapipe.md`, `docs/specs/edicion-lineas.md`
(formato), `gradle/libs.versions.toml`, `framework/ai/{build.gradle.kts,
AndroidManifest.xml,di/di.kt,spike/GemmaMediaPipeSpike.kt}`,
`framework/database/build.gradle.kts`, `feature/{gastos,escanear}/build.gradle.kts`,
`app/.../navigation/TallyApp.kt`, `app/src/main/AndroidManifest.xml`,
glob de `domain/expenses`, `app/src/androidTest`, mockups Stitch (grep).
NO inspeccionado: `TallyRoute.kt`, `ResumenScreen.kt`, `:test:unit` por dentro,
`TextEngineEntryPoint.kt` (el implementer los lee).

Hallazgos:
- El modelo es **gated** en HF (spike: 401): hace falta aceptar licencia y token.
- No existe pantalla Ajustes, ni modulo, ni ruta, ni mockup Stitch de Ajustes
  (solo el icono `account_circle` en los mockups 02/03). `ResumenScreen` no tiene
  top bar. No hay `AcScaffold`/`stateAsResultIn` en el repo (los VM de
  `:feature:escanear` usan su propio patron).
- `framework/ai` solo tiene `AiModule` (OcrEngine, TextEngine nube) en `di/di.kt`
  y un manifest vacio; el spike uso `.../files/models` del test APK.
- `:app` no declara `allowBackup`: el valor por defecto es true (afecta al token).
- No hay dependencia de WorkManager; E2E de `:app` usan la app real sin
  `HiltAndroidRule` (patron R-NUBE6/R-PROC3).

## Technical Approach

**Ejecucion**: `androidx.work` (WorkManager, `CoroutineWorker`, trabajo unico
`modelo-descarga`, constraint `NetworkType.UNMETERED`, backoff exponencial,
`setForeground` con notificacion de tipo `dataSync`). Justificacion: 3,4 GB tardan
~6 min a ~10 MB/s; hay que sobrevivir a background, corte de red y muerte de
proceso con ~0 codigo propio. No usar `DownloadManager`: reenvia `Authorization`
al CDN tras la redireccion de HF y falla (ver R-DM2).

**Cliente HTTP**: `ModelDownloadEngine` (`internal`, JVM puro salvo `java.net`)
sobre `HttpURLConnection`, solo HTTPS en produccion:
1. `followRedirects=false`, hasta 5 saltos manuales; `Authorization: Bearer` solo
   si el host es el de HF original (se retira en saltos a otros hosts).
2. Si existe `<final>.part` con N bytes -> `Range: bytes=N-`; 206 -> append;
   200 -> truncar y reiniciar; 416 con N == total -> completar; 416 otro -> reiniciar.
3. Total = `Content-Range` (o `Content-Length` en 200). Escribe en bloques de
   256 KB comprobando `ensureActive()`; emite progreso como maximo cada ~500 ms.
4. Al terminar, `length == total` o error de integridad (borra `.part`); rename
   atomico `.part` -> final. 401/403 -> fallo `Autorizacion`; otros 4xx -> `Red`;
   `IOException` -> el worker reintenta (maximo 5 intentos, luego fallo `Red`).

**Dominio** (`domain/expenses`, Kotlin puro):
- `entities/EstadoModelo`: `NoDescargado`, `Descargando(bytes,total)`,
  `EnPausa(bytes,total,motivo)`, `Descargado(bytes)`, `Fallo(motivo,bytes)`;
  `MotivoPausa {EsperandoWifi}`, `MotivoFallo {Autorizacion, Integridad, Red, SinEspacio}`.
- `entities/ResultadoDescarga`: `Iniciada`, `RequiereWifi`,
  `EspacioInsuficiente(necesarios,libres)`.
- `data/ModelDownloader` (interfaz): `estado: Flow<EstadoModelo>`, `iniciar()`,
  `cancelar()`, `eliminar()`, `rutaModelo(): String?` (null si no esta Descargado).
- `data/DownloadConditions`: `esRedSinCoste(): Boolean`, `espacioLibreBytes(): Long`,
  `bytesYaDescargados(): Long`.
- `data/ModelAccessTokenStore`: `hayToken: Flow<Boolean>`, `guardar(token)`, `borrar()`.
- `usecases/`: `ObservarEstadoModelo`, `DescargarModelo` (comprueba WiFi, luego
  espacio = `tamanoEsperado - bytesYaDescargados + 500 MB`, luego `iniciar()`),
  `CancelarDescargaModelo`, `EliminarModelo`, `ConfigurarTokenModelo`. El tamano
  esperado `3_655_827_456` vive como constante de dominio.

**Framework** (`framework/ai/modelo/`, todo `internal`): `ModelDownloadEngine`,
`ModelDownloadWorker` (construye el motor desde `applicationContext`; sin
`hilt-work`), `WorkManagerModelDownloader` (mapea `WorkInfo` + fichero ->
`EstadoModelo`), `AndroidDownloadConditions` (`NetworkCapabilities`
`NET_CAPABILITY_NOT_METERED` + `StatFs`), `FileModelAccessTokenStore` (fichero en
`noBackupFilesDir`, nunca en logs), `ModelPaths`. Bindings en el `di/di.kt`
existente (un `di.kt` por modulo). La URL
`https://huggingface.co/google/gemma-3n-E2B-it-litert-lm/resolve/main/gemma-3n-E2B-it-int4.litertlm`
es constante publica de `framework/ai` (no es secreto).

**Presentacion** (`:feature:ajustes`, ver D1): `AjustesScreen/AjustesViewModel/
AjustesState`; el VM combina `ObservarEstadoModelo`, `hayToken` y un
`MutableStateFlow` de avisos (WiFi/espacio) en un `StateFlow<AjustesUiState>`
(sin `load()` imperativo). Seccion "Modelo local": estado, barra
(`LinearProgressIndicator` con tokens Forest Precision), texto de bytes (cifras
`tnum`), botones Descargar/Cancelar/Eliminar, campo de token enmascarado.
`:app`: `TallyRoute.Ajustes`, `composable` que cablea `onBack`, y
`ResumenScreen(onAbrirAjustes)` con un `IconButton` arriba a la derecha donde el
mockup 02 pone `account_circle` (D2). La pantalla no recibe `NavController`.

## Expected File Changes

- `gradle/libs.versions.toml`: `workManager` (ultima estable compatible con
  compileSdk 36; verificarla en Google Maven) + `androidx-work-runtime-ktx`.
- `domain/expenses/.../{entities,data,usecases}/` ficheros listados arriba.
- `framework/ai/build.gradle.kts` (+work), `framework/ai/src/main/AndroidManifest.xml`
  (INTERNET, ACCESS_NETWORK_STATE, FOREGROUND_SERVICE, FOREGROUND_SERVICE_DATA_SYNC,
  POST_NOTIFICATIONS; override `SystemForegroundService` con
  `foregroundServiceType="dataSync"` y `tools:node="merge"`),
  `framework/ai/.../modelo/*.kt`, `di/di.kt`.
- `test/unit/src/main/kotlin/`: `FakeModelDownloader`, `FakeDownloadConditions`,
  `FakeModelAccessTokenStore`.
- Nuevo `feature/ajustes/` (+ `settings.gradle.kts`, `app/build.gradle.kts`,
  `docs/architecture.md`) si D1 se confirma; `app/.../TallyRoute.kt`, `TallyApp.kt`,
  `feature/resumen/.../ResumenScreen.kt`.
- Tests: ver Verification Plan. `app/src/main/AndroidManifest.xml`: sin cambios
  (los permisos entran por merge); `allowBackup` se deja (el token va a `noBackupFilesDir`).

## Visual Design Impact

UI con Material 3 y tokens de `DESIGN.md`/Forest Precision; sin mockup propio de
Ajustes (D3): reutilizar tarjeta `surface-container`, tipografia Inter, cifras
`tnum` y la convencion de chip de estado del procesado. No inventar decoracion
(DESIGN prohibe contenido ficticio). Revisar en 411dp y en Tab S9+ (ancho maximo
centrado). Mostrar tambien el motor/estado solo como texto: el badge de motor es de
`router-automatico`.

## Durable Documentation Impact

- `docs/technical-discovery.md`: update (3,4 GB, WorkManager, HttpURLConnection,
  token del usuario). `docs/risks-and-open-questions.md`: update (punto 2 resuelto;
  "~2-3 GB" -> 3,4 GB, finding Low de la sesion 007). `docs/architecture.md`:
  update (modulo ajustes, descarga). `AGENTS.md`/ADRs/`CONSTRAINTS.md`: not
  needed (sin cambios de regla; ADR 0001 ya preve la descarga). `CONTEXT.md`: not needed.

## Implementation Plan / Tasks

- [x] T1 Confirmar D1 con el orquestador/usuario antes de crear el modulo. Catalogo: WorkManager.
- [x] T2 Dominio: entidades, interfaces, casos de uso + fakes en `:test:unit`.
- [x] T3 `DescargarModeloTest` y resto de tests de casos de uso (rojo -> verde).
- [x] T4 `ModelDownloadEngine` + `ModelDownloadEngineTest` (servidor local JDK).
- [x] T5 Worker, `WorkManagerModelDownloader`, condiciones, token store, `ModelPaths`,
  manifest y DI.
- [x] T6 `:feature:ajustes` (VM + State + Screen) + `AjustesViewModelTest`.
- [x] T7 `:app`: ruta, entrada desde Resumen, `docs/architecture.md`.
- [x] T8 E2E `AjustesModeloFlujoTest` en `emulator-5554`.
- [ ] T9 Verificacion fisica en la Tab S9+ (descarga real, corte, red movil).
- [x] T10 Docs duraderas, `PROGRESS.md`, `feature_list.json` con evidencia.

## Verification Plan

- Base: `gradlew.bat assembleDebug testDebugUnitTest :test:unit:test :domain:expenses:test`.
- JVM: `DescargarModeloTest` (WiFi no -> `RequiereWifi` sin `iniciar`; espacio
  insuficiente; el espacio descuenta `.part`; ok -> `Iniciada`), `AjustesViewModelTest`
  (Turbine: estados, avisos), `ModelDownloadEngineTest` con `HttpServer` JDK:
  200 completo, reanudacion 206, servidor que ignora Range, 416, 401/403,
  redireccion cross-host SIN `Authorization`, longitud truncada -> integridad,
  cancelacion deja `.part`.
- E2E (`ANDROID_SERIAL=emulator-5554`): abrir Ajustes desde Resumen, "Modelo
  local" visible con estado No descargado y boton; con red movil simulada
  (`adb shell svc wifi disable; svc data enable`) el aviso aparece y no hay
  `WorkInfo` encolado. Suite `:app` completa sin regresiones.
- Fisico, Tab S9+ (`ANDROID_SERIAL`): descarga real con token HF, cortar WiFi a
  mitad (estado En pausa, bytes conservados), restaurar y comprobar reanudacion
  (log del `Range` y bytes iniciales > 0), `adb shell ls -l` del tamano final
  3.655.827.456, y que `GemmaMediaPipeSpike` pueda cargar esa ruta si se quiere
  (opcional). Medir tiempo total.

## Evidence To Capture

JUnit XML (JVM e instrumentados), capturas de Ajustes en cada estado en
`docs/evidence/descarga-modelo/`, `adb shell ls -l` del fichero, log de
reanudacion, tiempo de descarga, busquedas de 0 versiones literales fuera del
catalogo, 0 Retrofit/OkHttp, 0 secretos y diffs vacios en `init.sh`/`AGENTS.md`/
`build-logic/`.

## Risks

- R-DM1 Token HF: sin token valido no hay descarga real. Prerrequisito humano
  (como U1 de `extraccion-nube`): aceptar licencia Gemma en HF y crear un token de
  lectura. Sin el, solo se verifica el protocolo con el servidor local.
- R-DM2 Redireccion a CDN: la `Authorization` hacia el CDN rompe la descarga;
  mitigado con redireccion manual (probado en JVM). El host del CDN puede cambiar.
- R-DM3 Integridad solo por tamano; la URL `main` puede cambiar de version. No se
  fija un hash; un modelo corrupto fallaria al cargarse (`extraccion-local`).
- R-DM4 Un fichero preexistente (p. ej. `adb push` de desarrollo) se acepta como
  Descargado si existe y no es vacio; la responsabilidad es de quien lo copia.
- R-DM5 Android 14+: tipo `dataSync` limitado en tiempo y POST_NOTIFICATIONS puede
  estar denegado (la descarga sigue, sin notificacion visible).
- R-DM6 Metered: un WiFi marcado como medido o un hotspot se trata como "no WiFi".

## Open Questions (con DEFAULT)

- **D1 (requiere confirmacion; AGENTS: no crear modulos sin preguntar)**: crear
  `:feature:ajustes` (patron `:feature:gastos`+`tally.hilt`). Es la unica forma de
  respetar "pantallas en `feature/<pantalla>/`"; `ajustes-motor` lo reutilizara.
  DEFAULT: crearlo. Solo bloquea T6-T8; T2-T5 pueden avanzar.
- D2 Entrada a Ajustes: icono en `ResumenScreen` (posicion del avatar del mockup 02),
  sin cuarto destino de bottom bar. DEFAULT: asi.
- D3 Sin mockup de Ajustes: layout minimo de tarjeta. DEFAULT: asi; revision visual humana.
- D4 Origen del token: el usuario lo pega en Ajustes (almacenado en
  `noBackupFilesDir`). Alternativas (alojar el modelo propio, token en el APK)
  contradicen reglas de AGENTS o ADR. DEFAULT: token del usuario.
- D5 Cancelar descarta el `.part`; la pausa solo es automatica. DEFAULT: asi.
- D6 Ruta final: `getExternalFilesDir(null)/models/` (permite `adb push` de dev);
  si es null, `filesDir/models`. DEFAULT: asi.

## Validator Checklist

- [x] Escenarios 1-6 cumplidos con evidencia real (incluida la Tab S9+, no solo JVM).
- [x] Con red movil no se abre ninguna conexion (aviso + sin trabajo encolado).
- [x] Reanudacion demostrada con `Range` y bytes > 0 al reanudar.
- [x] 0 Retrofit/OkHttp, 0 secretos, token fuera de logs y de backup, modelo fuera del APK/repo.
- [x] `domain` puro; clases de `framework` `internal`; un `di.kt` por modulo; versiones en el catalogo.
- [x] Solo la seccion "Modelo local" (sin forzar motor); sin cargar el modelo.
- [x] Docs duraderas actualizadas (3,4 GB) y modulo nuevo solo si D1 fue confirmado.
