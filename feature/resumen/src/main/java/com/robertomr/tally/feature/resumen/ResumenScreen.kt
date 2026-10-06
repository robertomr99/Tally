package com.robertomr.tally.feature.resumen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp

@Composable
fun ResumenScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        TallyEmblem()
    }
}

@Composable
private fun TallyEmblem() {
    val markColor = MaterialTheme.colorScheme.primaryFixed
    Box(
        modifier = Modifier.size(128.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .width(120.dp)
                .height(96.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 96.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(markColor),
                )
            }
        }
        Box(
            modifier = Modifier
                .requiredSize(width = 144.dp, height = 12.dp)
                .clip(RoundedCornerShape(9999.dp))
                .background(markColor)
                .rotate(-28f),
        )
    }
}
