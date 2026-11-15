package com.gpsalarm.app.geofence

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.gpsalarm.app.R
import com.gpsalarm.app.alarm.AlarmNotifications
import com.gpsalarm.app.alarm.AlarmRingingService
import com.gpsalarm.app.alarm.TimeAlarmScheduler
import com.gpsalarm.app.diagnostics.DiagnosticsLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Polls GPS frequently (battery-expensive, so only while [ApproachTracker] is non-empty) to catch
 * the moment a target's real circle is reached. The lazy outer geofence that starts this service
 * is what makes the overall approach cheap: this only ever runs for the last stretch of a trip.
 */
class ApproachTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watchJob: Job? = null
    private var updatesActive = false
    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            checkArrivals(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        AlarmNotifications.ensureTrackingChannel(this)
        // One subscription for the service's whole lifetime: starts/stops GPS polling and the
        // service itself to match what's left to watch, however that set changes over time.
        watchJob = scope.launch {
            ApproachTracker.targets.collect { targets ->
                evictTimedOut()
                if (targets.isEmpty()) {
                    stopLocationUpdates()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    startLocationUpdatesIfNeeded()
                    startForeground(NOTIFICATION_ID, buildNotification())
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_WATCH -> {
                val target = ApproachTarget(
                    alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L),
                    label = intent.getStringExtra(EXTRA_LABEL).orEmpty(),
                    lat = intent.getDoubleExtra(EXTRA_LAT, 0.0),
                    lng = intent.getDoubleExtra(EXTRA_LNG, 0.0),
                    radiusMeters = intent.getFloatExtra(EXTRA_RADIUS, 0f),
                )
                if (target.alarmId != -1L) {
                    DiagnosticsLog.record(this, "Approach tracking started for #${target.alarmId} (\"${target.label}\")")
                    ApproachTracker.add(target)
                }
            }
            ACTION_UNWATCH -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                if (alarmId != -1L && ApproachTracker.targets.value.containsKey(alarmId)) {
                    DiagnosticsLog.record(this, "Approach tracking stopped for #$alarmId")
                }
                if (alarmId != -1L) ApproachTracker.remove(alarmId)
            }
        }
        // Must happen within a few seconds of startForegroundService regardless of branch; the
        // collector started in onCreate takes it from here (including tearing back down again).
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_NOT_STICKY
    }

    private fun evictTimedOut() {
        val now = System.currentTimeMillis()
        ApproachTracker.targets.value.values
            .filter { now - it.startedAtMillis > MAX_WATCH_DURATION_MILLIS }
            .forEach {
                DiagnosticsLog.record(this, "Approach tracking timed out for #${it.alarmId} (\"${it.label}\")")
                ApproachTracker.remove(it.alarmId)
            }
    }

    private fun startLocationUpdatesIfNeeded() {
        if (updatesActive) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, POLL_INTERVAL_MILLIS)
            .setMinUpdateIntervalMillis(POLL_INTERVAL_MILLIS)
            .build()
        runCatching {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            updatesActive = true
        }.onFailure {
            DiagnosticsLog.record(this, "Approach tracking could not start GPS updates: ${it.message}")
        }
    }

    private fun stopLocationUpdates() {
        if (!updatesActive) return
        fusedClient.removeLocationUpdates(locationCallback)
        updatesActive = false
    }

    private fun checkArrivals(location: Location) {
        ApproachTracker.targets.value.values.forEach { target ->
            val distance = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, target.lat, target.lng, distance)
            DiagnosticsLog.record(
                this,
                "Approach tracking poll for #${target.alarmId}: %.0f m away, accuracy %d m, mock=%b"
                    .format(java.util.Locale.US, distance[0], location.accuracy.toInt(), location.isFromMockProvider),
            )
            if (distance[0] <= target.radiusMeters) {
                DiagnosticsLog.record(
                    this,
                    "Approach tracking ENTER for #${target.alarmId}: %.0f m from centre, accuracy %d m"
                        .format(java.util.Locale.US, distance[0], location.accuracy.toInt()),
                )
                ApproachTracker.remove(target.alarmId)
                val serviceIntent = Intent(this, AlarmRingingService::class.java)
                    .setAction(AlarmRingingService.ACTION_START)
                    .putExtra(TimeAlarmScheduler.EXTRA_ALARM_ID, target.alarmId)
                runCatching { ContextCompat.startForegroundService(this, serviceIntent) }
                    .onFailure { DiagnosticsLog.record(this, "Could NOT start ringing for #${target.alarmId}: ${it.message}") }
            }
        }
    }

    private fun buildNotification(): android.app.Notification {
        val count = ApproachTracker.targets.value.size
        val text = if (count <= 1) {
            ApproachTracker.targets.value.values.firstOrNull()?.label?.let { "Watching for arrival at \"$it\"" }
                ?: "Watching for arrival"
        } else {
            "Watching for arrival at $count destinations"
        }
        return NotificationCompat.Builder(this, AlarmNotifications.TRACKING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle("GPS Alarm")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        stopLocationUpdates()
        watchJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_WATCH = "com.gpsalarm.app.action.APPROACH_WATCH"
        const val ACTION_UNWATCH = "com.gpsalarm.app.action.APPROACH_UNWATCH"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
        const val EXTRA_RADIUS = "extra_radius"

        private const val NOTIFICATION_ID = 43
        private const val POLL_INTERVAL_MILLIS = 15_000L
        private const val MAX_WATCH_DURATION_MILLIS = 3 * 60 * 60 * 1000L // safety valve: 3 hours

        fun watch(context: Context, target: ApproachTarget) {
            val intent = Intent(context, ApproachTrackingService::class.java)
                .setAction(ACTION_WATCH)
                .putExtra(EXTRA_ALARM_ID, target.alarmId)
                .putExtra(EXTRA_LABEL, target.label)
                .putExtra(EXTRA_LAT, target.lat)
                .putExtra(EXTRA_LNG, target.lng)
                .putExtra(EXTRA_RADIUS, target.radiusMeters)
            ContextCompat.startForegroundService(context, intent)
        }

        fun unwatch(context: Context, alarmId: Long) {
            val intent = Intent(context, ApproachTrackingService::class.java)
                .setAction(ACTION_UNWATCH)
                .putExtra(EXTRA_ALARM_ID, alarmId)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
