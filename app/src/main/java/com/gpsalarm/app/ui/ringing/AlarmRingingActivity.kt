package com.gpsalarm.app.ui.ringing

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
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
import com.gpsalarm.app.ui.theme.TactileButton
import com.gpsalarm.app.ui.theme.arcadeColors
import com.gpsalarm.app.ui.theme.arcadeType

class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val alarmId = intent.getLongExtra(TimeAlarmScheduler.EXTRA_ALARM_ID, -1L)

        setContent {
            // Always dark, regardless of the app's own theme setting — glare reduction for a
            // screen that's often seen half-asleep or in the dark.
            GpsAlarmTheme(darkTheme = true) {
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
    val colors = arcadeColors
    var label by remember { mutableStateOf("Alarm") }
    var isLocationAlarm by remember { mutableStateOf(false) }

    LaunchedEffect(alarmId) {
        val repository = AlarmRepository(AppDatabase.getInstance(context).alarmDao())
        repository.getAlarm(alarmId)?.let {
            label = it.label
            isLocationAlarm = it.type == AlarmType.LOCATION
        }
    }

    // A looping ambient glow communicates "live/ringing" state — the one place the glow accent
    // is used purely atmospherically.
    val infiniteTransition = rememberInfiniteTransition(label = "ringGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgBase),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(360.dp)
                .blur(90.dp)
                .background(
                    Brush.radialGradient(colors = listOf(colors.accentGlow.copy(alpha = glowAlpha), Color.Transparent)),
                    shape = CircleShape,
                ),
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(colors.bgSurface, CircleShape)
                    .border(1.5.dp, colors.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isLocationAlarm) Icons.Filled.LocationOn else Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = colors.accentGlow,
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            if (label.isNotBlank()) {
                Text(label, style = arcadeType.title, color = colors.textPrimary)
            }
            Spacer(modifier = Modifier.height(56.dp))

            TactileButton(
                text = "Snooze",
                onClick = onSnooze,
                fillColor = colors.accentGlow,
                textColor = colors.textPrimary,
                modifier = Modifier.fillMaxWidth(),
                height = 64.dp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TactileButton(
                text = "Dismiss",
                onClick = onDismiss,
                outlined = true,
                modifier = Modifier.fillMaxWidth(),
                height = 64.dp,
            )
        }
    }
}
