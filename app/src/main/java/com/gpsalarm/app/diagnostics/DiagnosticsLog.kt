package com.gpsalarm.app.diagnostics

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * A small on-device event log (geofence registrations, geofence events, alarm rings) so a late or
 * missing alarm can be explained after the fact. Kept to the last [MAX_ENTRIES] lines.
 */
object DiagnosticsLog {
    data class Entry(val timeMillis: Long, val text: String)

    private const val FILE_NAME = "diagnostics.log"
    private const val MAX_ENTRIES = 500

    private val lock = Any()
    private var state: MutableStateFlow<List<Entry>>? = null

    fun entries(context: Context): StateFlow<List<Entry>> = synchronized(lock) { flow(context) }

    fun record(context: Context, text: String) {
        val entry = Entry(System.currentTimeMillis(), text.replace('\n', ' ').replace('\t', ' '))
        runCatching {
            synchronized(lock) {
                val flow = flow(context)
                val file = file(context)
                val updated = flow.value + entry
                if (updated.size > MAX_ENTRIES) {
                    val trimmed = updated.takeLast(MAX_ENTRIES)
                    file.writeText(trimmed.joinToString("") { it.toLine() })
                    flow.value = trimmed
                } else {
                    file.appendText(entry.toLine())
                    flow.value = updated
                }
            }
        }
    }

    fun clear(context: Context) {
        synchronized(lock) {
            runCatching { file(context).delete() }
            flow(context).value = emptyList()
        }
    }

    private fun flow(context: Context): MutableStateFlow<List<Entry>> =
        state ?: MutableStateFlow(read(file(context))).also { state = it }

    private fun file(context: Context) = File(context.applicationContext.filesDir, FILE_NAME)

    private fun read(file: File): List<Entry> = runCatching {
        if (!file.exists()) return emptyList()
        file.readLines().mapNotNull { line ->
            val split = line.indexOf('\t')
            if (split <= 0) return@mapNotNull null
            line.substring(0, split).toLongOrNull()?.let { Entry(it, line.substring(split + 1)) }
        }
    }.getOrDefault(emptyList())

    private fun Entry.toLine() = "$timeMillis\t$text\n"
}
