package com.gpsalarm.app.alarm

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.gpsalarm.app.R
import com.gpsalarm.app.data.AlarmRepository
import com.gpsalarm.app.data.AlarmType
import com.gpsalarm.app.data.AppDatabase
import com.gpsalarm.app.ui.ringing.AlarmRingingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AlarmRingingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: AlarmRepository
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var ringJob: Job? = null
    private var ringingAlarmSnoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES

    override fun onCreate() {
        super.onCreate()
        repository = AlarmRepository(AppDatabase.getInstance(this).alarmDao())
        AlarmNotifications.ensureChannel(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(TimeAlarmScheduler.EXTRA_ALARM_ID, -1L) ?: -1L
        if (alarmId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_DISMISS -> stopRinging()
            ACTION_SNOOZE -> {
                TimeAlarmScheduler(this).scheduleSnooze(alarmId, ringingAlarmSnoozeMinutes)
                stopRinging()
            }
            else -> startRinging(alarmId)
        }
        return START_REDELIVER_INTENT
    }

    private fun startRinging(alarmId: Long) {
        startForeground(NOTIFICATION_ID, buildNotification(alarmId))
        acquireWakeLock()

        ringJob = scope.launch {
            val alarm = repository.getAlarm(alarmId)
            ringingAlarmSnoozeMinutes = alarm?.snoozeDurationMin ?: DEFAULT_SNOOZE_MINUTES

            val shouldAutoDisable = when (alarm?.type) {
                AlarmType.TIME -> repository.getTimeDetails(alarmId)?.isOneTime == true
                AlarmType.LOCATION -> repository.getLocationDetails(alarmId)?.oneShot == true
                null -> false
            }
            if (shouldAutoDisable) {
                repository.setEnabled(alarmId, false)
            }

            playSound(alarm?.soundUri)
            if (alarm?.vibrate != false) startVibration()
        }
    }

    private fun buildNotification(alarmId: Long): android.app.Notification {
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java)
            .putExtra(TimeAlarmScheduler.EXTRA_ALARM_ID, alarmId)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, alarmId.toInt(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val dismissPendingIntent = actionPendingIntent(alarmId, ACTION_DISMISS)
        val snoozePendingIntent = actionPendingIntent(alarmId, ACTION_SNOOZE)

        return NotificationCompat.Builder(this, AlarmNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle("Alarm")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .addAction(0, "Snooze", snoozePendingIntent)
            .build()
    }

    private fun actionPendingIntent(alarmId: Long, action: String): PendingIntent {
        val intent = Intent(this, AlarmRingingService::class.java)
            .setAction(action)
            .putExtra(TimeAlarmScheduler.EXTRA_ALARM_ID, alarmId)
        val requestCode = alarmId.toInt() * 10 + if (action == ACTION_DISMISS) 1 else 2
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun playSound(soundUri: String?) {
        val uri = soundUri?.let(Uri::parse)
            ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: return
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            isLooping = true
            runCatching {
                setDataSource(this@AlarmRingingService, uri)
                prepare()
                start()
            }
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 800, 500)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GpsAlarm:ringing").apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L)
        }
    }

    private fun stopRinging() {
        ringJob?.cancel()
        mediaPlayer?.let { runCatching { it.stop() }; it.release() }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopRinging()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.gpsalarm.app.action.START_RINGING"
        const val ACTION_DISMISS = "com.gpsalarm.app.action.DISMISS"
        const val ACTION_SNOOZE = "com.gpsalarm.app.action.SNOOZE"
        private const val DEFAULT_SNOOZE_MINUTES = 10
        private const val NOTIFICATION_ID = 42
    }
}
