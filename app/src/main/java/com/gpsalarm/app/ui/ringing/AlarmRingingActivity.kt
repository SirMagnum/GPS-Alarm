package com.gpsalarm.app.ui.ringing

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gpsalarm.app.alarm.AlarmRingingService
import com.gpsalarm.app.alarm.TimeAlarmScheduler
import com.gpsalarm.app.data.AlarmRepository
import com.gpsalarm.app.data.AlarmType
import com.gpsalarm.app.data.AppDatabase
import com.gpsalarm.app.ui.theme.GpsAlarmTheme
import com.gpsalarm.app.ui.theme.glassInk
import com.gpsalarm.app.ui.theme.glassPanel
import com.gpsalarm.app.ui.theme.matteBackgroundBrush

class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val alarmId = intent.getLongExtra(TimeAlarmScheduler.EXTRA_ALARM_ID, -1L)

        setContent {
            GpsAlarmTheme {
                AlarmRingingScreen(
                    alarmId = alarmId,
                    onDismiss = { sendServiceAction(alarmId, AlarmRingingService.ACTION_DISMISS) },
                    onSnooze = { sendServiceAction(alarmId, AlarmRingingService.ACTION_SNOOZE) },
                )
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
    val isDark = isSystemInDarkTheme()
    var label by remember { mutableStateOf("Alarm") }
    var isLocationAlarm by remember { mutableStateOf(false) }

    LaunchedEffect(alarmId) {
        val repository = AlarmRepository(AppDatabase.getInstance(context).alarmDao())
        repository.getAlarm(alarmId)?.let {
            label = it.label
            isLocationAlarm = it.type == AlarmType.LOCATION
        }
    }

    val glow = if (isLocationAlarm) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(matteBackgroundBrush(isDark)),
    ) {
        // The one bold moment in the app: a soft brand-colored glow behind the ring, blurred
        // against itself only (safe without backdrop-blur support) rather than a flat icon.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(360.dp)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(colors = listOf(glow.copy(alpha = 0.45f), Color.Transparent)),
                    shape = CircleShape,
                ),
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(84.dp).glassPanel(backdrop = null, shape = CircleShape, isDark = isDark),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isLocationAlarm) Icons.Filled.LocationOn else Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = glow,
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(label, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(56.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = glow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Dismiss")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onSnooze,
                border = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .glassPanel(backdrop = null, shape = MaterialTheme.shapes.extraLarge, isDark = isDark),
            ) {
                Text("Snooze", color = glassInk(isDark))
            }
        }
    }
}
