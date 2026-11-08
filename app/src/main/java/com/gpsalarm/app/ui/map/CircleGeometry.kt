package com.gpsalarm.app.ui.map

import org.osmdroid.util.GeoPoint
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val EARTH_RADIUS_METERS = 6371000.0

/** Points approximating a circle of [radiusMeters] around [center], for drawing as a map polygon. */
fun circlePoints(center: GeoPoint, radiusMeters: Double, segments: Int = 72): List<GeoPoint> {
    val angularDistance = radiusMeters / EARTH_RADIUS_METERS
    val lat1 = Math.toRadians(center.latitude)
    val lon1 = Math.toRadians(center.longitude)

    return (0..segments).map { i ->
        val bearing = 2 * PI * i / segments
        val lat2 = asin(sin(lat1) * cos(angularDistance) + cos(lat1) * sin(angularDistance) * cos(bearing))
        val lon2 = lon1 + atan2(
            sin(bearing) * sin(angularDistance) * cos(lat1),
            cos(angularDistance) - sin(lat1) * sin(lat2),
        )
        GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2))
    }
}
