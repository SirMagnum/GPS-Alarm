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
import com.gpsalarm.app.diagnostics.DiagnosticsLog

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent)
        if (event == null) {
            DiagnosticsLog.record(context, "Geofence broadcast received but had no event data")
            return
        }
        if (event.hasError()) {
            val error = GeofenceStatusCodes.getStatusCodeString(event.errorCode)
            Log.w(TAG, "Geofencing error: $error")
            DiagnosticsLog.record(context, "Geofence ERROR: $error")
            return
        }
        val transition = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            else -> {
                DiagnosticsLog.record(context, "Geofence ignored transition type ${event.geofenceTransition}")
                return
            }
        }

        val requestId = event.triggeringGeofences?.firstOrNull()?.requestId ?: return
        val isOuter = requestId.startsWith(OUTER_PREFIX)
        val alarmId = (if (isOuter) requestId.removePrefix(OUTER_PREFIX) else requestId).toLongOrNull() ?: return

        // How old the position behind this event is: a big gap means Google detected it late.
        val fix = event.triggeringLocation
        val fixInfo = if (fix != null) {
            val ageSec = (System.currentTimeMillis() - fix.time) / 1000
            " | position fix %.5f, %.5f, accuracy %d m, %d s old".format(
                java.util.Locale.US, fix.latitude, fix.longitude, fix.accuracy.toInt(), ageSec,
            )
        } else {
            " | no position fix attached"
        }
        val ring = if (isOuter) "outer ring" else "destination"
        DiagnosticsLog.record(context, "Geofence EVENT $transition ($ring) for #$alarmId$fixInfo")

        if (isOuter) {
            if (transition == "ENTER") {
                val target = ApproachTarget(
                    alarmId = alarmId,
                    label = intent.getStringExtra(GeofenceManager.EXTRA_LABEL).orEmpty(),
                    lat = intent.getDoubleExtra(GeofenceManager.EXTRA_LAT, fix?.latitude ?: 0.0),
                    lng = intent.getDoubleExtra(GeofenceManager.EXTRA_LNG, fix?.longitude ?: 0.0),
                    radiusMeters = intent.getFloatExtra(GeofenceManager.EXTRA_RADIUS, 0f),
                )
                ApproachTrackingService.watch(context, target)
            } else {
                // Wandered back out of the outer ring without reaching the destination: stop
                // the expensive GPS polling rather than keep it running indefinitely.
                ApproachTrackingService.unwatch(context, alarmId)
            }
            return
        }

        // The real destination was reached — either Play Services' own (lazy) check caught it, or
        // active approach tracking already did and this arrives moments later; either way, ring,
        // and make sure no battery is spent still watching for an arrival that already happened.
        ApproachTrackingService.unwatch(context, alarmId)

        val serviceIntent = Intent(context, AlarmRingingService::class.java)
            .setAction(AlarmRingingService.ACTION_START)
            .putExtra(TimeAlarmScheduler.EXTRA_ALARM_ID, alarmId)
        runCatching { ContextCompat.startForegroundService(context, serviceIntent) }
            .onFailure { DiagnosticsLog.record(context, "Could NOT start ringing for #$alarmId: ${it.message}") }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
        private const val OUTER_PREFIX = "outer_"
    }
}
