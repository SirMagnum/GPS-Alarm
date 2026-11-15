package com.gpsalarm.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.gpsalarm.app.diagnostics.DiagnosticsLog

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(TimeAlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        DiagnosticsLog.record(context, "Time alarm #$alarmId fired")
        val serviceIntent = Intent(context, AlarmRingingService::class.java)
            .setAction(AlarmRingingService.ACTION_START)
            .putExtra(TimeAlarmScheduler.EXTRA_ALARM_ID, alarmId)
        runCatching { ContextCompat.startForegroundService(context, serviceIntent) }
            .onFailure { DiagnosticsLog.record(context, "Could NOT start ringing for #$alarmId: ${it.message}") }
    }
}
