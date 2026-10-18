# Como ejecutar el gate fisico en la Tab S9+

1. Crear el directorio como la app: `adb -s <serial> shell run-as com.robertomr.tally mkdir /sdcard/Android/data/com.robertomr.tally/files/models` (si lo crea el shell, la app lo ve como "No descargado" y los tests se saltan). Modelo en `/sdcard/Android/data/com.robertomr.tally/files/models/gemma-3n-E2B-it-int4.litertlm`
   (descargar desde Ajustes o `adb -s <serial> push` desde `E:\AI-Expert\Modelos\`).
2. `set ANDROID_SERIAL=<serial Tab S9+>` y `gradlew installDebug installDebugAndroidTest`
   (NO uses `connected*AndroidTest`: desinstala la app y borra el modelo de 3,4 GB).
3. `adb -s <serial> logcat -c`
4. `adb -s <serial> shell am instrument -w -e class com.robertomr.tally.extraccion.ExtraccionLocalRealTest com.robertomr.tally.test/androidx.test.runner.AndroidJUnitRunner`
5. `adb -s <serial> logcat -d -s ExtraccionLocal:I > docs/evidence/extraccion-local/tab_s9_logcat.txt`
6. Rellenar la tabla de `latencias.md` (secciones Tab S9+), con contraste de totales
   (35,30 / 3,50 / 13,90 / 7,91) y anotar aciertos.

Nota: no ejecutar `AjustesModeloFlujoTest` (borra el modelo).
