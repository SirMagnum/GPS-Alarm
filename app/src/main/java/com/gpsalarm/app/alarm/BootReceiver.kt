package com.gpsalarm.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gpsalarm.app.data.AlarmRepository
import com.gpsalarm.app.data.AlarmType
import com.gpsalarm.app.data.AppDatabase
import com.gpsalarm.app.geofence.GeofenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Android cancels all AlarmManager alarms and geofences on reboot, so every enabled alarm must
 * be re-registered from Room.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = AlarmRepository(AppDatabase.getInstance(appContext).alarmDao())
                val scheduler = TimeAlarmScheduler(appContext)
                val geofenceManager = GeofenceManager(appContext)
                repository.getEnabledAlarms().forEach { alarm ->
                    when (alarm.type) {
                        AlarmType.TIME -> repository.getTimeDetails(alarm.id)?.let { details ->
                            scheduler.schedule(alarm.id, details.hour, details.minute, details.repeatDays)
                        }
                        AlarmType.LOCATION -> repository.getLocationDetails(alarm.id)?.let { details ->
                            geofenceManager.addGeofence(
                                alarm.id,
                                details.destinationLat,
                                details.destinationLng,
                                details.radiusMeters,
                                details.triggerOn,
                            )
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
