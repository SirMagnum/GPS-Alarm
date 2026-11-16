package com.gpsalarm.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpsalarm.app.R

// Three named families per the design spec. "Cabinet Grotesk" / "Departure Mono" / "General Sans"
// aren't Google Fonts and can't be safely bundled sight-unseen, so each uses the spec's own named
// fallback instead, all OFL-licensed and bundled as real files under res/font/:
//  - Display -> Bricolage Grotesque (an explicit alternate in the spec)
//  - Ticker   -> Space Mono (an explicit fallback in the spec)
//  - UI       -> Plus Jakarta Sans (a warm humanist grotesk, closest free equivalent to General Sans/Switzer)
val DisplayFontFamily = FontFamily(
    Font(R.font.bricolage_bold, FontWeight.Bold),
    Font(R.font.bricolage_extrabold, FontWeight.ExtraBold),
)
val TickerFontFamily = FontFamily(
    Font(R.font.space_mono_regular, FontWeight.Normal),
    Font(R.font.space_mono_bold, FontWeight.Bold),
)
val UiFontFamily = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
)

/** Named type roles from §2.2 of the design spec — not Material's generic display/body/label scale. */
data class ArcadeType(
    val displayTime: androidx.compose.ui.text.TextStyle,
    val alarmTime: androidx.compose.ui.text.TextStyle,
    val ticker: androidx.compose.ui.text.TextStyle,
    val title: androidx.compose.ui.text.TextStyle,
    val body: androidx.compose.ui.text.TextStyle,
    val button: androidx.compose.ui.text.TextStyle,
)

private val AppArcadeType = ArcadeType(
    displayTime = androidx.compose.ui.text.TextStyle(
        fontFamily = DisplayFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 76.sp, lineHeight = 80.sp,
    ),
    alarmTime = androidx.compose.ui.text.TextStyle(
        fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp,
    ),
    ticker = androidx.compose.ui.text.TextStyle(
        fontFamily = TickerFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp,
    ),
    title = androidx.compose.ui.text.TextStyle(
        fontFamily = UiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp,
    ),
    body = androidx.compose.ui.text.TextStyle(
        fontFamily = UiFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp,
    ),
    button = androidx.compose.ui.text.TextStyle(
        fontFamily = UiFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.3.sp,
    ),
)

/** Corner radius scale from §2.3. */
object ArcadeRadius {
    val sm = 12.dp
    val md = 20.dp
    val lg = 28.dp
}

/** Motion tokens from §3.1. */
object ArcadeMotion {
    const val micro = 120
    const val standard = 280
    const val container = 400
    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val spring = spring<Float>(stiffness = 400f, dampingRatio = Spring.DampingRatioMediumBouncy)
}

val LocalArcadeColors = staticCompositionLocalOf { MarqueeDay }
val LocalArcadeType: ProvidableCompositionLocal<ArcadeType> = staticCompositionLocalOf { AppArcadeType }

/** Convenience accessors so screens read `MaterialTheme`-style: `arcadeColors.accentPrimary`. */
val arcadeColors: ArcadePalette
    @Composable get() = LocalArcadeColors.current
val arcadeType: ArcadeType
    @Composable get() = LocalArcadeType.current

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(ArcadeRadius.sm),
    small = RoundedCornerShape(ArcadeRadius.sm),
    medium = RoundedCornerShape(ArcadeRadius.md),
    large = RoundedCornerShape(ArcadeRadius.md),
    extraLarge = RoundedCornerShape(ArcadeRadius.lg),
)

// Every slot explicitly set to one of our three families — a default Typography() baseline
// mixes in the system font for any slot left unset, which is exactly how the stock Material3
// TimePicker (reading displayMedium) silently fell back to it before that widget was replaced.
private fun materialTypography(): Typography {
    val display = AppArcadeType.displayTime
    val alarm = AppArcadeType.alarmTime
    val ticker = AppArcadeType.ticker
    val title = AppArcadeType.title
    val body = AppArcadeType.body
    val button = AppArcadeType.button
    return Typography(
        displayLarge = display,
        displayMedium = alarm,
        displaySmall = alarm,
        headlineLarge = alarm,
        headlineMedium = alarm,
        headlineSmall = title,
        titleLarge = title,
        titleMedium = title,
        titleSmall = title,
        bodyLarge = body,
        bodyMedium = body,
        bodySmall = ticker,
        labelLarge = button,
        labelMedium = button,
        labelSmall = ticker,
    )
}

private fun materialColorScheme(palette: ArcadePalette, dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = palette.accentPrimary,
        onPrimary = Color.White,
        primaryContainer = palette.accentPrimary,
        onPrimaryContainer = Color.White,
        secondary = palette.accentSecondary,
        onSecondary = palette.bgBase,
        secondaryContainer = palette.accentSecondary,
        onSecondaryContainer = palette.bgBase,
        tertiary = palette.accentGlow,
        onTertiary = palette.bgBase,
        tertiaryContainer = palette.accentGlow,
        onTertiaryContainer = palette.bgBase,
        error = palette.danger,
        onError = Color.White,
        background = palette.bgBase,
        onBackground = palette.textPrimary,
        surface = palette.bgSurface,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.bgSurfaceRaised,
        onSurfaceVariant = palette.textSecondary,
        outline = palette.outline,
    )
} else {
    lightColorScheme(
        primary = palette.accentPrimary,
        onPrimary = Color.White,
        primaryContainer = palette.accentPrimary,
        onPrimaryContainer = Color.White,
        secondary = palette.accentSecondary,
        onSecondary = Color.White,
        secondaryContainer = palette.accentSecondary,
        onSecondaryContainer = Color.White,
        tertiary = palette.accentGlow,
        onTertiary = palette.textPrimary,
        tertiaryContainer = palette.accentGlow,
        onTertiaryContainer = palette.textPrimary,
        error = palette.danger,
        onError = Color.White,
        background = palette.bgBase,
        onBackground = palette.textPrimary,
        surface = palette.bgSurface,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.bgSurfaceRaised,
        onSurfaceVariant = palette.textSecondary,
        outline = palette.outline,
    )
}

@Composable
fun GpsAlarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) MarqueeNight else MarqueeDay
    CompositionLocalProvider(LocalArcadeColors provides palette) {
        MaterialTheme(
            colorScheme = materialColorScheme(palette, darkTheme),
            shapes = AppShapes,
            typography = materialTypography(),
            content = content,
        )
    }
}
