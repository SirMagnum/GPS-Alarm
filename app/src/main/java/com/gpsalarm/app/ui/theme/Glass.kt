package com.gpsalarm.app.ui.theme

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Real glassmorphism, not a tinted rectangle: floating chrome (a top bar, a search field, map
 * controls) shows an actual blurred copy of whatever is scrolling or panning underneath it, with
 * a tint, a light-catch sheen, and a bright hairline edge on top. One ancestor records its content
 * into a [GlassBackdrop] with [Modifier.backdropSource]; any number of floating descendants read
 * that same backdrop with [Modifier.glassPanel]. Below Android 12 the blur silently has no effect
 * (a documented Compose limitation), so the tint there is deliberately stronger to compensate.
 */
@Stable
class GlassBackdrop internal constructor(val layer: GraphicsLayer) {
    var originInWindow: Offset = Offset.Zero
}

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    val density = LocalDensity.current
    return remember(layer) {
        val radiusPx = with(density) { 26.dp.toPx() }
        layer.renderEffect = BlurEffect(radiusPx, radiusPx, TileMode.Decal)
        GlassBackdrop(layer)
    }
}

/** Put on the ancestor whose content (a map, a list, a scrolling column) glass panels should blur. */
fun Modifier.backdropSource(backdrop: GlassBackdrop): Modifier = this
    .onGloballyPositioned { backdrop.originInWindow = it.positionInWindow() }
    .drawWithContent {
        backdrop.layer.record(this, layoutDirection, IntSize(size.width.toInt(), size.height.toInt())) {
            this@drawWithContent.drawContent()
        }
        drawContent()
    }

/**
 * Frosted glass: a real blurred copy of [backdrop] where this panel sits, tinted and bordered.
 * Pass `null` for [backdrop] when there's nothing worth blurring behind it (e.g. a speed-dial
 * pill over the plain matte background) — the tint alone still reads as glass there.
 */
fun Modifier.glassPanel(backdrop: GlassBackdrop?, shape: Shape, isDark: Boolean): Modifier = composed {
    var originInWindow by remember { mutableStateOf(Offset.Zero) }
    val tint = if (isDark) GlassTintDark else GlassTintLight
    val fillAlpha = if (backdrop != null) {
        if (isDark) 0.42f else 0.58f
    } else {
        if (isDark) 0.64f else 0.86f
    }
    val sheenAlpha = if (isDark) 0.16f else 0.60f
    val borderTop = if (isDark) 0.32f else 0.90f

    this
        .onGloballyPositioned { originInWindow = it.positionInWindow() }
        .clip(shape)
        .drawWithContent {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = Path().apply { addOutline(outline) }

            if (backdrop != null) {
                val delta = backdrop.originInWindow - originInWindow
                translate(delta.x, delta.y) {
                    drawLayer(backdrop.layer)
                }
            }
            drawPath(path, color = tint, alpha = fillAlpha)
            drawPath(
                path,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = sheenAlpha), Color.Transparent),
                    start = Offset.Zero,
                    end = Offset(size.width * 0.65f, size.height),
                ),
            )
            drawContent()
        }
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = borderTop), Color.White.copy(alpha = 0.04f)),
            ),
            shape = shape,
        )
}

/** Fixed, guaranteed-contrast ink for text/icons on a glass panel — never a theme surface color. */
fun glassInk(isDark: Boolean): Color = if (isDark) GlassInkOnDark else GlassInkOnLight
fun glassInkMuted(isDark: Boolean): Color = if (isDark) GlassInkMutedOnDark else GlassInkMutedOnLight

/** The barely-there gradient every screen sits on, so glass surfaces have something to sit atop. */
fun matteBackgroundBrush(isDark: Boolean): Brush = Brush.verticalGradient(
    colors = if (isDark) listOf(Ink, InkRaised, InkDeep) else listOf(Mist, MistDim, MistDeep),
)
