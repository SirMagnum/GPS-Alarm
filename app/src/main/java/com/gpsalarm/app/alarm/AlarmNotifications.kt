package com.gpsalarm.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object AlarmNotifications {
    const val CHANNEL_ID = "alarm_channel"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(CHANNEL_ID, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alarm ringing notifications"
            // The service plays the alarm sound itself, so the channel stays silent.
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }
}
