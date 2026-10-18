# Evidencia ajustes-motor (2026-10-18)

Solo emulador `emulator-5556`; no se uso ningun dispositivo fisico. No se leyo ni imprimio el token de Hugging Face.

## JVM
`gradlew.bat assembleDebug testDebugUnitTest :domain:expenses:test :test:unit:test --rerun-tasks` -> BUILD SUCCESSFUL.
- `EngineRouterTest` 16/16 (matriz de preferencia, Local forzado sin modelo/no capaz sin invocar motores,
  Nube ignora capacidad, relectura en cada extraccion, Nube con error de red sin fallback).
- `PreferenciaMotorUseCasesTest` 2/2, `AjustesViewModelTest` 13/13, `SharedPreferencesPreferenciaMotorTest` 4/4
  (AUTO por defecto/valor invalido, persistencia entre instancias), `EscanearViewModelTest` 44/44.

## Instrumentado (`am instrument`, sin connected*)
- `.\gradlew.bat :app:installDebug :app:installDebugAndroidTest` con `ANDROID_SERIAL=emulator-5556`.
- `-e class ...AjustesMotorFlujoTest` -> OK (3 tests): tres opciones visibles, seleccion + escritura en disco,
  aviso `motor_aviso_local` con Local sin modelo.
- Suite `:app` con `-e notClass ...ExtraccionLocalRealTest,...ExtraccionNubeRealTest` -> OK (18 tests).
- Persistencia: elegir Local, `am force-stop`, relanzar, abrir Ajustes -> el aviso de Local sigue visible
  (`ajustes-tras-force-stop-local.xml`, `ajustes-local-persistido-tras-reinicio.png`). Despues se restauro
  AUTO (`shared_prefs/tally_ajustes.xml` -> `AUTO`).

## No verificado (pendiente, usuario)
Tab S9+: Nube forzada -> badge Nube en la siguiente extraccion y viceversa; Local sin modelo -> error visible
con "Probar con Nube"; Nube sin red; persistencia tras reinicio real. E2E de extraccion forzada cubierto solo
por tests JVM del router/ViewModel.
