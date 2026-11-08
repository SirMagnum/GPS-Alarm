package com.gpsalarm.app.ui.map

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gpsalarm.app.R
import com.gpsalarm.app.settings.SettingsStore
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.TilesOverlay

private const val FIT_PADDING_PX = 64

@SuppressLint("ClickableViewAccessibility")
@Composable
fun LocationPickerMap(
    center: GeoPoint,
    radiusMeters: Float,
    onLocationSelected: (GeoPoint) -> Unit,
    modifier: Modifier = Modifier,
    /** Bump to re-frame the camera on [center] even if [center] itself didn't change (e.g. after panning away). */
    recenterToken: Int = 0,
) {
    val context = LocalContext.current
    val currentOnLocationSelected by rememberUpdatedState(onLocationSelected)
    val mapView = remember { MapView(context) }
    val marker = remember { Marker(mapView) }
    val circle = remember { Polygon(mapView) }
    val accent = MaterialTheme.colorScheme.secondary.toArgb()

    DisposableEffect(mapView) {
        mapView.setMultiTouchControls(true)
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(center)
        // The map lives inside a vertically scrolling form; keep drags on the map from scrolling the page.
        mapView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) v.parent?.requestDisallowInterceptTouchEvent(true)
            false
        }

        marker.icon = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.infoWindow = null
        marker.isDraggable = true
        marker.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
            override fun onMarkerDrag(m: Marker) = Unit
            override fun onMarkerDragStart(m: Marker) = Unit
            override fun onMarkerDragEnd(m: Marker) = currentOnLocationSelected(m.position)
        })

        // Taps inside the circle must reach the map (to move the pin), not pop up the polygon's info bubble.
        circle.infoWindow = null
        circle.setOnClickListener { _, _, _ -> false }
        circle.fillPaint.color = (accent and 0x00FFFFFF) or 0x33000000
        circle.outlinePaint.apply {
            color = accent
            strokeWidth = 5f
            isAntiAlias = true
        }

        val tapReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                currentOnLocationSelected(p)
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        }
        mapView.overlays.add(MapEventsOverlay(tapReceiver))
        mapView.overlays.add(circle)
        mapView.overlays.add(marker)
        // OSM's tile license requires visible attribution. Offset above the bottom edge to clear the
        // floating controls the screen draws over the map.
        mapView.overlays.add(
            CopyrightOverlay(context).apply {
                setTextSize(9) // dp; osmdroid applies display density itself
                setOffset(8, 8)
            },
        )

        onDispose { mapView.onDetach() }
    }

    val appIsDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mapStyle by SettingsStore.mapStyle(context).collectAsStateWithLifecycle()
    val darkTheme = when (mapStyle) {
        SettingsStore.MapStyle.MATCH_APP -> appIsDark
        SettingsStore.MapStyle.LIGHT -> false
        SettingsStore.MapStyle.DARK -> true
    }
    LaunchedEffect(darkTheme) {
        mapView.overlayManager.tilesOverlay.setColorFilter(if (darkTheme) TilesOverlay.INVERT_COLORS else null)
        mapView.invalidate()
    }

    // Radius changes: re-fit instantly every frame, so the circle holds a constant on-screen size and the
    // map zooms smoothly underneath it. Pin moves: glide to the new spot with one animated fit.
    val lastCenter = remember { arrayOfNulls<GeoPoint>(1) }
    val lastToken = remember { intArrayOf(recenterToken) }
    LaunchedEffect(center, radiusMeters, recenterToken) {
        marker.position = center
        val points = circlePoints(center, radiusMeters.toDouble())
        circle.setPoints(points)

        val shouldAnimate = lastCenter[0] != center || lastToken[0] != recenterToken
        lastCenter[0] = center
        lastToken[0] = recenterToken
        val box = BoundingBox.fromGeoPoints(points)
        if (mapView.isLayoutOccurred) {
            mapView.zoomToBoundingBox(box, shouldAnimate, FIT_PADDING_PX)
        } else {
            mapView.addOnFirstLayoutListener { _, _, _, _, _ -> mapView.zoomToBoundingBox(box, false, FIT_PADDING_PX) }
        }
        mapView.invalidate()
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.large),
        factory = { mapView },
    )
}
