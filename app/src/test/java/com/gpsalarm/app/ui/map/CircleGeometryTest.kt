package com.gpsalarm.app.ui.map

import org.osmdroid.util.GeoPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val EARTH_RADIUS_METERS = 6371000.0

private fun haversineMeters(a: GeoPoint, b: GeoPoint): Double {
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val h = sin(dLat / 2).let { it * it } + cos(lat1) * cos(lat2) * sin(dLon / 2).let { it * it }
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(h))
}

class CircleGeometryTest {

    @Test
    fun `every point lies at the requested radius from the center`() {
        val center = GeoPoint(37.4219, -122.0840)
        val radius = 500.0
        val points = circlePoints(center, radius, segments = 36)

        points.forEach { point ->
            val distance = haversineMeters(center, point)
            assertTrue(
                kotlin.math.abs(distance - radius) < 1.0,
                "Expected point at ~${radius}m, was ${distance}m",
            )
        }
    }

    @Test
    fun `first and last point coincide for a closed loop`() {
        val center = GeoPoint(0.0, 0.0)
        val points = circlePoints(center, 1000.0, segments = 36)

        assertEquals(points.first().latitude, points.last().latitude, 0.0001)
        assertEquals(points.first().longitude, points.last().longitude, 0.0001)
    }
}
