package com.gpsalarm.app.alarm

import java.util.Calendar

/** Bit 0 = Monday .. bit 6 = Sunday, matching [com.gpsalarm.app.data.TimeAlarmDetails.repeatDays]. */
private fun dayOfWeekToBit(calendarDayOfWeek: Int): Int = when (calendarDayOfWeek) {
    Calendar.MONDAY -> 0
    Calendar.TUESDAY -> 1
    Calendar.WEDNESDAY -> 2
    Calendar.THURSDAY -> 3
    Calendar.FRIDAY -> 4
    Calendar.SATURDAY -> 5
    else -> 6 // Calendar.SUNDAY
}

/**
 * Next trigger time for a time alarm. For a non-repeating alarm (repeatDays == 0), this is
 * today at hour:minute, or tomorrow if that has already passed. For a repeating alarm, it's the
 * soonest future occurrence among the selected days.
 */
fun nextTriggerMillis(
    hour: Int,
    minute: Int,
    repeatDays: Int,
    now: Calendar = Calendar.getInstance(),
): Long {
    val candidate = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (repeatDays == 0) {
        if (candidate.timeInMillis <= now.timeInMillis) {
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }
        return candidate.timeInMillis
    }

    for (offset in 0..7) {
        val day = (candidate.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
        val bit = dayOfWeekToBit(day.get(Calendar.DAY_OF_WEEK))
        if ((repeatDays shr bit) and 1 == 1 && day.timeInMillis > now.timeInMillis) {
            return day.timeInMillis
        }
    }
    error("repeatDays=$repeatDays selects no day")
}
