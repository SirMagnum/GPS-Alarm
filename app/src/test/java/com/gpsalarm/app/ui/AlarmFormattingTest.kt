package com.gpsalarm.app.ui

import com.gpsalarm.app.data.GeofenceTrigger
import com.gpsalarm.app.data.LocationAlarmDetails
import com.gpsalarm.app.data.TimeAlarmDetails
import kotlin.test.Test
import kotlin.test.assertEquals

class AlarmFormattingTest {

    @Test
    fun `one-time alarm`() {
        val details = TimeAlarmDetails(alarmId = 1, hour = 7, minute = 0, repeatDays = 0, isOneTime = true)
        assertEquals("7:00 AM · One-time", formatTimeSubtitle(details))
    }

    @Test
    fun `weekday repeat`() {
        val details = TimeAlarmDetails(alarmId = 1, hour = 18, minute = 30, repeatDays = 0b0011111, isOneTime = false)
        assertEquals("6:30 PM · Weekdays", formatTimeSubtitle(details))
    }

    @Test
    fun `weekend repeat`() {
        val details = TimeAlarmDetails(alarmId = 1, hour = 9, minute = 5, repeatDays = 0b1100000, isOneTime = false)
        assertEquals("9:05 AM · Weekends", formatTimeSubtitle(details))
    }

    @Test
    fun `every day repeat`() {
        val details = TimeAlarmDetails(alarmId = 1, hour = 6, minute = 0, repeatDays = 0b1111111, isOneTime = false)
        assertEquals("6:00 AM · Every day", formatTimeSubtitle(details))
    }

    @Test
    fun `specific days repeat`() {
        val details = TimeAlarmDetails(alarmId = 1, hour = 12, minute = 15, repeatDays = (1 shl 0) or (1 shl 2), isOneTime = false)
        assertEquals("12:15 PM · Mon, Wed", formatTimeSubtitle(details))
    }

    @Test
    fun `radius switches to kilometres and trims trailing zeros`() {
        assertEquals("100 m", formatRadius(100f))
        assertEquals("950 m", formatRadius(950f))
        assertEquals("1 km", formatRadius(1000f))
        assertEquals("1.5 km", formatRadius(1500f))
        assertEquals("1.05 km", formatRadius(1050f))
        assertEquals("5 km", formatRadius(5000f))
    }

    @Test
    fun `location arrival`() {
        val details = LocationAlarmDetails(
            alarmId = 1,
            destinationLat = 0.0,
            destinationLng = 0.0,
            radiusMeters = 500f,
            triggerOn = GeofenceTrigger.ENTER,
        )
        assertEquals("On arrival · 500 m", formatLocationSubtitle(details))
    }

    @Test
    fun `location departure`() {
        val details = LocationAlarmDetails(
            alarmId = 1,
            destinationLat = 0.0,
            destinationLng = 0.0,
            radiusMeters = 1200f,
            triggerOn = GeofenceTrigger.EXIT,
        )
        assertEquals("On departure · 1200 m", formatLocationSubtitle(details))
    }
}
