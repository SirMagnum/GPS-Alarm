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
import com.gpsalarm.app.diagnostics.DiagnosticsLog

/**
 * Registers two geofences per location alarm, because Android's geofencing API only checks
 * location occasionally (to save battery) and can take many minutes to notice a small circle:
 *  - an "outer" circle, a few km wider, which is cheap to leave lazily checked and gives Android
 *    plenty of warning before the real destination;
 *  - the real, small circle the alarm actually cares about.
 * Crossing the outer circle starts [ApproachTrackingService], which polls GPS frequently for the
 * final stretch so the alarm fires within seconds of actually arriving, not minutes after.
 */
class GeofenceManager(private val context: Context) {

    private val geofencingClient = LocationServices.getGeofencingClient(context)

    fun addGeofence(
        alarmId: Long,
        lat: Double,
        lng: Double,
        radiusMeters: Float,
        triggerOn: GeofenceTrigger,
        label: String = "",
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Skipping geofence registration for alarm $alarmId: location permission not granted")
            DiagnosticsLog.record(context, "Geofence #$alarmId NOT registered: location permission missing")
            return
        }

        val innerTransition = if (triggerOn == GeofenceTrigger.ENTER) {
            Geofence.GEOFENCE_TRANSITION_ENTER
        } else {
            Geofence.GEOFENCE_TRANSITION_EXIT
        }
        val inner = Geofence.Builder()
            .setRequestId(alarmId.toString())
            .setCircularRegion(lat, lng, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(innerTransition)
            .build()

        // The outer ring only ever drives approach tracking, which only makes sense for arrival
        // alarms (an "on departure" alarm is already right at its one and only boundary).
        val geofences = mutableListOf(inner)
        if (triggerOn == GeofenceTrigger.ENTER) {
            geofences += Geofence.Builder()
                .setRequestId(outerRequestId(alarmId))
                .setCircularRegion(lat, lng, radiusMeters + OUTER_BUFFER_METERS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        val description = "#$alarmId at %.5f, %.5f radius %d m".format(
            java.util.Locale.US, lat, lng, radiusMeters.toInt(),
        )
        runCatching {
            geofencingClient.addGeofences(request, pendingIntentFor(alarmId, lat, lng, radiusMeters, label))
                .addOnSuccessListener { DiagnosticsLog.record(context, "Geofence registered: $description") }
                .addOnFailureListener { e ->
                    DiagnosticsLog.record(context, "Geofence FAILED to register ($description): ${e.message}")
                }
        }.onFailure {
            Log.w(TAG, "Failed to add geofence for alarm $alarmId", it)
            DiagnosticsLog.record(context, "Geofence FAILED to register ($description): ${it.message}")
        }
    }

    fun removeGeofence(alarmId: Long) {
        geofencingClient.removeGeofences(listOf(alarmId.toString(), outerRequestId(alarmId)))
        ApproachTrackingService.unwatch(context, alarmId)
        DiagnosticsLog.record(context, "Geofence removed: #$alarmId")
    }

    private fun outerRequestId(alarmId: Long) = "outer_$alarmId"

    private fun pendingIntentFor(alarmId: Long, lat: Double, lng: Double, radiusMeters: Float, label: String): PendingIntent {
        // Must be mutable: Play Services fills in the GeofencingEvent extras when it fires this
        // broadcast. FLAG_IMMUTABLE here causes AddGeofences to fail with DEVELOPER_ERROR (10).
        // The destination's own details ride along so the receiver can hand them straight to
        // ApproachTrackingService on an outer-ring crossing without a database read.
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
            .putExtra(EXTRA_ALARM_ID, alarmId)
            .putExtra(EXTRA_LAT, lat)
            .putExtra(EXTRA_LNG, lng)
            .putExtra(EXTRA_RADIUS, radiusMeters)
            .putExtra(EXTRA_LABEL, label)
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_location_alarm_id"
        const val EXTRA_LAT = "extra_destination_lat"
        const val EXTRA_LNG = "extra_destination_lng"
        const val EXTRA_RADIUS = "extra_destination_radius"
        const val EXTRA_LABEL = "extra_destination_label"

        /** Widens the outer ring beyond the real radius: the real fix for geofencing's slow checks. */
        const val OUTER_BUFFER_METERS = 2000f

        private const val TAG = "GeofenceManager"
    }
}
