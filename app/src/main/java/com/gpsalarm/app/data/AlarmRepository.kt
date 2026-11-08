package com.gpsalarm.app.data

import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val dao: AlarmDao) {

    fun observeAlarms(): Flow<List<Alarm>> = dao.observeAlarms()

    fun observeAlarmsWithDetails(): Flow<List<AlarmWithDetails>> = dao.observeAlarmsWithDetails()

    suspend fun getEnabledAlarms(): List<Alarm> = dao.getEnabledAlarms()

    suspend fun getTimeDetails(alarmId: Long): TimeAlarmDetails? = dao.getTimeDetails(alarmId)

    suspend fun getLocationDetails(alarmId: Long): LocationAlarmDetails? = dao.getLocationDetails(alarmId)

    suspend fun getAlarm(alarmId: Long): Alarm? = dao.getAlarm(alarmId)

    suspend fun createTimeAlarm(alarm: Alarm, details: TimeAlarmDetails): Long {
        val id = dao.insertAlarm(alarm)
        dao.insertTimeDetails(details.copy(alarmId = id))
        return id
    }

    suspend fun createLocationAlarm(alarm: Alarm, details: LocationAlarmDetails): Long {
        val id = dao.insertAlarm(alarm)
        dao.insertLocationDetails(details.copy(alarmId = id))
        return id
    }

    // Update in place: re-inserting the parent row (REPLACE) would cascade-delete its details row.
    suspend fun updateTimeAlarm(alarm: Alarm, details: TimeAlarmDetails) {
        dao.updateAlarm(alarm)
        dao.insertTimeDetails(details)
    }

    suspend fun updateLocationAlarm(alarm: Alarm, details: LocationAlarmDetails) {
        dao.updateAlarm(alarm)
        dao.insertLocationDetails(details)
    }

    suspend fun setEnabled(alarmId: Long, isEnabled: Boolean) = dao.setEnabled(alarmId, isEnabled)

    suspend fun delete(alarm: Alarm) = dao.deleteAlarm(alarm)
}
