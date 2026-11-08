package com.gpsalarm.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gpsalarm.app.data.Alarm
import com.gpsalarm.app.data.AlarmRepository
import com.gpsalarm.app.data.AlarmType
import com.gpsalarm.app.data.AppDatabase
import com.gpsalarm.app.data.GeofenceTrigger
import com.gpsalarm.app.data.LocationAlarmDetails
import com.gpsalarm.app.geofence.GeofenceManager
import kotlinx.coroutines.launch

class LocationAlarmEdit(val alarm: Alarm, val details: LocationAlarmDetails)

class AddEditLocationAlarmViewModel(
    private val repository: AlarmRepository,
    private val geofenceManager: GeofenceManager,
) : ViewModel() {

    suspend fun load(alarmId: Long): LocationAlarmEdit? {
        val alarm = repository.getAlarm(alarmId) ?: return null
        val details = repository.getLocationDetails(alarmId) ?: return null
        return LocationAlarmEdit(alarm, details)
    }

    /** Creates a new alarm, or updates [existing] in place. Saving always (re-)enables the alarm. */
    fun save(
        existing: Alarm?,
        label: String,
        lat: Double,
        lng: Double,
        address: String?,
        radiusMeters: Float,
        triggerOn: GeofenceTrigger,
        soundUri: String?,
        vibrate: Boolean,
        snoozeDurationMin: Int,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            val alarm = existing?.copy(
                label = label.ifBlank { "Location alarm" },
                isEnabled = true,
                soundUri = soundUri,
                vibrate = vibrate,
            ) ?: Alarm(
                label = label.ifBlank { "Location alarm" },
                type = AlarmType.LOCATION,
                soundUri = soundUri,
                vibrate = vibrate,
                snoozeDurationMin = snoozeDurationMin,
            )
            val details = LocationAlarmDetails(
                alarmId = alarm.id,
                destinationLat = lat,
                destinationLng = lng,
                destinationAddress = address,
                radiusMeters = radiusMeters,
                triggerOn = triggerOn,
                oneShot = true,
            )
            val id = if (existing != null) {
                repository.updateLocationAlarm(alarm, details)
                // Drop the old region first so a moved or resized destination can't leave a stale one behind.
                geofenceManager.removeGeofence(existing.id)
                existing.id
            } else {
                repository.createLocationAlarm(alarm, details)
            }
            geofenceManager.addGeofence(id, lat, lng, radiusMeters, triggerOn)
            onSaved()
        }
    }
}

@Composable
fun addEditLocationAlarmViewModel(): AddEditLocationAlarmViewModel {
    val context = LocalContext.current
    return viewModel(
        factory = viewModelFactory {
            initializer {
                AddEditLocationAlarmViewModel(
                    AlarmRepository(AppDatabase.getInstance(context).alarmDao()),
                    GeofenceManager(context),
                )
            }
        },
    )
}
