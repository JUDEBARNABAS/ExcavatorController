package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ExcavatorDarkColorScheme = darkColorScheme(
    primary = IndustrialYellow,
    onPrimary = DarkBackground,
    primaryContainer = IndustrialAmber,
    onPrimaryContainer = TextPrimary,
    secondary = BleBlue,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = IndustrialAmber,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceBorder,
    error = IndustrialRed,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ExcavatorDarkColorScheme,
        typography = Typography,
        content = content
    )
}
