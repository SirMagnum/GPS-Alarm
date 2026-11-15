package com.gpsalarm.app.diagnostics

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock

/**
 * Debug-only helper that fakes GPS fixes through Android's mock-location provider, so a geofence
 * crossing can be tested without physically travelling to it.
 *
 * Only works once the phone's Developer options -> "Select mock location app" is set to this app;
 * otherwise every call fails with a SecurityException, which [inject] reports as a [Result] failure
 * rather than throwing.
 */
object MockLocationInjector {
    private const val PROVIDER = LocationManager.GPS_PROVIDER

    fun inject(context: Context, lat: Double, lng: Double, accuracyMeters: Float = 8f): Result<Unit> = runCatching {
        val locationManager = context.getSystemService(LocationManager::class.java)
        ensureTestProvider(locationManager)
        val location = Location(PROVIDER).apply {
            latitude = lat
            longitude = lng
            altitude = 0.0
            accuracy = accuracyMeters
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        locationManager.setTestProviderLocation(PROVIDER, location)
    }

    @Suppress("DEPRECATION")
    private fun ensureTestProvider(locationManager: LocationManager) {
        // "gps" is already registered as the real hardware provider, so this call re-points that
        // name at a test provider instead — that's how Android's mock-location mechanism works.
        // Ignore failure here: it only happens if a previous call in this process already did it.
        runCatching {
            locationManager.addTestProvider(
                PROVIDER,
                /* requiresNetwork = */ false,
                /* requiresSatellite = */ false,
                /* requiresCell = */ false,
                /* hasMonetaryCost = */ false,
                /* supportsAltitude = */ true,
                /* supportsSpeed = */ true,
                /* supportsBearing = */ true,
                Criteria.POWER_LOW,
                Criteria.ACCURACY_FINE,
            )
        }
        locationManager.setTestProviderEnabled(PROVIDER, true)
    }
}
