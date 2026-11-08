package com.gpsalarm.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gpsalarm.app.settings.SettingsStore
import java.util.Calendar

private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

/** Pass [alarmId] to edit an existing alarm; null creates a new one. */
@Composable
fun AddEditTimeAlarmScreen(
    alarmId: Long?,
    onDone: () -> Unit,
    viewModel: AddEditTimeAlarmViewModel = addEditTimeAlarmViewModel(),
) {
    // The pickers take their starting values once, so the form isn't built until the alarm has loaded.
    var loading by remember { mutableStateOf(alarmId != null) }
    var existing by remember { mutableStateOf<TimeAlarmEdit?>(null) }
    LaunchedEffect(alarmId) {
        if (alarmId == null) return@LaunchedEffect
        existing = viewModel.load(alarmId)
        loading = false
        if (existing == null) onDone() // alarm was deleted out from under us
    }

    if (loading) {
        LoadingScreen(onBack = onDone)
    } else {
        TimeAlarmForm(existing = existing, onDone = onDone, viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeAlarmForm(existing: TimeAlarmEdit?, onDone: () -> Unit, viewModel: AddEditTimeAlarmViewModel) {
    val context = LocalContext.current
    val now = remember { Calendar.getInstance() }
    val timePickerState = rememberTimePickerState(
        initialHour = existing?.details?.hour ?: now.get(Calendar.HOUR_OF_DAY),
        initialMinute = existing?.details?.minute ?: now.get(Calendar.MINUTE),
        is24Hour = false,
    )

    var label by remember { mutableStateOf(existing?.alarm?.label.orEmpty()) }
    var repeatDays by remember { mutableStateOf(existing?.details?.repeatDays ?: 0) }
    var vibrate by remember { mutableStateOf(existing?.alarm?.vibrate ?: true) }
    var soundUri by remember { mutableStateOf(existing?.alarm?.soundUri?.let(Uri::parse)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New time alarm" else "Edit time alarm") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimePicker(state = timePickerState)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Repeat")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                dayLabels.forEachIndexed { index, dayLabel ->
                    val selected = (repeatDays shr index) and 1 == 1
                    FilterChip(
                        selected = selected,
                        onClick = { repeatDays = repeatDays xor (1 shl index) },
                        label = { Text(dayLabel) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SoundAndVibrateSection(
                soundUri = soundUri,
                onSoundUriChange = { soundUri = it },
                vibrate = vibrate,
                onVibrateChange = { vibrate = it },
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.save(
                        existing = existing?.alarm,
                        label = label,
                        hour = timePickerState.hour,
                        minute = timePickerState.minute,
                        repeatDays = repeatDays,
                        soundUri = soundUri?.toString(),
                        vibrate = vibrate,
                        snoozeDurationMin = SettingsStore.defaultSnoozeMinutes(context),
                        onSaved = onDone,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (existing == null) "Save" else "Save changes")
            }
        }
    }
}
