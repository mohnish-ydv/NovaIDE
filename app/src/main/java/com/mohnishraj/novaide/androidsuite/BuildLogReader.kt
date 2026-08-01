package com.mohnishraj.novaide.androidsuite

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipInputStream

object BuildLogReader {
    private const val MAX_TEXT_BYTES = 8L * 1024L * 1024L
    private const val MAX_ENTRY_TEXT_BYTES = 2L * 1024L * 1024L
    private const val MAX_IGNORED_ENTRY_BYTES = 16L * 1024L * 1024L
    private const val MAX_ZIP_ENTRIES = 500
    private val textExtensions = setOf("txt", "log", "md", "out", "err", "json", "xml", "html")

    fun read(fileName: String, input: InputStream): String =
        if (fileName.lowercase(Locale.US).endsWith(".zip")) readZip(input) else readLimited(input, MAX_TEXT_BYTES)

    private fun readZip(input: InputStream): String {
        val sections = mutableListOf<Pair<String, String>>()
        var collectedBytes = 0L
        ZipInputStream(input.buffered()).use { zip ->
            var count = 0
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++count > MAX_ZIP_ENTRIES) throw IOException("Log ZIP has too many entries")
                val name = entry.name.replace('\\', '/')
                if (name.startsWith('/') || name.split('/').any { it == ".." }) throw IOException("Unsafe ZIP entry path")
                val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
                if (!entry.isDirectory && ext in textExtensions && collectedBytes < MAX_TEXT_BYTES) {
                    val allowance = minOf(MAX_ENTRY_TEXT_BYTES, MAX_TEXT_BYTES - collectedBytes)
                    val bytes = readEntryLimited(zip, allowance)
                    collectedBytes += bytes.size
                    val text = String(bytes, Charsets.UTF_8)
                    if (text.isNotBlank()) sections += name to text
                } else if (!entry.isDirectory) {
                    drain(zip, MAX_IGNORED_ENTRY_BYTES)
                }
                zip.closeEntry()
            }
        }
        if (sections.isEmpty()) throw IOException("No readable text logs were found in the ZIP")
        return sections.joinToString("\n\n") { (name, text) -> "===== $name =====\n$text" }
            .take(MAX_TEXT_BYTES.toInt())
    }

    private fun readEntryLimited(input: InputStream, maxBytes: Long): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) throw IOException("A text log entry exceeds the ${maxBytes / (1024 * 1024)} MB inspection allowance")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun readLimited(input: InputStream, maxBytes: Long): String =
        String(readEntryLimited(input, maxBytes), Charsets.UTF_8)

    private fun drain(input: InputStream, maxBytes: Long) {
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) throw IOException("A non-text ZIP entry exceeds the safe inspection limit")
        }
    }
}
