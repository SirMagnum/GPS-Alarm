package com.gpsalarm.app

import android.app.Application
import org.osmdroid.config.Configuration

class GpsAlarmApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            userAgentValue = packageName
            // Internal storage, not getExternalFilesDir: the FUSE-backed external storage on
            // modern Android denies the ioctl osmdroid's SQLite tile cache needs for locking.
            osmdroidBasePath = filesDir
            osmdroidTileCache = java.io.File(filesDir, "tiles")
        }
    }
}
