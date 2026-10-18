# Evidencia reintento-otro-motor

E2E `:app` en emulator-5556 (Pixel 10 Pro AVD, 4 GB RAM, sin modelo; el router elige Nube y el boton "Probar con ..." sale deshabilitado con motivo).
Ejecutado con `adb shell am instrument -w -e notClass ExtraccionLocalRealTest,ExtraccionNubeRealTest` (sin `connected*`).

Resultado: `OK (15 tests)` en 48 s: AjustesModeloFlujoTest, CapturaFlujoTest, ProcesadoFlujoTest (incluye
`resultadoOfreceProbarConElOtroMotorYElBadgeCambiaODeshabilitaConMotivo`), RevisionDatosFlujoTest, NavigationTest.

Pendiente (fisico, usuario): Local -> Nube sin nuevo OCR, cancelar a mitad, modelo borrado, error de nube en la Tab S9+.
