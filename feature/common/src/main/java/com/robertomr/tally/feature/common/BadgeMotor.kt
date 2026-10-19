package com.robertomr.tally.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.robertomr.tally.domain.expenses.entities.Motor

/**
 * Badge de motor (DESIGN.md): chip persistente con el motor REAL que extrajo el
 * ticket (en el detalle, `Ticket.motor`). Compartido entre la revision y el
 * detalle; conserva el testTag `revision_badge_motor`.
 */
@Composable
fun BadgeMotor(
    motor: Motor,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainer,
                RoundedCornerShape(9999.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(9999.dp),
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (motor) {
                Motor.LOCAL -> Icons.Outlined.Memory
                Motor.NUBE -> Icons.Outlined.Cloud
            },
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = etiquetaMotorBadge(motor),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("revision_badge_motor"),
        )
    }
}

/** "Motor Nube - Gemini" / "Motor Local - Gemma 3n" (mismo texto que el procesado). */
fun etiquetaMotorBadge(motor: Motor): String = when (motor) {
    Motor.NUBE -> "Motor Nube · Gemini"
    Motor.LOCAL -> "Motor Local · Gemma 3n"
}
