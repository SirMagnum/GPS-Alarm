package com.gpsalarm.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gpsalarm.app.data.Alarm
import com.gpsalarm.app.data.AlarmType

object Routes {
    const val ALARM_LIST = "alarm_list"
    const val SETTINGS = "settings"

    private const val ARG_ALARM_ID = "alarmId"
    private const val NO_ALARM = -1L

    // The same screens serve "new" (no id) and "edit" (with id).
    const val TIME_ALARM_PATTERN = "time_alarm?$ARG_ALARM_ID={$ARG_ALARM_ID}"
    const val LOCATION_ALARM_PATTERN = "location_alarm?$ARG_ALARM_ID={$ARG_ALARM_ID}"

    fun timeAlarm(alarmId: Long? = null) = "time_alarm" + (alarmId?.let { "?$ARG_ALARM_ID=$it" } ?: "")
    fun locationAlarm(alarmId: Long? = null) = "location_alarm" + (alarmId?.let { "?$ARG_ALARM_ID=$it" } ?: "")
    fun editAlarm(alarm: Alarm) = when (alarm.type) {
        AlarmType.TIME -> timeAlarm(alarm.id)
        AlarmType.LOCATION -> locationAlarm(alarm.id)
    }

    val alarmIdArguments = listOf(
        navArgument(ARG_ALARM_ID) {
            type = NavType.LongType
            defaultValue = NO_ALARM
        },
    )

    fun alarmId(entry: NavBackStackEntry): Long? =
        entry.arguments?.getLong(ARG_ALARM_ID)?.takeIf { it != NO_ALARM }
}

@Composable
fun GpsAlarmNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.ALARM_LIST) {
        composable(Routes.ALARM_LIST) {
            AlarmListScreen(
                onAddTimeAlarm = { navController.navigate(Routes.timeAlarm()) },
                onAddLocationAlarm = { navController.navigate(Routes.locationAlarm()) },
                onEditAlarm = { alarm -> navController.navigate(Routes.editAlarm(alarm)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TIME_ALARM_PATTERN, arguments = Routes.alarmIdArguments) { entry ->
            AddEditTimeAlarmScreen(alarmId = Routes.alarmId(entry), onDone = { navController.popBackStack() })
        }
        composable(Routes.LOCATION_ALARM_PATTERN, arguments = Routes.alarmIdArguments) { entry ->
            AddEditLocationAlarmScreen(alarmId = Routes.alarmId(entry), onDone = { navController.popBackStack() })
        }
    }
}
