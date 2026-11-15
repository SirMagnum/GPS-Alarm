package com.gpsalarm.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Midnight,
    onPrimary = Mist,
    primaryContainer = MidnightLight,
    onPrimaryContainer = Color.White,
    secondary = Dawn,
    onSecondary = Ink,
    secondaryContainer = Color(0xFFFFE2B8),
    onSecondaryContainer = DawnDeep,
    tertiary = Harbor,
    onTertiary = Mist,
    tertiaryContainer = Color(0xFFBFE6DD),
    onTertiaryContainer = Color(0xFF0E3B33),
    error = Ember,
    onError = Mist,
    background = Mist,
    onBackground = Ink,
    surface = Mist,
    onSurface = Ink,
    surfaceVariant = MistDim,
    onSurfaceVariant = Color(0xFF55555C),
    outline = Color(0xFFADAAA4),
)

private val DarkColors = darkColorScheme(
    primary = MidnightLight,
    onPrimary = Color.White,
    primaryContainer = Midnight,
    onPrimaryContainer = Color(0xFFC7CBEE),
    secondary = Dawn,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF5B3E12),
    onSecondaryContainer = Color(0xFFFFD9A0),
    tertiary = HarborLight,
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF1C4A42),
    onTertiaryContainer = Color(0xFFBFE6DD),
    error = Color(0xFFE2735B),
    onError = Ink,
    background = Ink,
    onBackground = Mist,
    surface = Ink,
    onSurface = Mist,
    surfaceVariant = InkRaised,
    onSurfaceVariant = Color(0xFFB8B6C4),
    outline = Color(0xFF53505F),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// Two families, clearly distinct, each used on purpose rather than everywhere:
//  - Display: a medium-weight humanist sans for titles and headlines — a step more confident
//    than plain body text, built into every Android device (no download, no bundled file).
//  - Numeral: real monospace, used only for the numbers that are the point of an alarm app — a
//    time, a radius, a snooze length — so they read as instrument-precise rather than decorative.
private val DisplayFontFamily = FontFamily(
    Font(DeviceFontFamilyName("sans-serif-medium"), FontWeight.Medium),
    Font(DeviceFontFamilyName("sans-serif"), FontWeight.Normal),
)
val NumeralFontFamily: FontFamily = FontFamily.Monospace

private val baseline = Typography()
private val AppTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = DisplayFontFamily, letterSpacing = (-0.5).sp),
    displayMedium = baseline.displayMedium.copy(fontFamily = DisplayFontFamily, letterSpacing = (-0.4).sp),
    displaySmall = baseline.displaySmall.copy(fontFamily = DisplayFontFamily),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = DisplayFontFamily, letterSpacing = (-0.3).sp),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = DisplayFontFamily, letterSpacing = (-0.2).sp),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = DisplayFontFamily),
    titleLarge = baseline.titleLarge.copy(fontFamily = DisplayFontFamily),
    titleMedium = baseline.titleMedium.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Medium),
    titleSmall = baseline.titleSmall.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge = baseline.bodyLarge.copy(lineHeight = 24.sp),
    labelLarge = baseline.labelLarge.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Medium),
)

@Composable
fun GpsAlarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
