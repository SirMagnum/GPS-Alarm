package com.gpsalarm.app.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.gpsalarm.app.data.GeofenceTrigger

class GeofenceManager(private val context: Context) {

    private val geofencingClient = LocationServices.getGeofencingClient(context)

    fun addGeofence(alarmId: Long, lat: Double, lng: Double, radiusMeters: Float, triggerOn: GeofenceTrigger) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Skipping geofence registration for alarm $alarmId: location permission not granted")
            return
        }

        val transitionType = if (triggerOn == GeofenceTrigger.ENTER) {
            Geofence.GEOFENCE_TRANSITION_ENTER
        } else {
            Geofence.GEOFENCE_TRANSITION_EXIT
        }
        val geofence = Geofence.Builder()
            .setRequestId(alarmId.toString())
            .setCircularRegion(lat, lng, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transitionType)
            .build()
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(if (triggerOn == GeofenceTrigger.ENTER) {
                GeofencingRequest.INITIAL_TRIGGER_ENTER
            } else {
                GeofencingRequest.INITIAL_TRIGGER_EXIT
            })
            .addGeofence(geofence)
            .build()

        runCatching {
            geofencingClient.addGeofences(request, pendingIntentFor(alarmId))
        }.onFailure { Log.w(TAG, "Failed to add geofence for alarm $alarmId", it) }
    }

    fun removeGeofence(alarmId: Long) {
        geofencingClient.removeGeofences(listOf(alarmId.toString()))
    }

    private fun pendingIntentFor(alarmId: Long): PendingIntent {
        // Must be mutable: Play Services fills in the GeofencingEvent extras when it fires this
        // broadcast. FLAG_IMMUTABLE here causes AddGeofences to fail with DEVELOPER_ERROR (10).
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).putExtra(EXTRA_ALARM_ID, alarmId)
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_location_alarm_id"
        private const val TAG = "GeofenceManager"
    }
}
