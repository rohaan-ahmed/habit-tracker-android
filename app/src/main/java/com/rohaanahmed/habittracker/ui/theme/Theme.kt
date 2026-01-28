package com.rohaanahmed.habittracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Extended color scheme for custom colors not in Material3
data class ExtendedColors(
    val successGreen: Color = SuccessGreen,
    val successGreenLight: Color = SuccessGreenLight,
    val successGreenSurface: Color = SuccessGreenSurface,
    val glassWhite: Color = GlassWhite,
    val glassBorder: Color = GlassBorder,
    val glassHighlight: Color = GlassHighlight,
    val vibrantPink: Color = VibrantPink,
    val goldenAmber: Color = GoldenAmber,
    val limeGreen: Color = LimeGreen
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }

private val DarkColorScheme = darkColorScheme(
    primary = NeonMint,
    primaryContainer = SoftLavender,
    secondary = ElectricBlue,
    secondaryContainer = ElectricBlue.copy(alpha = 0.25f),
    tertiary = WarmCoral,
    tertiaryContainer = WarmCoral.copy(alpha = 0.25f),
    background = DeepNight,
    surface = SurfaceDark,
    surfaceVariant = SurfaceElevated,
    onPrimary = DeepNight,
    onPrimaryContainer = DeepNight,
    onSecondary = TextHigh,
    onSecondaryContainer = TextHigh,
    onTertiary = DeepNight,
    onTertiaryContainer = DeepNight,
    onBackground = TextHigh,
    onSurface = TextHigh,
    onSurfaceVariant = TextMuted,
    outline = OutlineDark
)

@Composable
fun HabitTrackerTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val extendedColors = ExtendedColors()

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object ExtendedTheme {
    val colors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}
