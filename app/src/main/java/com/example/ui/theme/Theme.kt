package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = LibardinoRed,
    onPrimary = PureWhite,
    primaryContainer = LibardinoRedContainer,
    onPrimaryContainer = OnLibardinoRedContainer,
    secondary = EmeraldSuccess,
    onSecondary = PureWhite,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = OnEmeraldContainer,
    tertiary = AmberAlert,
    onTertiary = PureWhite,
    tertiaryContainer = AmberAlertContainer,
    onTertiaryContainer = OnAmberAlertContainer,
    background = CrispOffWhite,
    onBackground = InkBlack,
    surface = PureWhite,
    onSurface = InkBlack,
    surfaceVariant = SoftGrayContainer,
    onSurfaceVariant = MutedSlate,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = CrispOffWhite,
    surfaceContainer = SoftGrayContainer,
    surfaceContainerHigh = SoftGrayHigh,
    outline = LightOutline,
    outlineVariant = SubtleOutline,
    error = ErrorCrimson,
    onError = PureWhite,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = LibardinoRedLight,
    onPrimary = Color(0xFF4A0003),
    primaryContainer = LibardinoRedDark,
    onPrimaryContainer = LibardinoRedContainer,
    secondary = Color(0xFF4ADE80),
    onSecondary = Color(0xFF052E16),
    secondaryContainer = Color(0xFF14532D),
    onSecondaryContainer = EmeraldContainer,
    tertiary = AmberAlertLight,
    onTertiary = Color(0xFF2E1400),
    tertiaryContainer = AmberAlert,
    onTertiaryContainer = AmberAlertContainer,
    background = LibardinoBlack,
    onBackground = CrispOnDark,
    surface = LibardinoCharcoal,
    onSurface = CrispOnDark,
    surfaceVariant = LibardinoDarkSurface,
    onSurfaceVariant = MutedOnDark,
    surfaceContainerLowest = LibardinoBlack,
    surfaceContainerLow = LibardinoCharcoal,
    surfaceContainer = LibardinoDarkSurface,
    surfaceContainerHigh = LibardinoDarkElevated,
    outline = DarkOutline,
    outlineVariant = Color(0xFF282828),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = ErrorContainerLight
)

val LibardinoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LibardinoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ModaStockTypography,
        shapes = LibardinoShapes,
        content = content
    )
}
