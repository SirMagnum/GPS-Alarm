package com.gpsalarm.app.ui.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class Place(val displayName: String, val lat: Double, val lng: Double) {
    /** "Eiffel Tower" from "Eiffel Tower, 5 Avenue Anatole France, Paris, ...". */
    val title: String get() = displayName.substringBefore(",").trim()
    val subtitle: String get() = displayName.substringAfter(",", "").trim()
}

// Photon (photon.komoot.io) is built for search-as-you-type on OpenStreetMap data; Nominatim's
// public policy forbids typeahead use. Both are free and keyless, for light use.
private const val SUGGEST_URL = "https://photon.komoot.io/api/"
private const val REVERSE_URL = "https://nominatim.openstreetmap.org/reverse"
private const val USER_AGENT = "GPSAlarm/1.0 (com.gpsalarm.app)"
private const val TIMEOUT_MS = 8_000

fun parsePlaces(json: String): List<Place> {
    val features = JSONObject(json).optJSONArray("features") ?: return emptyList()
    return (0 until features.length()).mapNotNull { i ->
        val feature = features.getJSONObject(i)
        val coordinates = feature.optJSONObject("geometry")?.optJSONArray("coordinates")
        if (coordinates == null || coordinates.length() < 2) return@mapNotNull null
        val props = feature.optJSONObject("properties") ?: JSONObject()

        val street = listOf(props.optString("street"), props.optString("housenumber"))
            .filter { it.isNotBlank() }
            .joinToString(" ")
        val locality = props.optString("city").ifBlank { props.optString("district") }
            .ifBlank { props.optString("county") }
        val parts = listOf(props.optString("name"), street, locality, props.optString("state"), props.optString("country"))
            .filter { it.isNotBlank() }
            .distinct()
        if (parts.isEmpty()) return@mapNotNull null

        Place(parts.joinToString(", "), lat = coordinates.getDouble(1), lng = coordinates.getDouble(0))
    }
}

fun parseReverse(json: String): String? =
    JSONObject(json).optString("display_name").takeIf { it.isNotBlank() }

/**
 * Suggestions for a partially typed query, nearest to [biasLat]/[biasLng] first when given.
 * Throws [IOException] when the service can't be reached, so the UI can tell "no results" from "offline".
 */
suspend fun suggestPlaces(query: String, biasLat: Double? = null, biasLng: Double? = null): List<Place> =
    withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query, "UTF-8")
        val bias = if (biasLat != null && biasLng != null) "&lat=$biasLat&lon=$biasLng" else ""
        parsePlaces(httpGet("$SUGGEST_URL?limit=5&q=$q$bias"))
    }

suspend fun reverseGeocode(lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
    runCatching {
        parseReverse(httpGet("$REVERSE_URL?format=jsonv2&zoom=17&lat=$lat&lon=$lng"))
    }.getOrNull()
}

private fun httpGet(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept-Language", Locale.getDefault().toLanguageTag())
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException("HTTP ${connection.responseCode}")
        }
        return connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
