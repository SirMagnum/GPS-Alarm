package com.gpsalarm.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class TimeAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(alarmId: Long, hour: Int, minute: Int, repeatDays: Int) {
        val triggerAt = nextTriggerMillis(hour, minute, repeatDays)
        setExact(triggerAt, pendingIntentFor(alarmId))
    }

    fun scheduleSnooze(alarmId: Long, minutes: Int) {
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        setExact(triggerAt, pendingIntentFor(alarmId))
    }

    fun cancel(alarmId: Long) {
        alarmManager.cancel(pendingIntentFor(alarmId))
    }

    private fun setExact(triggerAt: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // User hasn't granted the exact-alarm permission; an inexact alarm still fires,
            // just not necessarily at the precise minute.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    private fun pendingIntentFor(alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).putExtra(EXTRA_ALARM_ID, alarmId)
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }
}
