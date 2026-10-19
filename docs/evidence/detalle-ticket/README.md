# Evidencia detalle-ticket (sesión 051)

Capturas tomadas en `emulator-5556` (Pixel_10_Pro, 1280x2856) con un test instrumentado temporal ya eliminado
(datos sembrados con el repositorio real y JPEG real en `filesDir/fotos`, limpiados al terminar).
Revisión visual humana pendiente: el agente solo las ha mirado de forma superficial.

| Fichero | Estado |
| --- | --- |
| `01-detalle-abierto.png` | Detalle abierto: badge de motor, "Ver foto original", formulario y dock (Guardar/Descartar deshabilitados sin cambios), sin barra inferior |
| `02-visor-foto.png` | Visor de foto a pantalla completa (JPEG real decodificado) |
| `03-editando.png` | Comercio/total editados y línea añadida; dock habilitado |
| `04-confirmar-borrado.png` | Diálogo "Eliminar ticket" con comercio e importe (forma 28 dp) |
| `05-foto-ausente.png` | Foto ausente: tarjeta "Foto no disponible"; el resto sigue operativo |
| `06-no-existe.png` | Ticket inexistente: "Este ticket ya no existe" + "Volver" |

## Borrado de la foto (filesDir)

- `DetalleTicketFlujoTest.borrarConConfirmacionEliminaTicketYFicheroDeFoto` comprueba `File(filesDir, ruta).exists()`
  antes (true) y después (false) de confirmar, y `getById == null`.
- Comprobación externa con `adb -s emulator-5556 shell run-as com.robertomr.tally ls files/fotos` muestreado mientras
  corría ese test: `detalle_e2e=0 total=66` -> `detalle_e2e=1 total=67` (foto sembrada) -> `detalle_e2e=0 total=66`
  (borrada por la app al confirmar).
