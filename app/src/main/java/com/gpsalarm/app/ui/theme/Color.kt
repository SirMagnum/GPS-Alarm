package com.gpsalarm.app.ui.theme

import androidx.compose.ui.graphics.Color

// Midnight — the quiet dark before an alarm fires. Primary; time-alarm chip.
val Midnight = Color(0xFF1E2340)
val MidnightLight = Color(0xFF3A4070)

// Dawn — the moment of waking or arriving. Reserved for the one accent: FAB, active swipe.
val Dawn = Color(0xFFF2A340)
val DawnDeep = Color(0xFFC97E1E)

// Harbor — place, arrival, departure. Location-alarm chip.
val Harbor = Color(0xFF2F7B6E)
val HarborLight = Color(0xFF4FA294)

// Ember — destructive only. Kept rare so red stays meaningful.
val Ember = Color(0xFFC8492E)

// Neutrals
val Mist = Color(0xFFF6F5F2)
val MistDim = Color(0xFFEAE8E3)
val Ink = Color(0xFF121319)
val InkRaised = Color(0xFF1C1E29)

// A third, deeper stop for each neutral: a visible matte gradient behind every screen, so glass
// surfaces have real depth to sit on rather than a flat fill.
val MistDeep = Color(0xFFDAD6CB)
val InkDeep = Color(0xFF07070A)

// Glass — the frosted material itself. Deliberately its own color, not surface-with-alpha, so a
// panel reads as a distinct material once blurred: a cool slate pane in dark, a warm pale one in
// light, each a clear step away from the matte background/surface tones around it.
val GlassTintDark = Color(0xFF2E3350)
val GlassTintLight = Color(0xFFFFFFFF)

// Guaranteed-contrast ink for anything drawn on glass — fixed values, never derived from a theme
// token meant for an opaque surface, so text stays legible regardless of what's blurred behind it.
val GlassInkOnDark = Color(0xFFF6F5F2)
val GlassInkMutedOnDark = Color(0xFFCBCAD6)
val GlassInkOnLight = Color(0xFF14151B)
val GlassInkMutedOnLight = Color(0xFF4E4F58)
