package com.gpsalarm.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gpsalarm.app.data.Alarm
import com.gpsalarm.app.data.AlarmType
import com.gpsalarm.app.data.AlarmWithDetails
import com.gpsalarm.app.ui.theme.ArcadeRadius
import com.gpsalarm.app.ui.theme.TactileSurface
import com.gpsalarm.app.ui.theme.TactileToggle
import com.gpsalarm.app.ui.theme.arcadeColors
import com.gpsalarm.app.ui.theme.arcadeType
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AlarmListScreen(
    onOpenDiagnostics: () -> Unit,
    viewModel: AlarmListViewModel = alarmListViewModel(),
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val colors = arcadeColors

    // Only one card is expanded (editing) at a time — tapping a card expands it in place and
    // collapses whichever other card was open, rather than navigating to a new screen.
    var expandedAlarmId by remember { mutableStateOf<Long?>(null) }
    // Settings and "new alarm" are floating trays that slide up over a dimmed home screen,
    // rather than separate screens — same mechanism (FloatingTray) for all three.
    var settingsOpen by remember { mutableStateOf(false) }
    var addAlarmType by remember { mutableStateOf<AlarmType?>(null) }

    // None of the above are nav destinations, so the system back button doesn't know about them
    // on its own — without this, back always fell straight through to exiting the app instead of
    // closing whatever's open first. A tray sits visually on top of an expanded card, so it takes
    // priority; only once everything's closed does back do its normal "exit app" thing.
    BackHandler(enabled = settingsOpen || addAlarmType != null || expandedAlarmId != null) {
        when {
            settingsOpen -> settingsOpen = false
            addAlarmType != null -> addAlarmType = null
            expandedAlarmId != null -> expandedAlarmId = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.background(colors.bgBase),
            containerColor = colors.bgBase,
            contentColor = colors.textPrimary,
            floatingActionButton = {
                // Hidden while a card is expanded: the editor's own Save button is the one
                // confirm action then, so a second "+" floating nearby would be ambiguous.
                if (expandedAlarmId == null) {
                    AddAlarmFab(onAdd = { type -> addAlarmType = type })
                }
            },
        ) { contentPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                ClockHeader(onOpenSettings = { settingsOpen = true })

                if (alarms.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = colors.textDisabled, modifier = Modifier.size(56.dp))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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

        FloatingTray(visible = settingsOpen, onDismiss = { settingsOpen = false }) {
            SettingsPanel(onClose = { settingsOpen = false }, onOpenDiagnostics = onOpenDiagnostics)
        }
        FloatingTray(visible = addAlarmType == AlarmType.TIME, onDismiss = { addAlarmType = null }) {
            NewTimeAlarmTray(onClose = { addAlarmType = null })
        }
        FloatingTray(visible = addAlarmType == AlarmType.LOCATION, onDismiss = { addAlarmType = null }) {
            NewLocationAlarmTray(onClose = { addAlarmType = null })
        }
    }
}

/**
 * The shared "floating container" chrome for Settings and the two new-alarm trays: a scrim that
 * fades in on its own, and a tray — already reading as a bottom-sheet-style slab — that slides up
 * from off-screen rather than scaling in from whichever icon triggered it.
 */
@Composable
private fun FloatingTray(visible: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val colors = arcadeColors

    AnimatedVisibility(visible = visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(320)) { fullHeight -> fullHeight },
        exit = slideOutVertically(tween(260)) { fullHeight -> fullHeight },
    ) {
        TactileSurface(
            shape = RoundedCornerShape(topStart = ArcadeRadius.lg, topEnd = ArcadeRadius.lg, bottomStart = 0.dp, bottomEnd = 0.dp),
            fillColor = colors.bgBase,
            outlineColor = colors.outline,
            shadowColor = colors.outline,
            pressed = false,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp)
                // Swallow taps so they don't fall through to the scrim's dismiss handler.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        ) {
            content()
        }
    }
}

/** Large live clock + date — the one headline numeral every other screen's clock shares visually. */
@Composable
private fun ClockHeader(onOpenSettings: () -> Unit) {
    val colors = arcadeColors
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffectTicker { now = LocalDateTime.now() }

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 32.dp, bottom = 16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                now.format(DateTimeFormatter.ofPattern("h:mm", Locale.US)),
                style = arcadeType.displayTime,
                color = colors.textPrimary,
            )
            Text(
                now.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.US)).uppercase(),
                style = arcadeType.ticker,
                color = colors.textSecondary,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = colors.textPrimary)
        }
    }
}

@Composable
private fun LaunchedEffectTicker(onTick: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            onTick()
            delay(15_000)
        }
    }
}

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
    val colors = arcadeColors
    val alarm = item.alarm
    val isTime = alarm.type == AlarmType.TIME
    val accent = if (isTime) colors.accentPrimary else colors.accentSecondary
    val subtitle = if (isTime) {
        item.timeDetails?.let(::formatTimeSubtitle)
    } else {
        item.locationDetails?.let(::formatLocationSubtitle)
    }
    val cardShape = RoundedCornerShape(ArcadeRadius.md)
    val disabledAlpha = if (alarm.isEnabled) 1f else 0.55f

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val cardContent: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = interactionSource, indication = null, onClick = onToggleExpanded),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = disabledAlpha))
                        .then(Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    val icon = if (isTime) Icons.Filled.Schedule else Icons.Filled.LocationOn
                    Icon(icon, contentDescription = null, tint = Color.White)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (isTime && item.timeDetails != null) {
                        Text(
                            formatAlarmHeadline(item.timeDetails.hour, item.timeDetails.minute),
                            style = arcadeType.alarmTime,
                            color = if (alarm.isEnabled) colors.textPrimary else colors.textDisabled,
                        )
                    } else {
                        Text(
                            alarm.label,
                            style = arcadeType.title,
                            color = if (alarm.isEnabled) colors.textPrimary else colors.textDisabled,
                        )
                    }
                    if (!subtitle.isNullOrEmpty()) {
                        Text(
                            subtitle.uppercase(),
                            style = arcadeType.ticker,
                            color = colors.textSecondary,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                TactileToggle(checked = alarm.isEnabled, onCheckedChange = onToggle)

                Spacer(modifier = Modifier.width(20.dp))

                val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand to edit",
                    tint = colors.textSecondary,
                    modifier = Modifier.rotate(chevronRotation),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(260)) + fadeIn(tween(200, delayMillis = 80)),
                exit = shrinkVertically(tween(220)) + fadeOut(tween(140)),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    if (isTime) {
                        InlineTimeAlarmEditor(alarmId = alarm.id, onDone = onCollapse)
                    } else {
                        InlineLocationAlarmEditor(alarmId = alarm.id, onDone = onCollapse)
                    }
                }
            }
        }
    }

    if (expanded) {
        // Swiping to delete while the card's own content has sliders/a map would fight those
        // gestures, so the dismiss-by-swipe affordance only applies to a collapsed card.
        TactileSurface(
            shape = cardShape,
            fillColor = colors.bgSurface,
            outlineColor = colors.outline,
            shadowColor = colors.outline,
            pressed = false,
            modifier = modifier.fillMaxWidth(),
        ) {
            cardContent()
        }
    } else {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                val dismissed = value == SwipeToDismissBoxValue.StartToEnd
                if (dismissed) onDelete()
                dismissed
            },
        )

        SwipeToDismissBox(
            state = dismissState,
            modifier = modifier,
            // Slide-right-only: the other direction's gesture is disabled outright (not just
            // rejected in confirmValueChange) so the card doesn't even drag that way.
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = false,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(cardShape)
                        .background(colors.danger)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
                }
            },
        ) {
            TactileSurface(
                shape = cardShape,
                fillColor = colors.bgSurface,
                outlineColor = colors.outline,
                shadowColor = colors.outline,
                pressed = pressed,
                modifier = Modifier.fillMaxWidth(),
            ) {
                cardContent()
            }
        }
    }
}

private fun formatAlarmHeadline(hour: Int, minute: Int): String {
    val h = if (hour % 12 == 0) 12 else hour % 12
    return "%d:%02d %s".format(Locale.US, h, minute, if (hour < 12) "AM" else "PM")
}

@Composable
private fun AddAlarmFab(onAdd: (AlarmType) -> Unit) {
    val colors = arcadeColors
    var expanded by remember { mutableStateOf(false) }

    // IntrinsicSize.Max makes both pills exactly as wide as whichever label is longer, rather
    // than guessing a fixed width — shared across both staggered items below.
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(IntrinsicSize.Max)) {
        // Cascades outward from the FAB rather than both popping in together: the pill closer
        // to the trigger (Time) leads, Location follows a beat later.
        StaggeredFabReveal(visible = expanded, delayMillis = 70) {
            LabeledMiniFab(
                icon = Icons.Filled.LocationOn,
                label = "Location",
                iconColor = colors.accentSecondary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                expanded = false
                onAdd(AlarmType.LOCATION)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        StaggeredFabReveal(visible = expanded, delayMillis = 0) {
            LabeledMiniFab(
                icon = Icons.Filled.Schedule,
                label = "Time",
                iconColor = colors.accentPrimary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                expanded = false
                onAdd(AlarmType.TIME)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        val fabInteraction = remember { MutableInteractionSource() }
        val fabPressed by fabInteraction.collectIsPressedAsState()
        TactileSurface(
            shape = CircleShape,
            fillColor = colors.accentPrimary,
            outlineColor = colors.outline,
            shadowColor = colors.outline,
            pressed = fabPressed,
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clickable(interactionSource = fabInteraction, indication = null) { expanded = !expanded },
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add alarm", tint = Color.White)
        }
    }
}

/** Delays its own reveal by [delayMillis] after [visible] turns true, so sibling calls with
 *  different delays cascade one after another instead of all animating at once. Collapses
 *  immediately (no delay) so closing the speed dial still feels snappy. */
@Composable
private fun StaggeredFabReveal(visible: Boolean, delayMillis: Int, content: @Composable () -> Unit) {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(delayMillis.toLong())
            show = true
        } else {
            show = false
        }
    }
    AnimatedVisibility(
        visible = show,
        enter = fadeIn(tween(180)) + slideInVertically(
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        ) { fullHeight -> fullHeight / 2 },
        exit = fadeOut(tween(120)) + slideOutVertically(tween(140)) { fullHeight -> fullHeight / 2 },
    ) {
        content()
    }
}

/** A type-choice button: icon kept (it's the recognizable part), now with its label alongside —
 *  neutral fill with just the icon colored, so it still reads as "a choice" against the solid
 *  primary "+" trigger below it. */
@Composable
private fun LabeledMiniFab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = arcadeColors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    TactileSurface(
        shape = RoundedCornerShape(ArcadeRadius.lg),
        fillColor = colors.bgSurfaceRaised,
        outlineColor = colors.outline,
        shadowColor = colors.outline,
        pressed = pressed,
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(52.dp)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 18.dp),
        ) {
            Icon(icon, contentDescription = null, tint = iconColor)
            Text(label, style = arcadeType.button, color = colors.textPrimary)
        }
    }
}
