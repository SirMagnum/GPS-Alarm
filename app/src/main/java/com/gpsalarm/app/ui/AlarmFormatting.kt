package com.gpsalarm.app.ui

import com.gpsalarm.app.data.GeofenceTrigger
import com.gpsalarm.app.data.LocationAlarmDetails
import com.gpsalarm.app.data.TimeAlarmDetails
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val dayAbbreviations = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** e.g. "7:00 AM · Weekdays", "7:00 AM · One-time", "7:00 AM · Mon, Wed, Fri". */
fun formatTimeSubtitle(details: TimeAlarmDetails): String {
    val time = LocalTime.of(details.hour, details.minute).format(timeFormatter)
    val days = details.repeatDays
    val repeatLabel = when {
        days == 0 -> "One-time"
        days == 0b1111111 -> "Every day"
        days == 0b0011111 -> "Weekdays"
        days == 0b1100000 -> "Weekends"
        else -> (0..6).filter { (days shr it) and 1 == 1 }.joinToString(", ") { dayAbbreviations[it] }
    }
    return "$time · $repeatLabel"
}

/** "800 m", "1.5 km", "2 km". */
fun formatRadius(meters: Float): String {
    if (meters < 1000f) return "${meters.toInt()} m"
    val km = "%.2f".format(Locale.US, meters / 1000f).trimEnd('0').trimEnd('.')
    return "$km km"
}

/** e.g. "On arrival · 500 m". */
fun formatLocationSubtitle(details: LocationAlarmDetails): String {
    val trigger = if (details.triggerOn == GeofenceTrigger.ENTER) "On arrival" else "On departure"
    return "$trigger · ${details.radiusMeters.toInt()} m"
}
