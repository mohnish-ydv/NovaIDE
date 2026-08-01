package com.mohnishraj.novaide.androidsuite

import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipInputStream

object ApkInspector {
    private const val MAX_ENTRIES = 80_000
    private const val MAX_ENTRY_BYTES = 300L * 1024L * 1024L
    private const val MAX_TOTAL_BYTES = 2L * 1024L * 1024L * 1024L

    fun inspect(fileName: String, input: InputStream): ApkReport {
        var entries = 0
        var compressed = 0L
        var uncompressed = 0L
        var dex = 0
        var nativeLibraries = 0
        var resourceEntries = 0
        var assetEntries = 0
        var manifest = false
        var resourcesTable = false
        var v1 = false
        var signingBlockHint = false
        val abis = linkedSetOf<String>()
        val largest = mutableListOf<Pair<String, Long>>()
        val warnings = mutableListOf<String>()
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++entries > MAX_ENTRIES) throw IOException("APK contains too many entries")
                val name = entry.name.replace('\\', '/')
                if (name.startsWith('/') || name.split('/').any { it == ".." }) throw IOException("Unsafe APK entry path: $name")
                var size = 0L
                val buffer = ByteArray(32 * 1024)
                while (!entry.isDirectory) {
                    val read = zip.read(buffer)
                    if (read < 0) break
                    size += read
                    uncompressed += read
                    if (size > MAX_ENTRY_BYTES || uncompressed > MAX_TOTAL_BYTES) throw IOException("APK exceeds mobile inspection limits")
                }
                compressed += entry.compressedSize.coerceAtLeast(0L)
                if (size > 0) {
                    largest += name to size
                    if (largest.size > 80) {
                        largest.sortByDescending { it.second }
                        while (largest.size > 30) largest.removeAt(largest.lastIndex)
                    }
                }
                val lower = name.lowercase(Locale.US)
                when {
                    Regex("^classes(?:\\d+)?\\.dex$").matches(lower) -> dex++
                    lower == "androidmanifest.xml" -> manifest = true
                    lower == "resources.arsc" -> resourcesTable = true
                    lower.startsWith("res/") -> resourceEntries++
                    lower.startsWith("assets/") -> assetEntries++
                    lower.startsWith("lib/") && lower.endsWith(".so") -> {
                        nativeLibraries++
                        lower.split('/').getOrNull(1)?.let(abis::add)
                    }
                    lower.startsWith("meta-inf/") && (lower.endsWith(".rsa") || lower.endsWith(".dsa") || lower.endsWith(".ec") || lower.endsWith(".sf")) -> v1 = true
                    lower.contains("meta-inf/com/android/build/gradle/app-metadata.properties") || lower.contains("stamp-cert-sha256") -> signingBlockHint = true
                }
                zip.closeEntry()
            }
        }
        if (!manifest) warnings += "AndroidManifest.xml is missing; this may not be a valid APK."
        if (dex == 0) warnings += "No classes.dex file was found. This may be a resource-only or invalid package."
        if (!v1) warnings += "No JAR (v1) signature files were found. The APK may still use v2/v3/v4 signing, which is stored outside ZIP entries."
        if (abis.size == 1) warnings += "Only one native ABI is packaged (${abis.first()}); verify device compatibility."
        if (uncompressed > 200L * 1024L * 1024L) warnings += "The APK expands beyond 200 MB and may have install or distribution constraints."
        return ApkReport(fileName, entries, compressed, uncompressed, dex, abis.sorted(), nativeLibraries, resourceEntries, assetEntries, manifest, resourcesTable, v1, signingBlockHint, largest.sortedByDescending { it.second }.take(20), warnings)
    }
}
