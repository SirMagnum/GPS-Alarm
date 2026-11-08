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
import com.gpsalarm.app.data.AlarmWithDetails
import com.gpsalarm.app.data.AppDatabase
import com.gpsalarm.app.geofence.GeofenceManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmListViewModel(
    private val repository: AlarmRepository,
    private val scheduler: TimeAlarmScheduler,
    private val geofenceManager: GeofenceManager,
) : ViewModel() {

    val alarms: StateFlow<List<AlarmWithDetails>> = repository.observeAlarmsWithDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setEnabled(alarm: Alarm, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(alarm.id, isEnabled)
            when (alarm.type) {
                AlarmType.TIME -> {
                    if (isEnabled) {
                        repository.getTimeDetails(alarm.id)?.let { details ->
                            scheduler.schedule(alarm.id, details.hour, details.minute, details.repeatDays)
                        }
                    } else {
                        scheduler.cancel(alarm.id)
                    }
                }
                AlarmType.LOCATION -> {
                    if (isEnabled) {
                        repository.getLocationDetails(alarm.id)?.let { details ->
                            geofenceManager.addGeofence(
                                alarm.id,
                                details.destinationLat,
                                details.destinationLng,
                                details.radiusMeters,
                                details.triggerOn,
                            )
                        }
                    } else {
                        geofenceManager.removeGeofence(alarm.id)
                    }
                }
            }
        }
    }

    fun delete(alarm: Alarm) {
        viewModelScope.launch {
            when (alarm.type) {
                AlarmType.TIME -> scheduler.cancel(alarm.id)
                AlarmType.LOCATION -> geofenceManager.removeGeofence(alarm.id)
            }
            repository.delete(alarm)
        }
    }
}

@Composable
fun alarmListViewModel(): AlarmListViewModel {
    val context = LocalContext.current
    return viewModel(
        factory = viewModelFactory {
            initializer {
                AlarmListViewModel(
                    AlarmRepository(AppDatabase.getInstance(context).alarmDao()),
                    TimeAlarmScheduler(context),
                    GeofenceManager(context),
                )
            }
        },
    )
}
