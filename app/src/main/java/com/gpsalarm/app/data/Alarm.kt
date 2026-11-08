package com.gpsalarm.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

enum class AlarmType { TIME, LOCATION }

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val isEnabled: Boolean = true,
    val type: AlarmType,
    val soundUri: String? = null,
    val vibrate: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeDurationMin: Int = 10,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "time_alarm_details",
    foreignKeys = [
        ForeignKey(entity = Alarm::class, parentColumns = ["id"], childColumns = ["alarmId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class TimeAlarmDetails(
    @PrimaryKey
    val alarmId: Long,
    val hour: Int,
    val minute: Int,
    /** Bitmask, bit 0 = Monday .. bit 6 = Sunday. Zero means one-time (non-repeating). */
    val repeatDays: Int = 0,
    val isOneTime: Boolean = true,
)

@Entity(
    tableName = "location_alarm_details",
    foreignKeys = [
        ForeignKey(entity = Alarm::class, parentColumns = ["id"], childColumns = ["alarmId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class LocationAlarmDetails(
    @PrimaryKey
    val alarmId: Long,
    val destinationLat: Double,
    val destinationLng: Double,
    val destinationAddress: String? = null,
    val radiusMeters: Float,
    val triggerOn: GeofenceTrigger = GeofenceTrigger.ENTER,
    val oneShot: Boolean = true,
)

enum class GeofenceTrigger { ENTER, EXIT }
