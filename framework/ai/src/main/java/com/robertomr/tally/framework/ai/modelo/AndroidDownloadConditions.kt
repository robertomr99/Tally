package com.robertomr.tally.framework.ai.modelo

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.StatFs
import com.robertomr.tally.domain.expenses.data.DownloadConditions
import java.io.File

/**
 * Red no medida (`NET_CAPABILITY_NOT_METERED`) y espacio libre en el volumen
 * del modelo (spec descarga-modelo, escenarios 3 y 4). Un WiFi marcado como
 * medido o un hotspot cuenta como "no WiFi" (R-DM6).
 */
internal class AndroidDownloadConditions(private val context: Context) : DownloadConditions {

    override fun esRedSinCoste(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val red = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(red) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    override fun espacioLibreBytes(): Long {
        var dir: File? = ModelPaths.directorio(context)
        while (dir != null && !dir.exists()) dir = dir.parentFile
        return StatFs((dir ?: context.filesDir).path).availableBytes
    }

    override fun bytesYaDescargados(): Long {
        val parcial = ModelPaths.archivoParcial(context)
        return if (parcial.isFile) parcial.length() else 0L
    }
}
