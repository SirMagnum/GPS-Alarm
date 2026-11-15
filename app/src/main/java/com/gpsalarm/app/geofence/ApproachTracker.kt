package com.gpsalarm.app.geofence

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** One location alarm currently being watched with active GPS polling (see [ApproachTrackingService]). */
data class ApproachTarget(
    val alarmId: Long,
    val label: String,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Float,
    val startedAtMillis: Long = System.currentTimeMillis(),
)

/**
 * In-process registry of alarms being actively watched. Android's geofencing API checks location
 * only occasionally to save battery, which can delay noticing a small circle by many minutes.
 * [GeofenceBroadcastReceiver] adds a target here the moment a much bigger, cheap "outer" geofence
 * is crossed, and [ApproachTrackingService] polls GPS frequently for everything in this map until
 * each target's real (small) circle is reached, then removes it.
 */
object ApproachTracker {
    private val _targets = MutableStateFlow<Map<Long, ApproachTarget>>(emptyMap())
    val targets: StateFlow<Map<Long, ApproachTarget>> = _targets

    fun add(target: ApproachTarget) {
        _targets.update { it + (target.alarmId to target) }
    }

    fun remove(alarmId: Long) {
        _targets.update { it - alarmId }
    }
}
