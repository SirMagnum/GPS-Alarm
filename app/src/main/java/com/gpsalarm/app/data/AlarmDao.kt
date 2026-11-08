package com.gpsalarm.app.data

import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class AlarmWithDetails(
    @Embedded val alarm: Alarm,
    @Relation(parentColumn = "id", entityColumn = "alarmId")
    val timeDetails: TimeAlarmDetails?,
    @Relation(parentColumn = "id", entityColumn = "alarmId")
    val locationDetails: LocationAlarmDetails?,
)

@androidx.room.Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms ORDER BY created_at DESC")
    fun observeAlarms(): Flow<List<Alarm>>

    @Transaction
    @Query("SELECT * FROM alarms ORDER BY created_at DESC")
    fun observeAlarmsWithDetails(): Flow<List<AlarmWithDetails>>

    @Query("SELECT * FROM alarms WHERE id = :alarmId")
    suspend fun getAlarm(alarmId: Long): Alarm?

    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    suspend fun getEnabledAlarms(): List<Alarm>

    @Query("SELECT * FROM time_alarm_details WHERE alarmId = :alarmId")
    suspend fun getTimeDetails(alarmId: Long): TimeAlarmDetails?

    @Query("SELECT * FROM location_alarm_details WHERE alarmId = :alarmId")
    suspend fun getLocationDetails(alarmId: Long): LocationAlarmDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeDetails(details: TimeAlarmDetails)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationDetails(details: LocationAlarmDetails)

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun deleteAlarm(alarm: Alarm)

    @Query("UPDATE alarms SET isEnabled = :isEnabled WHERE id = :alarmId")
    suspend fun setEnabled(alarmId: Long, isEnabled: Boolean)
}
