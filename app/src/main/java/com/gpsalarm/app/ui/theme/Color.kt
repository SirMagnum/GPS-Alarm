package com.gpsalarm.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "Marquee" palette — a lit-up arcade marquee: warm neutral base, punchy saturated accents, one
 * true "glow" color reserved for the alarm-ringing state. Light and dark are independently tuned,
 * not one inverted into the other. No blue/indigo/purple/navy anywhere, by design.
 */
data class ArcadePalette(
    val bgBase: Color,
    val bgSurface: Color,
    val bgSurfaceRaised: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val accentGlow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val danger: Color,
    val outline: Color,
)

/** Palette A — "Marquee Day": warm paper base, ink text, candy-bright accents. */
val MarqueeDay = ArcadePalette(
    bgBase = Color(0xFFFAF4E8),
    bgSurface = Color(0xFFFFFFFF),
    bgSurfaceRaised = Color(0xFFFFFFFF),
    accentPrimary = Color(0xFFFF4B2B),
    accentSecondary = Color(0xFF1FAE6E),
    accentGlow = Color(0xFFFFC53D),
    textPrimary = Color(0xFF1C1710),
    textSecondary = Color(0xFF8A7F6D),
    textDisabled = Color(0xFFC9BFA9),
    danger = Color(0xFFD62839),
    outline = Color(0xFF1C1710),
)

/** Palette B — "Marquee Night". */
val MarqueeNight = ArcadePalette(
    bgBase = Color(0xFF14100C),
    bgSurface = Color(0xFF1F1A14),
    bgSurfaceRaised = Color(0xFF28221A),
    accentPrimary = Color(0xFFFF6A3D),
    accentSecondary = Color(0xFF3EE68C),
    accentGlow = Color(0xFFFFD23F),
    textPrimary = Color(0xFFF6EFE2),
    textSecondary = Color(0xFFA89A84),
    textDisabled = Color(0xFF564C3E),
    danger = Color(0xFFFF5C5C),
    outline = Color(0xFF0A0806),
)
