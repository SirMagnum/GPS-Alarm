package com.gpsalarm.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.gpsalarm.app.data.GeofenceTrigger
import com.gpsalarm.app.settings.SettingsStore
import com.gpsalarm.app.ui.map.LocationPickerMap
import com.gpsalarm.app.ui.map.Place
import com.gpsalarm.app.ui.map.reverseGeocode
import com.gpsalarm.app.ui.map.suggestPlaces
import com.gpsalarm.app.ui.theme.ArcadeRadius
import com.gpsalarm.app.ui.theme.TactileButton
import com.gpsalarm.app.ui.theme.TactileSurface
import com.gpsalarm.app.ui.theme.arcadeColors
import com.gpsalarm.app.ui.theme.arcadeType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.io.IOException
import java.util.Locale
import kotlin.math.roundToInt

private val defaultCenter = GeoPoint(20.0, 0.0)

/**
 * Content for the floating "new alarm" tray (see AlarmListScreen's FloatingTray) — a title row
 * with a close "X", then the same fields used when editing an existing alarm from the list.
 */
@Composable
fun NewLocationAlarmTray(onClose: () -> Unit, viewModel: AddEditLocationAlarmViewModel = addEditLocationAlarmViewModel()) {
    val colors = arcadeColors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("NEW LOCATION ALARM", style = arcadeType.button, color = colors.textPrimary)
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = colors.textPrimary)
            }
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxSize(),
        ) {
            LocationAlarmFields(existing = null, onDone = onClose, viewModel = viewModel, mapHeight = null)
        }
    }
}

/**
 * The editor shown inline, inside an alarm's own list card, when that card is expanded —
 * no navigation, no separate screen. The card grows around this content, pushing the cards
 * below it down the list.
 */
@Composable
fun InlineLocationAlarmEditor(
    alarmId: Long,
    onDone: () -> Unit,
    viewModel: AddEditLocationAlarmViewModel = addEditLocationAlarmViewModel(),
) {
    var existing by remember { mutableStateOf<LocationAlarmEdit?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(alarmId) {
        existing = viewModel.load(alarmId)
        loading = false
        if (existing == null) onDone() // alarm was deleted out from under us
    }

    if (loading) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // The map can't use weight(1f) here — unlike the full-screen Scaffold, a LazyColumn item
        // has no bounded height to share, so it gets a fixed height instead.
        LocationAlarmFields(existing = existing, onDone = onDone, viewModel = viewModel, mapHeight = 260.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationAlarmFields(
    existing: LocationAlarmEdit?,
    onDone: () -> Unit,
    viewModel: AddEditLocationAlarmViewModel,
    mapHeight: androidx.compose.ui.unit.Dp?,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var fineGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var backgroundGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val foregroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }
    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> backgroundGranted = granted }

    var label by remember { mutableStateOf(existing?.alarm?.label.orEmpty()) }
    var center by remember {
        mutableStateOf(existing?.details?.let { GeoPoint(it.destinationLat, it.destinationLng) } ?: defaultCenter)
    }
    var address by remember { mutableStateOf(existing?.details?.destinationAddress) }
    var needsReverse by remember { mutableStateOf(existing != null && existing.details.destinationAddress == null) }
    var radiusMeters by remember { mutableStateOf(existing?.details?.radiusMeters ?: 500f) }
    var vibrate by remember { mutableStateOf(existing?.alarm?.vibrate ?: true) }
    var soundUri by remember { mutableStateOf(existing?.alarm?.soundUri?.let(Uri::parse)) }

    var recenterToken by remember { mutableIntStateOf(0) }
    var locating by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // What's in the box vs. what drives suggestions: picking a suggestion rewrites the box but must
    // not fire a new search, so only typing updates [suggestQuery].
    var searchQuery by remember { mutableStateOf("") }
    var suggestQuery by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchMessage by remember { mutableStateOf<String?>(null) }

    fun clearSearchResults() {
        results = emptyList()
        searchMessage = null
    }

    fun pickPoint(point: GeoPoint, recenter: Boolean = false) {
        center = point
        address = null
        needsReverse = true
        if (recenter) recenterToken++
        clearSearchResults()
        focusManager.clearFocus()
    }

    fun selectPlace(place: Place) {
        center = GeoPoint(place.lat, place.lng)
        address = place.displayName
        needsReverse = false
        recenterToken++
        searchQuery = place.title
        suggestQuery = ""
        clearSearchResults()
        focusManager.clearFocus()
    }

    fun showMessage(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(message, actionLabel, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }

    fun useMyLocation() {
        if (!fineGranted) {
            foregroundPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
            return
        }
        if (locating) return

        val locationManager = context.getSystemService(LocationManager::class.java)
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
            showMessage("Location is turned off.", actionLabel = "Settings") {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            return
        }

        locating = true
        val client = LocationServices.getFusedLocationProviderClient(context)

        fun finish(location: Location?) {
            locating = false
            if (location != null) {
                pickPoint(GeoPoint(location.latitude, location.longitude), recenter = true)
            } else {
                showMessage("Couldn't get your location. Try again in a moment.")
            }
        }

        // A fresh fix can take a while indoors; fall back to the last known position rather than doing nothing.
        fun fallBackToLastKnown() {
            client.lastLocation.addOnCompleteListener { task -> finish(if (task.isSuccessful) task.result else null) }
        }

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(30_000)
            .setDurationMillis(10_000)
            .build()
        runCatching {
            client.getCurrentLocation(request, null)
                .addOnSuccessListener { location -> if (location != null) finish(location) else fallBackToLastKnown() }
                .addOnFailureListener { fallBackToLastKnown() }
        }.onFailure { locating = false }
    }

    // Type-ahead: wait for a short pause in typing, then ask for suggestions near the map.
    LaunchedEffect(suggestQuery) {
        val query = suggestQuery.trim()
        if (query.length < 2) {
            searching = false
            clearSearchResults()
            return@LaunchedEffect
        }
        delay(350)
        searching = true
        try {
            val bias = center.takeIf { it != defaultCenter }
            val found = suggestPlaces(query, bias?.latitude, bias?.longitude)
            results = found
            searchMessage = if (found.isEmpty()) "No places found for \"$query\"." else null
        } catch (e: IOException) {
            results = emptyList()
            searchMessage = "Can't reach the search service. Check your connection."
        } finally {
            // A newer keystroke cancels this run and owns the spinner now.
            if (isActive) searching = false
        }
    }

    // Start a new alarm where the user is; an existing alarm keeps its saved destination.
    if (fineGranted && existing == null) {
        LaunchedEffect(Unit) {
            runCatching {
                LocationServices.getFusedLocationProviderClient(context).lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            center = GeoPoint(location.latitude, location.longitude)
                            needsReverse = true
                        }
                    }
            }
        }
    }

    // Name the spot once the pin settles; restarting on every move debounces dragging.
    LaunchedEffect(center, needsReverse) {
        if (!needsReverse) return@LaunchedEffect
        delay(600)
        address = reverseGeocode(center.latitude, center.longitude)
        needsReverse = false
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!fineGranted) {
                PermissionRationale(
                    text = "GPS Alarm needs location access to detect when you arrive at or leave a destination.",
                    buttonText = "Allow location access",
                    onClick = {
                        foregroundPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    },
                )
            } else {
                if (!backgroundGranted) {
                    PermissionRationale(
                        text = "For this alarm to fire even when the app is closed, allow location access \"all the time\".",
                        buttonText = "Allow all the time",
                        onClick = { backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                MapWithControls(
                    modifier = if (mapHeight != null) Modifier.height(mapHeight) else Modifier.weight(1f),
                    center = center,
                    radiusMeters = radiusMeters,
                    addressText = address ?: "%.4f, %.4f".format(Locale.US, center.latitude, center.longitude),
                    onPickPoint = ::pickPoint,
                    recenterToken = recenterToken,
                    searchQuery = searchQuery,
                    onSearchQueryChange = {
                        searchQuery = it
                        suggestQuery = it
                    },
                    onSearch = {
                        // Keyboard search key: take the best suggestion if there is one.
                        results.firstOrNull()?.let(::selectPlace) ?: focusManager.clearFocus()
                    },
                    searching = searching,
                    results = results,
                    searchMessage = searchMessage,
                    onSelectPlace = ::selectPlace,
                    locating = locating,
                    onUseMyLocation = ::useMyLocation,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Radius", style = arcadeType.title, color = arcadeColors.textPrimary)
                    Text(
                        formatRadius(radiusMeters),
                        style = arcadeType.ticker.copy(fontSize = 20.sp),
                        color = arcadeColors.accentSecondary,
                    )
                }
                Slider(
                    value = radiusMeters,
                    onValueChange = { radiusMeters = (it / 50f).roundToInt() * 50f },
                    valueRange = 100f..5000f,
                    colors = SliderDefaults.colors(
                        thumbColor = arcadeColors.accentSecondary,
                        activeTrackColor = arcadeColors.accentSecondary,
                        inactiveTrackColor = arcadeColors.outline.copy(alpha = 0.3f),
                    ),
                )

                SoundAndVibrateSection(
                    soundUri = soundUri,
                    onSoundUriChange = { soundUri = it },
                    vibrate = vibrate,
                    onVibrateChange = { vibrate = it },
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label", style = arcadeType.body) },
                    shape = RoundedCornerShape(ArcadeRadius.sm),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = arcadeColors.outline,
                        unfocusedIndicatorColor = arcadeColors.outline,
                        focusedContainerColor = arcadeColors.bgSurface,
                        unfocusedContainerColor = arcadeColors.bgSurface,
                        focusedTextColor = arcadeColors.textPrimary,
                        unfocusedTextColor = arcadeColors.textPrimary,
                        cursorColor = arcadeColors.accentSecondary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                TactileButton(
                    text = if (existing == null) "Save" else "Save changes",
                    fillColor = arcadeColors.accentSecondary,
                    onClick = {
                        viewModel.save(
                            existing = existing?.alarm,
                            label = label,
                            lat = center.latitude,
                            lng = center.longitude,
                            address = address,
                            radiusMeters = radiusMeters,
                            triggerOn = GeofenceTrigger.ENTER, // arrival only
                            soundUri = soundUri?.toString(),
                            vibrate = vibrate,
                            snoozeDurationMin = SettingsStore.defaultSnoozeMinutes(context),
                            onSaved = onDone,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun MapWithControls(
    modifier: Modifier,
    center: GeoPoint,
    radiusMeters: Float,
    addressText: String,
    onPickPoint: (GeoPoint) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    searching: Boolean,
    results: List<Place>,
    searchMessage: String?,
    onSelectPlace: (Place) -> Unit,
    recenterToken: Int,
    locating: Boolean,
    onUseMyLocation: () -> Unit,
) {
    val panelVisible = results.isNotEmpty() || searchMessage != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large),
    ) {
        LocationPickerMap(
            center = center,
            radiusMeters = radiusMeters,
            onLocationSelected = onPickPoint,
            recenterToken = recenterToken,
            modifier = Modifier,
        )

        Column(modifier = Modifier.align(Alignment.TopCenter).padding(12.dp)) {
            SearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onSearch = onSearch,
                searching = searching,
            )
            AnimatedVisibility(visible = panelVisible) {
                TactileSurface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(ArcadeRadius.sm),
                    fillColor = arcadeColors.bgSurface,
                    outlineColor = arcadeColors.outline,
                    shadowColor = arcadeColors.outline,
                    pressed = false,
                    shadowOffset = 3.dp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (searchMessage != null) {
                            Text(
                                searchMessage,
                                style = arcadeType.body,
                                color = arcadeColors.textSecondary,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                        results.forEachIndexed { index, place ->
                            if (index > 0) {
                                HorizontalDivider(color = arcadeColors.outline.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
                            }
                            ResultRow(place, onClick = { onSelectPlace(place) })
                        }
                    }
                }
            }
        }

        if (!panelVisible) {
            TactileSurface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(ArcadeRadius.sm),
                fillColor = arcadeColors.bgSurface,
                outlineColor = arcadeColors.outline,
                shadowColor = arcadeColors.outline,
                pressed = false,
                shadowOffset = 3.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, end = 72.dp, bottom = 30.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = arcadeColors.accentSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        addressText,
                        style = arcadeType.body,
                        color = arcadeColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (!panelVisible) {
            val locateInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            TactileSurface(
                shape = androidx.compose.foundation.shape.CircleShape,
                fillColor = arcadeColors.bgSurface,
                outlineColor = arcadeColors.outline,
                shadowColor = arcadeColors.outline,
                pressed = false,
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 30.dp)
                    .size(48.dp)
                    .clickable(interactionSource = locateInteraction, indication = null, onClick = onUseMyLocation),
            ) {
                if (locating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = arcadeColors.textPrimary,
                    )
                } else {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Use my location", tint = arcadeColors.textPrimary)
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    searching: Boolean,
) {
    TactileSurface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(ArcadeRadius.sm),
        fillColor = arcadeColors.bgSurface,
        outlineColor = arcadeColors.outline,
        shadowColor = arcadeColors.outline,
        pressed = false,
        shadowOffset = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search", style = arcadeType.body) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = arcadeColors.textSecondary) },
            trailingIcon = {
                when {
                    searching -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = arcadeColors.accentPrimary)
                    query.isNotEmpty() -> IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = arcadeColors.textSecondary)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            textStyle = arcadeType.body,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = arcadeColors.bgSurface,
                unfocusedContainerColor = arcadeColors.bgSurface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = arcadeColors.textPrimary,
                unfocusedTextColor = arcadeColors.textPrimary,
                cursorColor = arcadeColors.accentPrimary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ResultRow(place: Place, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            place.title,
            style = arcadeType.body,
            color = arcadeColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (place.subtitle.isNotEmpty()) {
            Text(
                place.subtitle,
                style = arcadeType.ticker,
                color = arcadeColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PermissionRationale(text: String, buttonText: String, onClick: () -> Unit) {
    TactileSurface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(ArcadeRadius.md),
        fillColor = arcadeColors.bgSurface,
        outlineColor = arcadeColors.outline,
        shadowColor = arcadeColors.outline,
        pressed = false,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text, style = arcadeType.body, color = arcadeColors.textPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            TactileButton(text = buttonText, onClick = onClick, modifier = Modifier.fillMaxWidth(), height = 48.dp)
        }
    }
}
