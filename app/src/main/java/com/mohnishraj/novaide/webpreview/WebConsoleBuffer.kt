package com.mohnishraj.novaide.webpreview

import java.text.SimpleDateFormat
import java.util.Date
import java.util.ArrayDeque
import java.util.Locale

data class WebConsoleEntry(
    val level: String,
    val message: String,
    val source: String = "",
    val line: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

class WebConsoleBuffer(private val capacity: Int = 300) {
    private val entries = ArrayDeque<WebConsoleEntry>()

    @Synchronized
    fun add(entry: WebConsoleEntry) {
        val safe = entry.copy(message = entry.message.take(8_000), source = entry.source.take(500))
        if (entries.lastOrNull()?.let { it.level == safe.level && it.message == safe.message && it.source == safe.source && it.line == safe.line } == true) return
        entries.addLast(safe)
        while (entries.size > capacity.coerceAtLeast(20)) entries.removeFirst()
    }

    @Synchronized fun clear() = entries.clear()
    @Synchronized fun snapshot(): List<WebConsoleEntry> = entries.toList()

    @Synchronized
    fun render(): String {
        if (entries.isEmpty()) return "No console or runtime messages yet."
        val time = SimpleDateFormat("HH:mm:ss", Locale.US)
        return entries.joinToString("\n\n") { entry ->
            buildString {
                append('[').append(time.format(Date(entry.timestamp))).append("] ")
                append(entry.level.uppercase(Locale.ROOT)).append("\n")
                append(entry.message)
                if (entry.source.isNotBlank()) append("\n").append(entry.source).append(if (entry.line > 0) ":${entry.line}" else "")
            }
        }
    }
}
