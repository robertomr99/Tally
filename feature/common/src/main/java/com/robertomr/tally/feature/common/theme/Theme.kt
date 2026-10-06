package com.robertomr.tally.feature.common.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val TallyColorScheme = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = ForestOnPrimary,
    primaryContainer = ForestPrimaryContainer,
    onPrimaryContainer = ForestOnPrimaryContainer,
    primaryFixed = ForestPrimaryFixed,
    primaryFixedDim = ForestPrimaryFixedDim,
    onPrimaryFixed = ForestOnPrimaryFixed,
    onPrimaryFixedVariant = ForestOnPrimaryFixedVariant,
    secondary = ForestSecondary,
    onSecondary = ForestOnSecondary,
    secondaryContainer = ForestSecondaryContainer,
    onSecondaryContainer = ForestOnSecondaryContainer,
    secondaryFixed = ForestSecondaryFixed,
    secondaryFixedDim = ForestSecondaryFixedDim,
    onSecondaryFixed = ForestOnSecondaryFixed,
    onSecondaryFixedVariant = ForestOnSecondaryFixedVariant,
    tertiary = ForestTertiary,
    onTertiary = ForestOnTertiary,
    tertiaryContainer = ForestTertiaryContainer,
    onTertiaryContainer = ForestOnTertiaryContainer,
    tertiaryFixed = ForestTertiaryFixed,
    tertiaryFixedDim = ForestTertiaryFixedDim,
    onTertiaryFixed = ForestOnTertiaryFixed,
    onTertiaryFixedVariant = ForestOnTertiaryFixedVariant,
    error = ForestError,
    onError = ForestOnError,
    errorContainer = ForestErrorContainer,
    onErrorContainer = ForestOnErrorContainer,
    background = ForestBackground,
    onBackground = ForestOnBackground,
    surface = ForestSurface,
    onSurface = ForestOnSurface,
    surfaceVariant = ForestSurfaceVariant,
    onSurfaceVariant = ForestOnSurfaceVariant,
    surfaceDim = ForestSurfaceDim,
    surfaceBright = ForestSurfaceBright,
    surfaceContainerLowest = ForestSurfaceContainerLowest,
    surfaceContainerLow = ForestSurfaceContainerLow,
    surfaceContainer = ForestSurfaceContainer,
    surfaceContainerHigh = ForestSurfaceContainerHigh,
    surfaceContainerHighest = ForestSurfaceContainerHighest,
    inverseSurface = ForestInverseSurface,
    inverseOnSurface = ForestInverseOnSurface,
    inversePrimary = ForestInversePrimary,
    surfaceTint = ForestSurfaceTint,
    outline = ForestOutline,
    outlineVariant = ForestOutlineVariant,
    scrim = ForestScrim,
)

private val TallyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(9999.dp),
)

@Composable
fun TallyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TallyColorScheme,
        typography = TallyTypography,
        shapes = TallyShapes,
        content = content,
    )
}
