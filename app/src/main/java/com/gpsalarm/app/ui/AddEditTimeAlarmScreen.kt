package com.gpsalarm.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gpsalarm.app.settings.SettingsStore
import com.gpsalarm.app.ui.theme.ArcadeRadius
import com.gpsalarm.app.ui.theme.TactileButton
import com.gpsalarm.app.ui.theme.arcadeColors
import com.gpsalarm.app.ui.theme.arcadeType
import java.util.Calendar

private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * Content for the floating "new alarm" tray (see AlarmListScreen's FloatingTray) — a title row
 * with a close "X", then the same fields used when editing an existing alarm from the list.
 */
@Composable
fun NewTimeAlarmTray(onClose: () -> Unit, viewModel: AddEditTimeAlarmViewModel = addEditTimeAlarmViewModel()) {
    val colors = arcadeColors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("NEW TIME ALARM", style = arcadeType.button, color = colors.textPrimary)
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = colors.textPrimary)
            }
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimeAlarmFields(existing = null, onDone = onClose, viewModel = viewModel)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * The editor shown inline, inside an alarm's own list card, when that card is expanded —
 * no navigation, no separate screen. The card grows around this content.
 */
@Composable
fun InlineTimeAlarmEditor(
    alarmId: Long,
    onDone: () -> Unit,
    viewModel: AddEditTimeAlarmViewModel = addEditTimeAlarmViewModel(),
) {
    var existing by remember { mutableStateOf<TimeAlarmEdit?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(alarmId) {
        existing = viewModel.load(alarmId)
        loading = false
        if (existing == null) onDone() // alarm was deleted out from under us
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = arcadeColors.accentPrimary)
        }
    } else {
        TimeAlarmFields(existing = existing, onDone = onDone, viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeAlarmFields(
    existing: TimeAlarmEdit?,
    onDone: () -> Unit,
    viewModel: AddEditTimeAlarmViewModel,
    modifier: Modifier = Modifier,
) {
    val colors = arcadeColors
    val context = androidx.compose.ui.platform.LocalContext.current
    val now = remember { Calendar.getInstance() }
    var hour24 by remember { mutableIntStateOf(existing?.details?.hour ?: now.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(existing?.details?.minute ?: now.get(Calendar.MINUTE)) }

    var label by remember { mutableStateOf(existing?.alarm?.label.orEmpty()) }
    var repeatDays by remember { mutableStateOf(existing?.details?.repeatDays ?: 0) }
    var vibrate by remember { mutableStateOf(existing?.alarm?.vibrate ?: true) }
    var soundUri by remember { mutableStateOf(existing?.alarm?.soundUri?.let(Uri::parse)) }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        TimeDial(
            hour24 = hour24,
            minute = minute,
            onHour24Change = { hour24 = it },
            onMinuteChange = { minute = it },
        )

        Spacer(modifier = Modifier.height(20.dp))

        TextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Label", style = arcadeType.body) },
            shape = RoundedCornerShape(ArcadeRadius.sm),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedContainerColor = colors.bgSurfaceRaised,
                unfocusedContainerColor = colors.bgSurfaceRaised,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                cursorColor = colors.accentPrimary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            dayLabels.forEachIndexed { index, dayLabel ->
                val selected = (repeatDays shr index) and 1 == 1
                DayChip(index = index, label = dayLabel, selected = selected) { repeatDays = repeatDays xor (1 shl index) }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SoundAndVibrateSection(
            soundUri = soundUri,
            onSoundUriChange = { soundUri = it },
            vibrate = vibrate,
            onVibrateChange = { vibrate = it },
        )

        Spacer(modifier = Modifier.height(24.dp))

        TactileButton(
            text = if (existing == null) "Save" else "Save changes",
            onClick = {
                viewModel.save(
                    existing = existing?.alarm,
                    label = label,
                    hour = hour24,
                    minute = minute,
                    repeatDays = repeatDays,
                    soundUri = soundUri?.toString(),
                    vibrate = vibrate,
                    snoozeDurationMin = SettingsStore.defaultSnoozeMinutes(context),
                    onSaved = onDone,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
