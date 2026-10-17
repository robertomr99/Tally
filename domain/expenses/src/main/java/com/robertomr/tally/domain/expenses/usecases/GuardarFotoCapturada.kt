package com.robertomr.tally.domain.expenses.usecases

import com.robertomr.tally.domain.expenses.data.CropRegion
import com.robertomr.tally.domain.expenses.data.PhotoFileStore
import com.robertomr.tally.domain.expenses.data.PhotoProcessor
import javax.inject.Inject

/**
 * Guarda la foto capturada en el momento de la captura (spec captura-foto):
 * procesa los bytes JPEG crudos (recorte del encuadre + orientacion EXIF +
 * ~1280 px + JPEG ~85) y los escribe en el almacenamiento privado, devolviendo
 * el nombre relativo.
 *
 * La foto es la evidencia inmutable del ticket: se guarda aunque el Borrador
 * nunca llegue a persistirse (el Borrador vive solo en memoria).
 */
class GuardarFotoCapturada @Inject constructor(
    private val photoProcessor: PhotoProcessor,
    private val photoFileStore: PhotoFileStore,
) {

    /**
     * @return nombre relativo bajo `filesDir` de la foto ya guardada
     * (nunca una ruta absoluta; contrato de [PhotoFileStore.save]).
     */
    suspend operator fun invoke(rawJpeg: ByteArray, crop: CropRegion): String {
        val jpeg = photoProcessor.process(rawJpeg, crop)
        return photoFileStore.save(jpeg)
    }
}
