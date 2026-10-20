# Evidencia: galeria-escaneo (sesión 067, 2026-10-20)

Todo en `emulator-5556` (Pixel_10_Pro AVD). La tablet y los móviles físicos no se tocaron
(`ANDROID_SERIAL=emulator-5556` en Gradle; adb siempre con `-s emulator-5556`).

## Capturas (reducidas a 480 px de ancho)

| Fichero | Qué muestra |
| --- | --- |
| `01-encuadre-boton-galeria.png` | Encuadre de cámara con el botón **Galería** (círculo 48 dp + etiqueta) a la izquierda del obturador, simétrico a "Cerrar". |
| `02-selector-del-sistema.png` | Photo Picker REAL del sistema abierto desde el botón (imágenes de prueba subidas a `/sdcard/Pictures/TallyTest`). |
| `03-resultado-tras-elegir-exif6.png` | Tras elegir un JPEG apaisado 1600x900 con EXIF 6: "Revisar foto" con el ticket vertical, completo, y el OCR ("Texto crudo (debug)"). |
| `04-error-imagen-ilegible.png` | Fichero `.png` con bytes basura elegido en el selector: estado Error con "La imagen no es válida" y "Volver al encuadre". |
| `05-permiso-con-boton-galeria.png` | Pantalla de permiso de cámara denegado con "Permitir cámara" y el botón "Elegir de la galería". |
| `06-permiso-letra-2-0.png` | La misma pantalla con `font_scale` 2,0: el texto envuelve, ambos botones visibles. |

## Dimensiones de la foto guardada (leída de `files/fotos` vía `run-as`)

- JPEG 1600x900 con EXIF 6 elegido en el selector real: guardada **720x1280** (vertical, completa, escalada a 1280 de lado largo).
- JPEG 6000x4000 (24 MP) elegido en el selector real: guardada **1280x853**, sin OOM.
- Tests instrumentados (`CapturedPhotoProcessorTest`): 4000x3000 con EXIF 6 y `CropRegion.Completo` -> 960x1280 sin tag de
  orientación; EXIF 8 2000x1000 -> 640x1280; PNG y WebP decodifican; bytes basura -> `IllegalStateException("La imagen no es válida")`.

## Cómo se verificó el caso "sin permiso de cámara"

No se revoca el permiso con la app en marcha (Android mata el proceso). Se hace `am force-stop`, `pm revoke
com.robertomr.tally android.permission.CAMERA` con la app parada y después se lanza la app / la prueba:
`GaleriaFlujoTest.a_sinPermisoDeCamaraLaGaleriaLlegaARevisionYRepetirVuelveAlPermiso` corrió con CAMERA denegada
(comprobado con `dumpsys package`: `granted=false`) y las capturas 05/06 se tomaron en ese estado. Al terminar se volvió a
conceder el permiso (estado previo del emulador). El selector del sistema no se pulsó manualmente sin permiso (lo cubre el
E2E con el selector simulado).

## Manifest

El manifest fusionado de debug no contiene `READ_MEDIA_IMAGES` ni `READ_EXTERNAL_STORAGE`
(`app/build/intermediates/merged_manifests/debug/.../AndroidManifest.xml`); `git diff` no toca `AndroidManifest.xml`.

## Limpieza

`/sdcard/Pictures/TallyTest` borrado (`rm -r`) y re-escaneado; sin entradas `TallyTest` en MediaStore. El E2E borra su
imagen de MediaStore en `@After`. `font_scale` restaurado a 1.0. Sin vídeos ni tests temporales.
