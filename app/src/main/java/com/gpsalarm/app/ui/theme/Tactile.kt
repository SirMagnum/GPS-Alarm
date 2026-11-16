package com.gpsalarm.app.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The core "arcade cabinet" primitive from design §3A/§2.4: a flat shape sitting on a hard,
 * un-blurred offset duplicate of itself. [pressed] collapses that offset to 0 while the real
 * surface slides down-right onto it — the button visually sinks into the cabinet rather than
 * just dimming or scaling.
 *
 * Uses a two-content [Layout] rather than a plain `Box { shadow; surface }` so [modifier] (size,
 * `.align()`, `.clickable()`, …) lands on the one node this composable's caller's own parent
 * actually sees and positions — attaching it to the inner surface box instead (a grandchild)
 * would make `.align()` from a caller silently no-op, since Box alignment only reads parentData
 * from its direct children.
 */
@Composable
fun TactileSurface(
    shape: Shape,
    fillColor: Color,
    outlineColor: Color,
    shadowColor: Color,
    pressed: Boolean,
    modifier: Modifier = Modifier,
    shadowOffset: Dp = 4.dp,
    borderWidth: Dp = 1.5.dp,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit,
) {
    val sink by animateDpAsState(
        targetValue = if (pressed) shadowOffset else 0.dp,
        animationSpec = tween(if (pressed) 140 else 180),
        label = "tactileSink",
    )
    val density = LocalDensity.current
    val shadowOffsetPx = with(density) { shadowOffset.roundToPx() }
    val sinkPx = with(density) { sink.roundToPx() }

    val shadowContent: @Composable () -> Unit = {
        Box(Modifier.clip(shape).background(shadowColor))
    }
    val surfaceContent: @Composable () -> Unit = {
        Box(
            Modifier.clip(shape).background(fillColor).border(borderWidth, outlineColor, shape),
            contentAlignment = contentAlignment,
        ) { content() }
    }

    Layout(contents = listOf(shadowContent, surfaceContent), modifier = modifier) { measurables, constraints ->
        val (shadowMeasurables, surfaceMeasurables) = measurables
        // The surface is sized by the incoming constraints (whatever modifier asked for, or its
        // own intrinsic content size if modifier asked for nothing) — the shadow just matches it.
        val surfacePlaceable = surfaceMeasurables.first().measure(constraints)
        val shadowPlaceable = shadowMeasurables.first().measure(
            Constraints.fixed(surfacePlaceable.width, surfacePlaceable.height),
        )
        layout(surfacePlaceable.width, surfacePlaceable.height) {
            shadowPlaceable.placeRelative(shadowOffsetPx, shadowOffsetPx)
            surfacePlaceable.placeRelative(sinkPx, sinkPx)
        }
    }
}

private fun Modifier.offsetBy(x: Dp, y: Dp): Modifier = this.then(Modifier.offset(x = x, y = y))

/** A single-word, all-caps tactile button — Save, Dismiss, Snooze. */
@Composable
fun TactileButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillColor: Color = arcadeColors.accentPrimary,
    textColor: Color = Color.White,
    outlined: Boolean = false,
    height: Dp = 56.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val resolvedFill = if (outlined) Color.Transparent else fillColor
    val resolvedText = if (outlined) arcadeColors.textPrimary else textColor

    TactileSurface(
        shape = RoundedCornerShape(ArcadeRadius.sm),
        fillColor = resolvedFill,
        outlineColor = arcadeColors.outline,
        shadowColor = arcadeColors.outline,
        pressed = pressed,
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(height)
            .clickable(interactionSource = interactionSource, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
    ) {
        Text(text.uppercase(), style = arcadeType.button, color = resolvedText)
    }
}

/** A segmented-control option (Settings' Theme/Map/Snooze rows) — same hard outline+shadow
 *  treatment as every other tappable element, so it doesn't read as flatter/lower-contrast. */
@Composable
fun TactileChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    TactileSurface(
        shape = RoundedCornerShape(ArcadeRadius.sm),
        fillColor = if (selected) arcadeColors.accentSecondary else arcadeColors.bgSurfaceRaised,
        outlineColor = arcadeColors.outline,
        shadowColor = arcadeColors.outline,
        pressed = pressed,
        shadowOffset = 3.dp,
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(44.dp)
            .clickable(interactionSource = interactionSource, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
    ) {
        Text(
            text,
            style = arcadeType.body,
            color = if (selected) arcadeColors.bgBase else arcadeColors.textPrimary,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
    }
}

/** The on/off alarm toggle: a flip switch with hard-cut corners, not an iOS-style pill. */
@Composable
fun TactileToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val trackColor = if (checked) arcadeColors.accentSecondary else arcadeColors.bgBase
    val knobOffset by animateDpAsState(if (checked) 22.dp else 0.dp, animationSpec = tween(90), label = "toggleKnob")

    Box(
        modifier = modifier
            .size(width = 50.dp, height = 28.dp)
            .clip(RoundedCornerShape(ArcadeRadius.sm))
            .background(trackColor)
            .border(1.5.dp, arcadeColors.outline, RoundedCornerShape(ArcadeRadius.sm))
            .clickable(interactionSource = interactionSource, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            }
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offsetBy(knobOffset, 0.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) Color.White else arcadeColors.textDisabled)
                .border(1.dp, arcadeColors.outline, RoundedCornerShape(6.dp)),
        )
    }
}

