# Feature Implementation Spec: Elegir una foto de la galería para escanear

## Source Feature

- `id`: galeria-escaneo
- `area`: captura
- `depends_on`: detalle-ticket (accepted); `forma-pago` accepted y commiteada (50c521a)
- `status`: not_started
- `source`: `feature_list.json` + petición del usuario (2026-10-20): poder elegir una foto de la galería para no estar obligado a hacerla con la cámara en ese momento.

## Goal

En Escanear, el usuario puede elegir una imagen de su galería con el selector del sistema (Photo Picker, `ActivityResultContracts.PickVisualMedia`, sin permisos de almacenamiento ni cambios de manifest), también con el permiso de cámara denegado. La imagen elegida sigue el mismo camino que una captura: Procesando → guardar foto (EXIF + escala + JPEG 85) → OCR → Revisión de foto → procesado → revisión de datos → guardado.

## Non-Goals

- Selección múltiple, selector propio dentro de la app, miniaturas, Coil u otra dependencia de imágenes.
- Pantalla de recorte/encuadre sobre la foto elegida (se usa la imagen completa; ver Enfoque 3). Sin deskew ni ajuste de esquinas.
- Importar PDF, capturas con varios tickets, o leer del portapapeles/compartir-a-Tally (intent filter) : fuera de alcance.
- Corregir orientaciones EXIF espejo (2/4/5/7): sigue sin corregirse (límite ya existente del pipeline).
- Permisos nuevos (`READ_MEDIA_IMAGES`, etc.), módulos Gradle nuevos, cambios de Room/dominio de tickets, red nueva.
- Borrar o tocar la imagen original de la galería (se lee una vez y se copia procesada al almacenamiento privado).

## Job Story

When tengo ya una foto del ticket en el móvil (o la cámara no me sirve ahora),
I want to elegirla de la galería dentro de Escanear,
so I can registrar el gasto sin tener el ticket delante ni conceder la cámara.

## Acceptance Scenarios

### Scenario 1: Elegir de la galería con permiso de cámara concedido
Given Escanear en Encuadre
When pulso "Elegir de la galería" (`captura_galeria`) y el selector devuelve una imagen
Then se muestra `captura_procesando`, se guarda la foto procesada, se corre el OCR y se abre `captura_revision` con la foto; Continuar sigue el flujo normal hasta guardar el ticket.

### Scenario 2: Sin permiso de cámara
Given permiso de cámara denegado (estado PermisoPendiente, `captura_permiso`)
When pulso "Elegir de la galería" (`permiso_galeria`) y elijo una imagen
Then se recorre el mismo flujo hasta `captura_revision`; no se pide ningún permiso. Al "Repetir foto" o volver desde Error, se regresa a `captura_permiso` (no a un encuadre sin cámara).

### Scenario 3: Cancelar el selector
Given el selector abierto
When lo cierro sin elegir (resultado `null`)
Then el estado, la pantalla y el borrador no cambian; no hay error ni snackbar.

### Scenario 4: Imagen grande o con rotación EXIF
Given una imagen de 12 MP o más, o un JPEG con EXIF orientación 6 u 8
When se elige
Then la foto guardada tiene el lado largo <= 1280 px, la orientación horneada (vertical se ve vertical), sin tag EXIF, sin OOM, y la imagen completa (sin recorte).

### Scenario 5: Imagen ilegible, demasiado grande o formato no decodificable
Given un fichero que no se puede abrir/decodificar, o de más de 32 MiB
When se elige
Then se muestra el estado Error existente (`captura_error`) con mensaje claro en español ("No se pudo leer la imagen..." / "La imagen es demasiado grande...") y "Volver al encuadre" regresa al Encuadre (o a PermisoPendiente sin permiso). No se guarda ninguna foto ni queda fichero huérfano.

### Scenario 6: Formatos alternativos
Given PNG, WebP o HEIC (HEIC solo API >= 28)
When se elige
Then se decodifica con `BitmapFactory` y se guarda como JPEG; si el dispositivo no puede decodificarla aplica el Scenario 5.

### Scenario 7: Privacidad
Given el selector del sistema
Then la app solo recibe el `Uri` de la imagen elegida (acceso temporal); el APK no añade permisos al manifest fusionado.

## Repository Research (inspeccionado)

- `feature/escanear/.../EscanearViewModel.kt`: `CapturaUiState` (:72 PermisoPendiente…), `onImageCapturado` (:243) pone `EntradaCaptura.Cruda(bytes, crop)`; `resultadoDeCaptura` (:480) emite Procesando → `guardarFotoCapturada(bytes, crop)` → `reconocerTextoCrudo(ruta)` (fallo de OCR no bloquea); `estadoDe` (:639) devuelve `PermisoPendiente` si `!permiso` ANTES de mirar la captura; `onRepetirFoto` (:451) borra la foto de Revision y pone `Ninguna`; `EntradaCaptura` (:712).
- `EscanearScreen.kt`: `rememberLauncherForActivityResult(RequestPermission)` (:68), `LaunchedEffect` que informa el permiso (:76), `when(estado)` (:126), `PermisoCamaraContent` (:227, usa `EstadoPantalla` con slot, botón `captura_permitir` 48 dp pill).
- `CameraEncuadre.kt`: fila inferior (:162-189) con `Spacer(56.dp)` + obturador `captura_obturador` (80 dp) + `BotonCerrar` (círculo 48 dp translúcido + etiqueta "Cerrar", tag `captura_cerrar`); KDoc :70-72 y comentario :172 dicen que galería es non-goal.
- `domain/expenses/data/PhotoProcessor.kt` (`process(rawJpeg, crop: CropRegion)`), `usecases/GuardarFotoCapturada.kt`, `framework/database/.../source/CapturedPhotoProcessor.kt` (EXIF → rotación 3/6/8 → `recorteEnImagen` → `inSampleSize` → escala ≤1280 → JPEG 85), `photo/PhotoPipelineMath.kt` (`recorteEnImagen` exige `viewAspectRatio > 0`; con aspecto de vista = aspecto de imagen rotada el recorte es identidad), `test/unit/.../FakePhotoProcessor.kt`.
- `app/src/androidTest/.../captura/CapturaFlujoTest.kt` (tags `captura_encuadre`, `captura_obturador`, `captura_permiso`…; `pm grant` por test; cámara virtual); `ProcesadoFlujoTest`; `framework/database/src/androidTest/.../CapturedPhotoProcessorTest.kt`.
- `gradle/libs.versions.toml`: activity-compose 1.13.0 (incluye `PickVisualMedia`), exifinterface 1.4.2, minSdk 26, `androidx-test-espresso-core` ya declarado (no hay `espresso-intents`).
- No inspeccionado: contenido de `ErrorContent`/`EstadoPantalla` (el implementador confirma que los botones se apilan y el texto de "Volver al encuadre"), ni `runCatchingLanzandoCancelacion` (confirmar si captura `OutOfMemoryError`).

## Technical Approach

1. **Puntos de entrada.** Botón secundario en dos sitios, ambos lanzan el mismo launcher de la pantalla:
   - `CameraEncuadre` recibe `onElegirGaleria: () -> Unit`. En el hueco izquierdo (sustituye al `Spacer(56.dp)`): `BotonGaleria` espejo de `BotonCerrar` (círculo 48 dp `Color.White.copy(0.1f)` + borde 1 dp, icono `Icons.Outlined.PhotoLibrary` blanco, etiqueta "Galería" `labelSmall` blanco 80 %), `Role.Button`, `contentDescription = "Elegir de la galería"`, tag `captura_galeria`, siempre habilitado (no depende de `camaraLista`). El hueco izquierdo y `BotonCerrar` mantienen simetría respecto al obturador.
   - `PermisoCamaraContent` recibe `onElegirGaleria`; debajo de "Permitir cámara" un `OutlinedButton` pill 48 dp "Elegir de la galería" con icono + texto (no solo color), tag `permiso_galeria`. El slot ya es una columna: se apila sin lógica nueva; a fontScale 2.0 el texto envuelve (alto mínimo 48 dp, no alto fijo). Copy del mensaje: añadir "O elige una foto de tu galería." Mantener tags existentes.
2. **Estado y permiso (cambio en VM).** `estadoDe`: `!permiso && flujo.captura is CapturaEnProgreso.Ninguna -> PermisoPendiente`; con captura en curso/guardada se ignora el permiso. Así Procesando/Revision/Error funcionan sin cámara, y al volver a `Ninguna` sin permiso se vuelve a PermisoPendiente. Nuevo `fun onImagenElegida(bytes: ByteArray)` = `entradaCaptura.value = EntradaCaptura.Cruda(bytes, CropRegion.Completo)`; para fallo de lectura se reutiliza `onCapturaFallida(mensaje)`. Cancelar el selector no llama al VM.
3. **Recorte: `CropRegion.Completo` (decisión).** Se descarta reutilizar la pantalla de encuadre (UI nueva, fuera de alcance) y cambiar la firma de `PhotoProcessor` (rompe fake y tests). Se añade en dominio `CropRegion.Completo` (companion, `CropRegion(0f,0f,1f,1f, viewAspectRatio = 0f)` documentado como "imagen completa; el aspecto lo resuelve el procesador"). En `CapturedPhotoProcessor.process`, tras calcular `rotAncho/rotAlto`, si `crop == CropRegion.Completo` usa `CropRegion(0f,0f,1f,1f, rotAncho.toFloat()/rotAlto)`. Así no hay acoplamiento: el aspecto sale de EXIF + dimensiones dentro del propio procesador. `PhotoPipelineMath` no cambia. `GuardarFotoCapturada` no cambia.
4. **Lectura del Uri (capa Android en `:feature:escanear`, sin puerto nuevo).** Fichero nuevo `LecturaImagen.kt` (internal): `suspend fun leerBytesAcotado(resolver: ContentResolver, uri: Uri, maxBytes: Int = MAX_BYTES_GALERIA /*32 MiB*/): ByteArray` con `withContext(Dispatchers.IO)`, `openInputStream`, lectura por bloques que aborta con excepción propia si supera el máximo (nunca `readBytes()` sin tope). La pantalla lanza con `rememberCoroutineScope()` desde el callback del launcher: éxito → `viewModel.onImagenElegida(bytes)`; excepción → `viewModel.onCapturaFallida("No se pudo leer la imagen. Prueba con otra foto.")` o "La imagen es demasiado grande. Elige una de menos de 32 MB." Se respeta AGENTS.md: dominio sin Android, VM sin Context ni Compose, ninguna feature conoce a otra, nada nuevo en `framework`. Justificación de no crear caso de uso/puerto: el VM ya recibe `ByteArray` como con la cámara; el `Uri` nunca cruza al dominio.
5. **Launcher.** `rememberLauncherForActivityResult(PickVisualMedia()) { uri -> if (uri != null) leer… }` con `PickVisualMediaRequest(PickVisualMedia.ImageOnly)`. Se declara junto al de permisos. El fallback automático del contrato (documentos/GMS) cubre dispositivos sin Photo Picker.
6. **Errores de decodificación.** Reutilizar el `check` de `CapturedPhotoProcessor` pero cambiar el texto a "La imagen no es válida" (ya no es solo "captura"; revisar tests que lo comparen). Un fallo en `process`/`save` produce `Fallida(mensaje)` → estado Error ya existente. Asegurar que `OutOfMemoryError` en decodificación se convierte en fallo controlado (confirmar `runCatchingLanzandoCancelacion`; si no captura `Error`, capturarlo en el procesador). El `inSampleSize` existente acota la memoria (el bitmap decodificado queda a ~1-2x el objetivo de 1280 px).
7. **Doble toque/rotación.** Mientras el selector está abierto no hay estado nuevo. Si la pantalla sale de composición durante la lectura (rotación), la corrutina se cancela y no pasa nada (lectura de <1 s); documentado como límite aceptado.

## Expected File Changes

- `domain/expenses/.../data/PhotoProcessor.kt`: companion `CropRegion.Completo` + KDoc.
- `framework/database/.../source/CapturedPhotoProcessor.kt`: resolución de `Completo`; texto de error.
- `feature/escanear/.../EscanearViewModel.kt`: `estadoDe`, `onImagenElegida`.
- `feature/escanear/.../EscanearScreen.kt`: launcher de galería, lectura, `PermisoCamaraContent`.
- `feature/escanear/.../CameraEncuadre.kt`: `onElegirGaleria`, `BotonGaleria`, KDoc :70-72 y comentario :172.
- `feature/escanear/.../LecturaImagen.kt`: nuevo (único fichero nuevo en main).
- Tests: ver Verification. Sin cambios en manifest, `libs.versions.toml` salvo la dependencia de test de abajo, Room ni `build-logic/`.

## Visual Design Impact

- Encuadre: botón con estilo del tema de cámara (blanco translúcido sobre vista previa oscura), mismos 48 dp y etiqueta que "Cerrar"; contraste: icono y texto blancos sobre scrim/fondo negro, borde 1 dp como en Cerrar. Etiqueta visible (no solo icono).
- Permiso: botón `OutlinedButton` coherente con `captura_permitir` (pill, 48 dp, `colorScheme.primary`), icono + texto.
- Sin mockup en `docs/design/stitch/` para la galería; se documenta en DESIGN.md como excepción aprobada por el usuario a la regla R4 (decoración ficticia): ahora el botón Galería es funcional y real.

## Durable Documentation Impact

- `docs/specs/captura-foto.md` (:34-37) y `docs/specs/captura-recorte-encuadre.md` (:40-42): quitar "galería" de los non-goals con nota "ver galeria-escaneo".
- KDoc de `CameraEncuadre` (:70-72) y comentario :172: actualizar.
- `CONTEXT.md` (Foto original, :37-39): una foto de galería también puede ser Foto original; es la copia procesada guardada, inmutable; la imagen de la galería no se modifica ni se enlaza; puede no ser una captura reciente.
- `DESIGN.md`: R4 y sección de captura: Galería deja de ser decoración prohibida; define el botón (tema cámara, 48 dp, etiqueta).
- `docs/build-brief.md` (línea 56 "Captura: cámara con encuadre guiado y foto original guardada"): ampliar a "cámara con encuadre guiado o foto elegida de la galería"; la ampliación de alcance la decidió el usuario (anotarlo).
- `docs/architecture.md`: una línea sobre el flujo de captura con dos orígenes (cámara / selector) que convergen en `EntradaCaptura.Cruda`.
- `docs/technical-discovery.md` :32 (nada en MediaStore): sigue válido; anotar que se lee de la galería pero no se escribe.
- `AGENTS.md`: no necesita cambios. `docs/risks-and-open-questions.md`: no necesario.

## Implementation Plan / Tasks

1. `CropRegion.Completo` + rama en `CapturedPhotoProcessor` + texto de error; test instrumentado del procesador.
2. VM: `estadoDe` y `onImagenElegida`; tests unitarios.
3. `LecturaImagen.kt` y launcher en `EscanearScreen`; `PermisoCamaraContent`.
4. `CameraEncuadre`: `BotonGaleria`, parámetro, KDoc.
5. androidTest de flujo (tags aditivos); verificación manual con capturas.
6. Docs; actualizar `feature_list.json`/`PROGRESS.md` (lo hace el orquestador/implementador, no el planner).

## Verification Plan

Base: `gradlew.bat assembleDebug testDebugUnitTest --console=plain` (o `bash init.sh`). Solo emulator-5556: `am instrument` por clase; nunca `connected*`, nunca `pm clear`; no tocar la tablet.

- **Unit (`EscanearViewModelTest`, `FakePhotoProcessor`)**: `onImagenElegida` → Procesando → Revision, y el fake recibe `CropRegion.Completo`; con `permisoConcedido=false` el flujo llega a Revision (no se queda en PermisoPendiente) y `onRepetirFoto` vuelve a PermisoPendiente; `onCapturaFallida` sin permiso → Error; tests existentes de PermisoPendiente intactos; OCR vacío/fallido no bloquea (igual que cámara).
- **Unit puro**: test de `leerBytesAcotado` (Robolectric no disponible: extraer la lógica de tope a función sobre `InputStream` y probarla con `ByteArrayInputStream` mayor/menor que el tope). 
- **Instrumentado framework** (`CapturedPhotoProcessorTest`, amplía): JPEG sintético 4000x3000 con EXIF 6 y `CropRegion.Completo` → salida 960x1280 (vertical), lado largo 1280, sin EXIF; PNG y WebP sintéticos decodifican; bytes basura → excepción "La imagen no es válida".
- **E2E de flujo (`app/.../captura/GaleriaFlujoTest.kt`, nuevo, aditivo)**: sustituye el selector real por un resultado simulado con `androidx.test.espresso:espresso-intents` (androidTest-only, mismo `androidxTestEspresso` en `libs.versions.toml`; única dependencia nueva, justificada porque es la forma oficial de stubear el `Intent` del selector sin tocarlo). `Intents.init()` + `intending(anyOf(hasAction(MediaStore.ACTION_PICK_IMAGES), hasAction(Intent.ACTION_OPEN_DOCUMENT), hasAction(PickVisualMedia.ACTION_SYSTEM_FALLBACK_PICK_IMAGES), hasAction("com.google.android.gms.provider.action.PICK_IMAGES")))`  `.respondWith(ActivityResult(RESULT_OK, Intent().setData(uri)))`. La imagen de prueba se genera en el test (JPEG sintético con texto/EXIF 6, `Bitmap.compress`) y se inserta en MediaStore con el `targetContext` de la app (`RELATIVE_PATH=Pictures/TallyTest`), de modo que la app es dueña y la lee sin permisos; se borra en `@After`. Casos: (1) con `pm grant` CAMERA: `captura_galeria` → `captura_revision` → `captura_foto` visible; (2) cancelar (`RESULT_CANCELED`) → sigue `captura_encuadre`; (3) uri inexistente → `captura_error` y `captura_reintentar` vuelve al encuadre; (4) permiso denegado: primera en orden (`@FixMethodOrder`, estado limpio, como `denegadoMuestraEstadoInAppSinCrash`, con `assumeTrue(!concedido)`): `permiso_galeria` → `captura_revision`, `captura_repetir` → `captura_permiso`. Sigue vigente la limitación de no revocar en caliente (matar proceso).
- **Selector real del sistema (manual, emulator-5556)**: `adb -s emulator-5556 push` de 2-3 imágenes de prueba (una vertical con EXIF, una grande) a `/sdcard/Pictures/TallyTest/` + `am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/TallyTest/<f>.jpg`; abrir Escanear, pulsar el botón, elegir en el Photo Picker real, capturar `adb exec-out screencap -p` en: encuadre con botón, selector, Procesando/Revisión, y permiso denegado (instalación limpia). Tras la prueba borrar con `adb shell rm -r /sdcard/Pictures/TallyTest` y re-escanear. Revisar también fontScale 2.0 en la pantalla de permiso (`adb shell settings put system font_scale 2.0`, restaurar a 1.0).
- Regresión: `CapturaFlujoTest`, `ProcesadoFlujoTest`, `RevisionDatosFlujoTest`, `NavigationTest` sin tocar y en verde.
- Extracción real con la foto de galería: usar el motor ya configurado en el emulador; si el modelo local no está, validar hasta `ExtraccionFallida`/Revisión y registrarlo como límite (no se inventa evidencia).

## Evidence To Capture

- Salida de Gradle (build + unit) y de `am instrument` por clase con recuento de tests.
- Capturas del encuadre con el botón, del selector, de la revisión de foto y del permiso con el botón.
- Dimensiones de la foto guardada (salida del test) con EXIF 6 y con imagen grande.
- Confirmación de que el manifest fusionado no añade permisos (`aapt dump permissions` o `build/intermediates/merged_manifests`).
- Confirmación de limpieza de `/sdcard/Pictures/TallyTest` y de que la tablet no se tocó.

## Decisiones Abiertas (con default)

1. Tope de lectura 32 MiB y mensajes exactos de error: default los de arriba (decisión técnica, no de producto).
2. Etiqueta "Galería" visible bajo el icono vs solo icono: default etiqueta visible (accesibilidad, simetría con "Cerrar").
3. Dependencia `espresso-intents` solo androidTest: default sí; alternativa sin dependencia = solo verificación manual del selector (menos reproducible).
4. Mostrar primero un recorte/encuadre sobre la imagen elegida: default NO (foto completa; el usuario puede repetir con otra). Revisar tras uso real.
5. Fotos espejo EXIF (2/4/5/7): default no corregir (límite heredado).

## Validator Checklist

- [ ] `captura_galeria` y `permiso_galeria` existen, 48 dp, con `contentDescription`/texto, y no se eliminó ningún tag previo.
- [ ] Galería funciona con cámara denegada; Repetir foto/Error devuelve a `captura_permiso`.
- [ ] Cancelar el selector no cambia estado; error de imagen ilegible/grande es claro y recuperable.
- [ ] Foto guardada <=1280 px, orientada, completa, sin EXIF; sin OOM con imagen grande.
- [ ] Sin permisos nuevos, sin módulos nuevos, `domain` sin Android, sin versiones literales.
- [ ] Docs listados actualizados (captura-foto, captura-recorte-encuadre, KDoc, CONTEXT, DESIGN, build-brief, architecture).
- [ ] Evidencia en emulator-5556 y limpieza de imágenes de prueba; la tablet intacta.
