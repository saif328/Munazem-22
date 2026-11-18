package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = SkyPrimary,
    onPrimary = NavyDeep,
    primaryContainer = NavyElevated,
    onPrimaryContainer = SkyBright,
    secondary = ElectricBlue,
    onSecondary = NavyDeep,
    secondaryContainer = NavyCard,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = IndigoAccent,
    onTertiary = TextPrimaryDark,
    background = NavyDeep,
    onBackground = TextPrimaryDark,
    surface = NavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NavyCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0x337DD3FC),
    error = CrimsonUrgent,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BluePrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EEFF),
    onPrimaryContainer = LightNavyText,
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = LightCard,
    onSecondaryContainer = LightNavyText,
    tertiary = IndigoAccent,
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightNavyText,
    surface = LightSurface,
    onSurface = LightNavyText,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightSecondaryText,
    outline = Color(0x330284C7),
    error = CrimsonUrgent,
    onError = Color.White
)

val MunazzemShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

data class GlassColors(
    val cardBackground: Color,
    val cardBorder: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val isDark: Boolean
)

val LocalGlassColors = staticCompositionLocalOf {
    GlassColors(
        cardBackground = Color(0xAA132043),
        cardBorder = Color(0x337DD3FC),
        gradientStart = NavyDeep,
        gradientEnd = Color(0xFF0D1D42),
        isDark = true
    )
}

@Composable
fun MunazzemTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val glassColors = if (darkTheme) {
        GlassColors(
            cardBackground = Color(0xB3121E3D),
            cardBorder = Color(0x387DD3FC),
            gradientStart = NavyDeep,
            gradientEnd = Color(0xFF0E1D42),
            isDark = true
        )
    } else {
        GlassColors(
            cardBackground = Color(0xCCFFFFFF),
            cardBorder = Color(0x330284C7),
            gradientStart = Color(0xFFEAF3FF),
            gradientEnd = Color(0xFFF6FAFF),
            isDark = false
        )
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalGlassColors provides glassColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MunazzemTypography,
            shapes = MunazzemShapes,
            content = content
        )
    }
}
