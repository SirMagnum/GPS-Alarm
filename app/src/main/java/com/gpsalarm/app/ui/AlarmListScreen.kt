package com.gpsalarm.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gpsalarm.app.data.Alarm
import com.gpsalarm.app.data.AlarmType
import com.gpsalarm.app.data.AlarmWithDetails
import com.gpsalarm.app.ui.theme.NumeralFontFamily
import com.gpsalarm.app.ui.theme.backdropSource
import com.gpsalarm.app.ui.theme.glassInk
import com.gpsalarm.app.ui.theme.glassPanel
import com.gpsalarm.app.ui.theme.matteBackgroundBrush
import com.gpsalarm.app.ui.theme.rememberGlassBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    onAddTimeAlarm: () -> Unit,
    onAddLocationAlarm: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: AlarmListViewModel = alarmListViewModel(),
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backdrop = rememberGlassBackdrop()

    // Only one card is expanded (editing) at a time — tapping a card expands it in place and
    // collapses whichever other card was open, rather than navigating to a new screen.
    var expandedAlarmId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = Modifier.background(matteBackgroundBrush(isDark)),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("Alarms", color = glassInk(isDark)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = glassInk(isDark))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.glassPanel(backdrop, RectangleShape, isDark),
            )
        },
        floatingActionButton = {
            AddAlarmFab(onAddTimeAlarm = onAddTimeAlarm, onAddLocationAlarm = onAddLocationAlarm)
        },
    ) { contentPadding ->
        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().backdropSource(backdrop).padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No alarms yet. Tap + to add one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().backdropSource(backdrop).padding(contentPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(alarms, key = { it.alarm.id }) { item ->
                    AlarmRow(
                        item = item,
                        expanded = expandedAlarmId == item.alarm.id,
                        onToggleExpanded = {
                            expandedAlarmId = if (expandedAlarmId == item.alarm.id) null else item.alarm.id
                        },
                        onCollapse = { expandedAlarmId = null },
                        onToggle = { enabled -> viewModel.setEnabled(item.alarm, enabled) },
                        onDelete = { viewModel.delete(item.alarm) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmRow(
    item: AlarmWithDetails,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onCollapse: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alarm = item.alarm
    val isTime = alarm.type == AlarmType.TIME
    val chipColor = if (isTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val chipOnColor = if (isTime) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary
    val subtitle = if (isTime) {
        item.timeDetails?.let(::formatTimeSubtitle)
    } else {
        item.locationDetails?.let(::formatLocationSubtitle)
    }
    val cardShape = MaterialTheme.shapes.large

    val cardContent: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(chipColor),
                    contentAlignment = Alignment.Center,
                ) {
                    val icon = if (isTime) Icons.Filled.Schedule else Icons.Filled.LocationOn
                    Icon(icon, contentDescription = null, tint = chipOnColor)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(alarm.label, style = MaterialTheme.typography.titleMedium)
                    if (!subtitle.isNullOrEmpty()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = NumeralFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = chipColor, checkedThumbColor = chipOnColor),
                )

                Spacer(modifier = Modifier.width(4.dp))

                val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand to edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(chevronRotation),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(260)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(220)) + fadeOut(tween(140)),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (isTime) {
                            InlineTimeAlarmEditor(alarmId = alarm.id, onDone = onCollapse)
                        } else {
                            InlineLocationAlarmEditor(alarmId = alarm.id, onDone = onCollapse)
                        }
                    }
                }
            }
        }
    }

    if (expanded) {
        // Swiping to delete while the card's own content has sliders/a map would fight those
        // gestures, so the dismiss-by-swipe affordance only applies to a collapsed card.
        Card(
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = modifier.fillMaxWidth(),
        ) {
            cardContent()
        }
    } else {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                val dismissed = value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart
                if (dismissed) onDelete()
                dismissed
            },
        )
        val revealColor by animateColorAsState(
            targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0f)
            } else {
                MaterialTheme.colorScheme.error
            },
            label = "swipeReveal",
        )

        SwipeToDismissBox(
            state = dismissState,
            modifier = modifier,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(cardShape)
                        .background(revealColor)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onError)
                }
            },
        ) {
            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                cardContent()
            }
        }
    }
}

@Composable
private fun AddAlarmFab(onAddTimeAlarm: () -> Unit, onAddLocationAlarm: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 45f else 0f, label = "fabRotation")

    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(160)) +
                scaleIn(tween(200), transformOrigin = TransformOrigin(1f, 1f)) +
                slideInVertically(tween(200)) { it / 3 },
            exit = fadeOut(tween(120)) + scaleOut(tween(120), transformOrigin = TransformOrigin(1f, 1f)),
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SpeedDialAction(
                    icon = Icons.Filled.LocationOn,
                    label = "Location alarm",
                    color = MaterialTheme.colorScheme.tertiary,
                    onColor = MaterialTheme.colorScheme.onTertiary,
                    onClick = {
                        expanded = false
                        onAddLocationAlarm()
                    },
                )
                SpeedDialAction(
                    icon = Icons.Filled.Schedule,
                    label = "Time alarm",
                    color = MaterialTheme.colorScheme.primary,
                    onColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = {
                        expanded = false
                        onAddTimeAlarm()
                    },
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        FloatingActionButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Filled.Add, contentDescription = "Add alarm", modifier = Modifier.rotate(rotation))
        }
    }
}

@Composable
private fun SpeedDialAction(icon: ImageVector, label: String, color: Color, onColor: Color, onClick: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent,
        shadowElevation = 4.dp,
        modifier = Modifier.glassPanel(backdrop = null, shape = MaterialTheme.shapes.extraLarge, isDark = isDark),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = glassInk(isDark))
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = onColor, modifier = Modifier.size(18.dp))
            }
        }
    }
}
