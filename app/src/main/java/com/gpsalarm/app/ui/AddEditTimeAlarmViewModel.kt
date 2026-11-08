package com.gpsalarm.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gpsalarm.app.alarm.TimeAlarmScheduler
import com.gpsalarm.app.data.Alarm
import com.gpsalarm.app.data.AlarmRepository
import com.gpsalarm.app.data.AlarmType
import com.gpsalarm.app.data.AppDatabase
import com.gpsalarm.app.data.TimeAlarmDetails
import kotlinx.coroutines.launch

class TimeAlarmEdit(val alarm: Alarm, val details: TimeAlarmDetails)

class AddEditTimeAlarmViewModel(
    private val repository: AlarmRepository,
    private val scheduler: TimeAlarmScheduler,
) : ViewModel() {

    suspend fun load(alarmId: Long): TimeAlarmEdit? {
        val alarm = repository.getAlarm(alarmId) ?: return null
        val details = repository.getTimeDetails(alarmId) ?: return null
        return TimeAlarmEdit(alarm, details)
    }

    /** Creates a new alarm, or updates [existing] in place. Saving always (re-)enables the alarm. */
    fun save(
        existing: Alarm?,
        label: String,
        hour: Int,
        minute: Int,
        repeatDays: Int,
        soundUri: String?,
        vibrate: Boolean,
        snoozeDurationMin: Int,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            val alarm = existing?.copy(
                label = label.ifBlank { "Alarm" },
                isEnabled = true,
                soundUri = soundUri,
                vibrate = vibrate,
            ) ?: Alarm(
                label = label.ifBlank { "Alarm" },
                type = AlarmType.TIME,
                soundUri = soundUri,
                vibrate = vibrate,
                snoozeDurationMin = snoozeDurationMin,
            )
            val details = TimeAlarmDetails(
                alarmId = alarm.id,
                hour = hour,
                minute = minute,
                repeatDays = repeatDays,
                isOneTime = repeatDays == 0,
            )
            val id = if (existing != null) {
                repository.updateTimeAlarm(alarm, details)
                existing.id
            } else {
                repository.createTimeAlarm(alarm, details)
            }
            // Same PendingIntent as before, so this replaces any alarm already scheduled for [id].
            scheduler.schedule(id, hour, minute, repeatDays)
            onSaved()
        }
    }
}

@Composable
fun addEditTimeAlarmViewModel(): AddEditTimeAlarmViewModel {
    val context = LocalContext.current
    return viewModel(
        factory = viewModelFactory {
            initializer {
                AddEditTimeAlarmViewModel(
                    AlarmRepository(AppDatabase.getInstance(context).alarmDao()),
                    TimeAlarmScheduler(context),
                )
            }
        },
    )
}
