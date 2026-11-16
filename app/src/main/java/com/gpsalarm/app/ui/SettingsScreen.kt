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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.gpsalarm.app.ui.theme.ArcadeRadius
import com.gpsalarm.app.ui.theme.TactileChip
import com.gpsalarm.app.ui.theme.TactileSurface
import com.gpsalarm.app.ui.theme.arcadeColors
import com.gpsalarm.app.ui.theme.arcadeType

private data class HealthCheck(
    val title: String,
    val okText: String,
    val problemText: String,
    val isOk: Boolean,
    val fix: Intent,
)

/**
 * Settings' content, as a floating panel over the dimmed home screen rather than a separate
 * nav screen — see [AlarmListScreen], which hosts this inside an `AnimatedVisibility` that
 * expands out of the gear icon and collapses back into it.
 */
@Composable
fun SettingsPanel(onClose: () -> Unit, onOpenDiagnostics: () -> Unit) {
    val context = LocalContext.current
    val colors = arcadeColors

    // Permissions are changed in system screens, so re-check whenever the user comes back.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val checks = remember(refresh) { healthChecks(context) }

    val themeMode by SettingsStore.themeMode(context).collectAsStateWithLifecycle()
    val mapStyle by SettingsStore.mapStyle(context).collectAsStateWithLifecycle()
    var snoozeMinutes by remember { mutableIntStateOf(SettingsStore.defaultSnoozeMinutes(context)) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("SETTINGS", style = arcadeType.button, color = colors.textPrimary)
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close settings", tint = colors.textPrimary)
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle("Appearance")
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theme", style = arcadeType.title, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            SettingsStore.ThemeMode.SYSTEM to "System",
                            SettingsStore.ThemeMode.LIGHT to "Light",
                            SettingsStore.ThemeMode.DARK to "Dark",
                        ).forEach { (mode, name) ->
                            TactileChip(text = name, selected = themeMode == mode, onClick = { SettingsStore.setThemeMode(context, mode) })
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Map", style = arcadeType.title, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            SettingsStore.MapStyle.MATCH_APP to "Match app",
                            SettingsStore.MapStyle.LIGHT to "Light",
                            SettingsStore.MapStyle.DARK to "Dark",
                        ).forEach { (style, name) ->
                            TactileChip(text = name, selected = mapStyle == style, onClick = { SettingsStore.setMapStyle(context, style) })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Alarms")
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Snooze length", style = arcadeType.title, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsStore.SNOOZE_CHOICES_MIN.forEach { minutes ->
                            TactileChip(
                                text = "${minutes}m",
                                selected = snoozeMinutes == minutes,
                                onClick = {
                                    snoozeMinutes = minutes
                                    SettingsStore.setDefaultSnoozeMinutes(context, minutes)
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Reliability")
            Text(
                "Alarms only fire on time if Android lets this app run in the background. Anything marked below can make an alarm late or silent.",
                style = arcadeType.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                checks.forEach { check -> HealthRow(check, context) }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Troubleshooting")
            SettingsCard(onClick = onOpenDiagnostics) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Diagnostics", style = arcadeType.title, color = colors.textPrimary)
                    Text(
                        "See when alarms were set up and when they fired, to find out why one was late.",
                        style = arcadeType.body,
                        color = colors.textSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("About")
            Text("GPS Alarm ${versionName(context)}", style = arcadeType.body, color = colors.textPrimary)
            Text(
                "Map data © OpenStreetMap contributors",
                style = arcadeType.ticker,
                color = colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = arcadeType.ticker,
        color = arcadeColors.accentPrimary,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

/** The one card shape every settings row/group shares: hard-shadow outline, no soft Material elevation. */
@Composable
private fun SettingsCard(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val colors = arcadeColors
    val modifier = if (onClick != null) {
        val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        Modifier.fillMaxWidth().clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    TactileSurface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(ArcadeRadius.md),
        fillColor = colors.bgSurface,
        outlineColor = colors.outline,
        shadowColor = colors.outline,
        pressed = false,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun HealthRow(check: HealthCheck, context: Context) {
    val colors = arcadeColors
    TactileSurface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(ArcadeRadius.md),
        fillColor = colors.bgSurface,
        outlineColor = colors.outline,
        shadowColor = colors.outline,
        pressed = false,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (check.isOk) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = if (check.isOk) "OK" else "Needs attention",
                tint = if (check.isOk) colors.accentSecondary else colors.danger,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(check.title, style = arcadeType.title, color = colors.textPrimary)
                Text(
                    if (check.isOk) check.okText else check.problemText,
                    style = arcadeType.body,
                    color = colors.textSecondary,
                )
            }
            if (!check.isOk) {
                TextButton(onClick = { runCatching { context.startActivity(check.fix) } }) {
                    Text("Fix", style = arcadeType.button, color = colors.accentPrimary)
                }
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
