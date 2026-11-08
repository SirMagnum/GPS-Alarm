package com.gpsalarm.app.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.gpsalarm.app.alarm.AlarmRingingService
import com.gpsalarm.app.alarm.TimeAlarmScheduler

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.w(TAG, "Geofencing error: ${GeofenceStatusCodes.getStatusCodeString(event.errorCode)}")
            return
        }
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER &&
            event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            return
        }

        val alarmId = event.triggeringGeofences?.firstOrNull()?.requestId?.toLongOrNull() ?: return
        val serviceIntent = Intent(context, AlarmRingingService::class.java)
            .setAction(AlarmRingingService.ACTION_START)
            .putExtra(TimeAlarmScheduler.EXTRA_ALARM_ID, alarmId)
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
