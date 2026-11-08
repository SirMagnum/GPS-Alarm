package com.gpsalarm.app.alarm

import java.util.Calendar
import kotlin.test.Test
import kotlin.test.assertEquals

class AlarmTimeCalculatorTest {

    private fun calendarAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Calendar =
        Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }

    @Test
    fun `one-time alarm later today fires today`() {
        val now = calendarAt(2026, Calendar.NOVEMBER, 9, 8, 0) // Monday
        val result = nextTriggerMillis(hour = 9, minute = 0, repeatDays = 0, now = now)
        val expected = calendarAt(2026, Calendar.NOVEMBER, 9, 9, 0).timeInMillis
        assertEquals(expected, result)
    }

    @Test
    fun `one-time alarm earlier today rolls to tomorrow`() {
        val now = calendarAt(2026, Calendar.NOVEMBER, 9, 8, 0) // Monday
        val result = nextTriggerMillis(hour = 7, minute = 0, repeatDays = 0, now = now)
        val expected = calendarAt(2026, Calendar.NOVEMBER, 10, 7, 0).timeInMillis
        assertEquals(expected, result)
    }

    @Test
    fun `repeating alarm picks next selected day, skipping today if already past`() {
        val now = calendarAt(2026, Calendar.NOVEMBER, 9, 8, 0) // Monday, bit 0
        // Monday (bit0) and Wednesday (bit2), alarm time 7:00 already passed today.
        val repeatDays = (1 shl 0) or (1 shl 2)
        val result = nextTriggerMillis(hour = 7, minute = 0, repeatDays = repeatDays, now = now)
        val expected = calendarAt(2026, Calendar.NOVEMBER, 11, 7, 0).timeInMillis // Wednesday
        assertEquals(expected, result)
    }

    @Test
    fun `repeating alarm fires today if the time hasn't passed yet`() {
        val now = calendarAt(2026, Calendar.NOVEMBER, 9, 8, 0) // Monday, bit 0
        val repeatDays = 1 shl 0
        val result = nextTriggerMillis(hour = 20, minute = 0, repeatDays = repeatDays, now = now)
        val expected = calendarAt(2026, Calendar.NOVEMBER, 9, 20, 0).timeInMillis
        assertEquals(expected, result)
    }

    @Test
    fun `repeating alarm wraps into next week when only today is selected and time has passed`() {
        val now = calendarAt(2026, Calendar.NOVEMBER, 9, 8, 0) // Monday, bit 0
        val repeatDays = 1 shl 0
        val result = nextTriggerMillis(hour = 7, minute = 0, repeatDays = repeatDays, now = now)
        val expected = calendarAt(2026, Calendar.NOVEMBER, 16, 7, 0).timeInMillis // next Monday
        assertEquals(expected, result)
    }
}
