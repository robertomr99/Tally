package com.robertomr.tally

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.robertomr.tally.feature.common.theme.TallyTheme
import com.robertomr.tally.feature.resumen.ResumenScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TallyTheme {
                ResumenScreen()
            }
        }
    }
}
