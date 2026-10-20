package com.robertomr.tally

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.robertomr.tally.feature.common.theme.TallyTheme
import com.robertomr.tally.navigation.TallyApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Debe ir ANTES de super.onCreate: aplica postSplashScreenTheme.
        val splash = installSplashScreen()

        // Hold solo en arranque en frio: sin estado guardado y primera actividad
        // del proceso (SplashPolitica, spec splash-animado D3/D4/D5).
        val esPrimeraCreacion = savedInstanceState == null && !SplashPolitica.yaMostrado
        SplashPolitica.yaMostrado = true
        val inicio = SystemClock.uptimeMillis()
        val escala = Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        splash.setKeepOnScreenCondition {
            SplashPolitica.debeMantener(
                esPrimeraCreacion = esPrimeraCreacion,
                sdkInt = Build.VERSION.SDK_INT,
                escalaAnimacion = escala,
                transcurridoMs = SystemClock.uptimeMillis() - inicio,
            )
        }

        super.onCreate(savedInstanceState)
        // Edge-to-edge con barras transparentes e iconos OSCUROS: la app solo tiene
        // tema claro, así que el reloj y los iconos no deben ser blancos aunque el
        // sistema esté en modo oscuro (mismo aspecto en API < 35 y >= 35).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            TallyTheme {
                TallyApp()
            }
        }
    }
}
