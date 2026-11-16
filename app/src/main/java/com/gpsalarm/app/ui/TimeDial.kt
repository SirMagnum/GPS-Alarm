package com.gpsalarm.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gpsalarm.app.ui.theme.ArcadeRadius
import com.gpsalarm.app.ui.theme.arcadeColors
import com.gpsalarm.app.ui.theme.arcadeType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A fully self-contained time picker: one [hour24]/[minute] pair of state, read directly by the
 * digit display, the AM/PM pill, and the dial's hand — so none of them can ever disagree with
 * each other or with the alarm being edited, unlike Material3's TimePicker (which hides its own
 * state inside TimePickerState and turned out to desync under heavy theming).
 */
@Composable
fun TimeDial(hour24: Int, minute: Int, onHour24Change: (Int) -> Unit, onMinuteChange: (Int) -> Unit) {
    val colors = arcadeColors
    var editingHour by remember { mutableStateOf(true) }
    val isPm = hour24 >= 12
    val hourOf12 = when (hour24 % 12) { 0 -> 12; else -> hour24 % 12 }

    fun setHourOf12(newHourOf12: Int) {
        val h = newHourOf12 % 12
        onHour24Change(if (isPm) h + 12 else h)
    }

    fun setPm(pm: Boolean) {
        onHour24Change(if (pm) hourOf12 % 12 + 12 else hourOf12 % 12)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeDigitBox(
                text = "%02d".format(hourOf12),
                selected = editingHour,
                onClick = { editingHour = true },
            )
            Text(
                ":",
                style = arcadeType.displayTime.copy(fontSize = 48.sp),
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            TimeDigitBox(
                text = "%02d".format(minute),
                selected = !editingHour,
                onClick = { editingHour = false },
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AmPmPill(text = "AM", selected = !isPm, onClick = { setPm(false) })
                AmPmPill(text = "PM", selected = isPm, onClick = { setPm(true) })
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        ClockFace(
            editingHour = editingHour,
            hourOf12 = hourOf12,
            minute = minute,
            onHourOf12Change = ::setHourOf12,
            onMinuteChange = onMinuteChange,
            onHourCommitted = { editingHour = false },
        )
    }
}

@Composable
private fun TimeDigitBox(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = arcadeColors
    Box(
        modifier = Modifier
            .size(width = 84.dp, height = 72.dp)
            .clip(RoundedCornerShape(ArcadeRadius.sm))
            .background(if (selected) colors.accentPrimary else colors.bgSurfaceRaised)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = arcadeType.displayTime.copy(fontSize = 36.sp),
            color = if (selected) Color.White else colors.textPrimary,
        )
    }
}

@Composable
private fun AmPmPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = arcadeColors
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 32.dp)
            .clip(RoundedCornerShape(ArcadeRadius.sm))
            .background(if (selected) colors.accentSecondary else colors.bgSurfaceRaised)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = arcadeType.ticker, color = if (selected) colors.bgBase else colors.textSecondary)
    }
}

@Composable
private fun ClockFace(
    editingHour: Boolean,
    hourOf12: Int,
    minute: Int,
    onHourOf12Change: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onHourCommitted: () -> Unit,
) {
    val colors = arcadeColors
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 16.sp, color = colors.textPrimary)
    val knobLabelStyle = TextStyle(fontSize = 16.sp, color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    val diameter = 260.dp

    fun angleToHour(deg: Float): Int {
        val idx = (deg / 30f).roundToInt() % 12
        return if (idx == 0) 12 else idx
    }
    fun angleToMinute(deg: Float): Int = (deg / 6f).roundToInt() % 60

    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(colors.bgSurfaceRaised)
            .pointerInput(editingHour) {
                fun handle(offset: Offset) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = offset.x - center.x
                    val dy = offset.y - center.y
                    val deg = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 90 + 360) % 360).toFloat()
                    if (editingHour) {
                        onHourOf12Change(angleToHour(deg))
                    } else {
                        onMinuteChange(angleToMinute(deg))
                    }
                }
                detectDragGestures(
                    onDragStart = { offset -> handle(offset) },
                    onDragEnd = { if (editingHour) onHourCommitted() },
                    onDrag = { change, _ -> handle(change.position) },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val labelRadius = radius * 0.78f

            // Tick labels: hour mode shows 1-12, minute mode shows 0,5,...,55.
            for (i in 0 until 12) {
                val value = if (editingHour) (if (i == 0) 12 else i) else i * 5
                val angleRad = Math.toRadians(i * 30.0 - 90.0)
                val x = center.x + labelRadius * cos(angleRad).toFloat()
                val y = center.y + labelRadius * sin(angleRad).toFloat()
                val layout: TextLayoutResult = textMeasurer.measure("%d".format(value), labelStyle)
                drawText(textLayoutResult = layout, topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f))
            }

            // The hand + knob read hour/minute directly — the single visual proof that this
            // state is shared with the digit boxes rather than tracked separately.
            val handValue = if (editingHour) hourOf12 % 12 else minute
            val stepDeg = 360f / if (editingHour) 12 else 60
            val handAngleRad = Math.toRadians((handValue * stepDeg - 90f).toDouble())
            val knobRadius = radius * 0.78f
            val knobCenter = Offset(
                center.x + knobRadius * cos(handAngleRad).toFloat(),
                center.y + knobRadius * sin(handAngleRad).toFloat(),
            )
            drawLine(color = colors.accentPrimary, start = center, end = knobCenter, strokeWidth = with(density) { 2.dp.toPx() })
            drawCircle(color = colors.accentPrimary, radius = with(density) { 4.dp.toPx() }, center = center)
            drawCircle(color = colors.accentPrimary, radius = with(density) { 20.dp.toPx() }, center = knobCenter)

            // The knob sits exactly on top of whichever label it's pointing at — redraw that
            // value in white on top of it, the same way a native clock picker does, so the
            // number is never actually lost under the hand.
            val knobText = "%d".format(if (editingHour) handValue.let { if (it == 0) 12 else it } else minute)
            val knobLayout = textMeasurer.measure(knobText, knobLabelStyle)
            drawText(textLayoutResult = knobLayout, topLeft = Offset(knobCenter.x - knobLayout.size.width / 2f, knobCenter.y - knobLayout.size.height / 2f))
        }
    }
}

private val dayFullNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

/** A single-letter repeat-day chip — flat fill (no border; the parent card already carries one),
 *  with an accessibility label distinct from the shared "T"/"S" glyphs. */
@Composable
fun DayChip(index: Int, label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = arcadeColors
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (selected) colors.accentPrimary else colors.bgSurfaceRaised)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .clearAndSetSemantics { contentDescription = dayFullNames[index] },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = arcadeType.ticker, color = if (selected) Color.White else colors.textSecondary)
    }
}
