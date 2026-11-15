package com.gpsalarm.app.ui

import android.Manifest
import android.app.AlarmManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.PowerManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gpsalarm.app.BuildConfig
import com.gpsalarm.app.data.AlarmType
import com.gpsalarm.app.diagnostics.DiagnosticsLog
import com.gpsalarm.app.diagnostics.MockLocationInjector
import com.gpsalarm.app.geofence.GeofenceManager
import com.gpsalarm.app.ui.theme.backdropSource
import com.gpsalarm.app.ui.theme.glassInk
import com.gpsalarm.app.ui.theme.glassPanel
import com.gpsalarm.app.ui.theme.matteBackgroundBrush
import com.gpsalarm.app.ui.theme.rememberGlassBackdrop
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("MMM d HH:mm:ss", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(onBack: () -> Unit, viewModel: AlarmListViewModel = alarmListViewModel()) {
    val context = LocalContext.current
    val entries by DiagnosticsLog.entries(context).collectAsStateWithLifecycle()

    // Permissions change in system screens, so re-read them whenever the user comes back.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val status = remember(refresh) { statusLines(context) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backdrop = rememberGlassBackdrop()

    Scaffold(
        modifier = Modifier.background(matteBackgroundBrush(isDark)),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics", color = glassInk(isDark)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = glassInk(isDark))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.glassPanel(backdrop, RectangleShape, isDark),
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().backdropSource(backdrop).padding(contentPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item {
                Text("Right now", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
            }
            items(status) { line ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(line.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        line.value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (line.ok) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { copyLog(context, status, entries) }) { Text("Copy log") }
                    OutlinedButton(onClick = { DiagnosticsLog.clear(context) }) { Text("Clear") }
                }
            }
            if (BuildConfig.DEBUG) {
                item { SimulateArrivalSection(viewModel) }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Events (newest first)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (entries.isEmpty()) {
                    Text(
                        "Nothing recorded yet. Events appear here as alarms are set up and fire.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(entries.asReversed()) { entry ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        timeFormat.format(Date(entry.timeMillis)),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(entry.text, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private data class StatusLine(val name: String, val value: String, val ok: Boolean)

private fun statusLines(context: Context): List<StatusLine> {
    val lines = mutableListOf<StatusLine>()
    fun add(name: String, ok: Boolean, good: String, bad: String) {
        lines.add(StatusLine(name, if (ok) good else bad, ok))
    }

    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val background = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    add("Precise location", fine, "Allowed", "Not allowed")
    add("Location \"all the time\"", background, "Allowed", "Not allowed")
    add(
        "Phone location switch",
        LocationManagerCompat.isLocationEnabled(context.getSystemService(LocationManager::class.java)),
        "On",
        "Off",
    )
    add("Notifications", NotificationManagerCompat.from(context).areNotificationsEnabled(), "Allowed", "Blocked")
    add(
        "Exact alarms",
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
        "Allowed",
        "Not allowed",
    )
    val power = context.getSystemService(PowerManager::class.java)
    add("Battery optimization", power.isIgnoringBatteryOptimizations(context.packageName), "Unrestricted", "Optimized")
    add("Battery saver", !power.isPowerSaveMode, "Off", "On")
    lines.add(
        StatusLine(
            "Phone",
            "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}",
            ok = true,
        ),
    )
    return lines
}

/**
 * Debug-only: fakes GPS fixes to test a geofence crossing without physically travelling. Requires
 * Developer options -> "Select mock location app" to be set to GPS Alarm, or every tap fails.
 */
@Composable
private fun SimulateArrivalSection(viewModel: AlarmListViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val locationAlarms = alarms.filter { it.alarm.type == AlarmType.LOCATION && it.alarm.isEnabled && it.locationDetails != null }

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text("Simulate arrival (debug build)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Fakes a GPS jump to test the two-ring approach without walking there. First turn on " +
                "Developer options → Select mock location app → GPS Alarm, then tap the three " +
                "buttons below in order and watch the events below.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (locationAlarms.isEmpty()) {
            Text(
                "No enabled location alarms to simulate.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        locationAlarms.forEach { item ->
            val details = item.locationDetails ?: return@forEach
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(item.alarm.label, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    val outerRadius = details.radiusMeters + GeofenceManager.OUTER_BUFFER_METERS
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            coroutineScope.launch {
                                val (lat, lng) = offsetNorth(details.destinationLat, details.destinationLng, outerRadius + 1000.0)
                                simulate(context, lat, lng, "moved outside the outer ring of \"${item.alarm.label}\"")
                            }
                        }) { Text("1. Far away") }
                        OutlinedButton(onClick = {
                            coroutineScope.launch {
                                val (lat, lng) = offsetNorth(
                                    details.destinationLat,
                                    details.destinationLng,
                                    (details.radiusMeters + outerRadius) / 2.0,
                                )
                                simulate(context, lat, lng, "entered the outer ring of \"${item.alarm.label}\"")
                            }
                        }) { Text("2. Enter outer ring") }
                        OutlinedButton(onClick = {
                            coroutineScope.launch {
                                simulate(
                                    context,
                                    details.destinationLat,
                                    details.destinationLng,
                                    "arrived at \"${item.alarm.label}\"",
                                )
                            }
                        }) { Text("3. Arrive") }
                    }
                }
            }
        }
    }
}

private fun simulate(context: Context, lat: Double, lng: Double, description: String) {
    MockLocationInjector.inject(context, lat, lng)
        .onSuccess {
            DiagnosticsLog.record(context, "Simulated: $description")
            Toast.makeText(context, "Mock location sent", Toast.LENGTH_SHORT).show()
        }
        .onFailure {
            DiagnosticsLog.record(context, "Mock location FAILED: ${it::class.simpleName}: ${it.message}")
            Toast.makeText(context, "Couldn't set mock location: ${it.message}", Toast.LENGTH_LONG).show()
        }
}

/** Moves roughly [meters] north of (lat, lng) — good enough to step outside a geofence circle. */
private fun offsetNorth(lat: Double, lng: Double, meters: Double): Pair<Double, Double> {
    val metersPerDegreeLat = 111_320.0
    return (lat + meters / metersPerDegreeLat) to lng
}

private fun copyLog(context: Context, status: List<StatusLine>, entries: List<DiagnosticsLog.Entry>) {
    val text = buildString {
        appendLine("GPS Alarm diagnostics")
        status.forEach { appendLine("${it.name}: ${it.value}") }
        appendLine()
        entries.forEach { appendLine("${timeFormat.format(Date(it.timeMillis))}  ${it.text}") }
    }
    context.getSystemService(ClipboardManager::class.java)
        .setPrimaryClip(ClipData.newPlainText("GPS Alarm log", text))
    Toast.makeText(context, "Log copied", Toast.LENGTH_SHORT).show()
}
