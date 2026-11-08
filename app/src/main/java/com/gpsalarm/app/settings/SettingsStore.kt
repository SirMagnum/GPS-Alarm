package com.gpsalarm.app.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SettingsStore {
    private const val PREFS = "settings"
    private const val KEY_SNOOZE_MIN = "default_snooze_min"

    const val DEFAULT_SNOOZE_MIN = 10
    val SNOOZE_CHOICES_MIN = listOf(5, 10, 15, 20)

    fun defaultSnoozeMinutes(context: Context): Int =
        prefs(context).getInt(KEY_SNOOZE_MIN, DEFAULT_SNOOZE_MIN)

    fun setDefaultSnoozeMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_SNOOZE_MIN, minutes).apply()
    }

    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    private const val KEY_THEME = "theme_mode"
    private var themeState: MutableStateFlow<ThemeMode>? = null

    /** Observable so the whole UI recomposes the moment the choice changes. */
    fun themeMode(context: Context): StateFlow<ThemeMode> = themeFlow(context)

    fun setThemeMode(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY_THEME, mode.name).apply()
        themeFlow(context).value = mode
    }

    /** Map tiles have their own light/dark choice, independent of the app theme. */
    enum class MapStyle { MATCH_APP, LIGHT, DARK }

    private const val KEY_MAP_STYLE = "map_style"
    private var mapStyleState: MutableStateFlow<MapStyle>? = null

    fun mapStyle(context: Context): StateFlow<MapStyle> = mapStyleFlow(context)

    fun setMapStyle(context: Context, style: MapStyle) {
        prefs(context).edit().putString(KEY_MAP_STYLE, style.name).apply()
        mapStyleFlow(context).value = style
    }

    private fun mapStyleFlow(context: Context): MutableStateFlow<MapStyle> = synchronized(this) {
        mapStyleState ?: MutableStateFlow(
            runCatching { MapStyle.valueOf(prefs(context).getString(KEY_MAP_STYLE, null).orEmpty()) }
                .getOrDefault(MapStyle.MATCH_APP),
        ).also { mapStyleState = it }
    }

    private fun themeFlow(context: Context): MutableStateFlow<ThemeMode> = synchronized(this) {
        themeState ?: MutableStateFlow(
            runCatching { ThemeMode.valueOf(prefs(context).getString(KEY_THEME, null).orEmpty()) }
                .getOrDefault(ThemeMode.SYSTEM),
        ).also { themeState = it }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
