package com.gpsalarm.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object Routes {
    const val ALARM_LIST = "alarm_list"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"
    const val TIME_ALARM = "time_alarm"
    const val LOCATION_ALARM = "location_alarm"
}

@Composable
fun GpsAlarmNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.ALARM_LIST) {
        composable(Routes.ALARM_LIST) {
            AlarmListScreen(
                onAddTimeAlarm = { navController.navigate(Routes.TIME_ALARM) },
                onAddLocationAlarm = { navController.navigate(Routes.LOCATION_ALARM) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
            )
        }
        composable(Routes.DIAGNOSTICS) {
            DiagnosticsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.TIME_ALARM,
            enterTransition = { fadeIn() + scaleIn(initialScale = 0.92f) },
            exitTransition = { fadeOut() },
            popExitTransition = { fadeOut() + scaleOut(targetScale = 0.92f) },
        ) {
            AddEditTimeAlarmScreen(onDone = { navController.popBackStack() })
        }
        composable(
            Routes.LOCATION_ALARM,
            enterTransition = { fadeIn() + scaleIn(initialScale = 0.92f) },
            exitTransition = { fadeOut() },
            popExitTransition = { fadeOut() + scaleOut(targetScale = 0.92f) },
        ) {
            AddEditLocationAlarmScreen(onDone = { navController.popBackStack() })
        }
    }
}
