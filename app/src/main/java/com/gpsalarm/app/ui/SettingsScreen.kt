package com.gpsalarm.app.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gpsalarm.app.settings.SettingsStore

private data class HealthCheck(
    val title: String,
    val okText: String,
    val problemText: String,
    val isOk: Boolean,
    val fix: Intent,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // Permissions are changed in system screens, so re-check whenever the user comes back.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val checks = remember(refresh) { healthChecks(context) }

    val themeMode by SettingsStore.themeMode(context).collectAsStateWithLifecycle()
    val mapStyle by SettingsStore.mapStyle(context).collectAsStateWithLifecycle()
    var snoozeMinutes by remember { mutableIntStateOf(SettingsStore.defaultSnoozeMinutes(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle("Appearance")
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            SettingsStore.ThemeMode.SYSTEM to "System",
                            SettingsStore.ThemeMode.LIGHT to "Light",
                            SettingsStore.ThemeMode.DARK to "Dark",
                        ).forEach { (mode, name) ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { SettingsStore.setThemeMode(context, mode) },
                                label = { Text(name) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Map", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Separate from the app theme, so you can keep the map easy to read.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            SettingsStore.MapStyle.MATCH_APP to "Match app",
                            SettingsStore.MapStyle.LIGHT to "Light",
                            SettingsStore.MapStyle.DARK to "Dark",
                        ).forEach { (style, name) ->
                            FilterChip(
                                selected = mapStyle == style,
                                onClick = { SettingsStore.setMapStyle(context, style) },
                                label = { Text(name) },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Alarms")
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Snooze length", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Applies to alarms you create from now on.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsStore.SNOOZE_CHOICES_MIN.forEach { minutes ->
                            FilterChip(
                                selected = snoozeMinutes == minutes,
                                onClick = {
                                    snoozeMinutes = minutes
                                    SettingsStore.setDefaultSnoozeMinutes(context, minutes)
                                },
                                label = { Text("$minutes min") },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Reliability")
            Text(
                "Alarms only fire on time if Android lets this app run in the background. Anything marked below can make an alarm late or silent.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                checks.forEach { check -> HealthRow(check, context) }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("About")
            Text(
                "GPS Alarm ${versionName(context)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Map data © OpenStreetMap contributors",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun HealthRow(check: HealthCheck, context: Context) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (check.isOk) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = if (check.isOk) "OK" else "Needs attention",
                tint = if (check.isOk) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(check.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (check.isOk) check.okText else check.problemText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!check.isOk) {
                TextButton(onClick = { runCatching { context.startActivity(check.fix) } }) { Text("Fix") }
            }
        }
    }
}

private fun healthChecks(context: Context): List<HealthCheck> {
    val pkg = context.packageName
    val packageUri = Uri.parse("package:$pkg")
    val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)

    val notificationsOk = NotificationManagerCompat.from(context).areNotificationsEnabled()

    val exactAlarmsOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    val batteryOk = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(pkg)

    val fineOk = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val backgroundOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

    return listOf(
        HealthCheck(
            title = "Notifications",
            okText = "Allowed",
            problemText = "Blocked. Alarms can't show their ring screen.",
            isOk = notificationsOk,
            fix = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg),
        ),
        HealthCheck(
            title = "Exact alarms",
            okText = "Allowed",
            problemText = "Not allowed. Time alarms may fire a few minutes late.",
            isOk = exactAlarmsOk,
            fix = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri),
        ),
        HealthCheck(
            title = "Battery optimization",
            okText = "Unrestricted",
            problemText = "Android may delay or stop alarms to save battery.",
            isOk = batteryOk,
            fix = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri),
        ),
        HealthCheck(
            title = "Location access",
            okText = "Always allowed",
            problemText = if (fineOk) {
                "Set to \"Allow all the time\" so location alarms fire when the app is closed."
            } else {
                "Not allowed. Location alarms can't work."
            },
            isOk = fineOk && backgroundOk,
            fix = appDetails,
        ),
    )
}

private fun versionName(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
