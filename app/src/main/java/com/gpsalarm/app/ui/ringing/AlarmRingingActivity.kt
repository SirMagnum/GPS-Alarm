package com.gpsalarm.app.ui.ringing

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gpsalarm.app.alarm.AlarmRingingService
import com.gpsalarm.app.alarm.TimeAlarmScheduler
import com.gpsalarm.app.data.AlarmRepository
import com.gpsalarm.app.data.AppDatabase
import com.gpsalarm.app.ui.theme.GpsAlarmTheme

class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val alarmId = intent.getLongExtra(TimeAlarmScheduler.EXTRA_ALARM_ID, -1L)

        setContent {
            GpsAlarmTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmRingingScreen(
                        alarmId = alarmId,
                        onDismiss = { sendServiceAction(alarmId, AlarmRingingService.ACTION_DISMISS) },
                        onSnooze = { sendServiceAction(alarmId, AlarmRingingService.ACTION_SNOOZE) },
                    )
                }
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun sendServiceAction(alarmId: Long, action: String) {
        val intent = Intent(this, AlarmRingingService::class.java)
            .setAction(action)
            .putExtra(TimeAlarmScheduler.EXTRA_ALARM_ID, alarmId)
        startService(intent)
        finish()
    }
}

@Composable
private fun AlarmRingingScreen(alarmId: Long, onDismiss: () -> Unit, onSnooze: () -> Unit) {
    val context = LocalContext.current
    var label by remember { mutableStateOf("Alarm") }

    LaunchedEffect(alarmId) {
        val repository = AlarmRepository(AppDatabase.getInstance(context).alarmDao())
        repository.getAlarm(alarmId)?.let { label = it.label }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Dismiss")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onSnooze, modifier = Modifier.fillMaxWidth()) {
            Text("Snooze")
        }
    }
}
