package com.mohnishraj.novaide.diagnostics

import java.security.MessageDigest

object DuplicateCodeAnalyzer {
    private const val MAX_GROUPS = 120
    private const val WINDOW_LINES = 8
    private const val STEP_LINES = 4
    private const val MIN_NORMALIZED_CHARS = 120
    private const val MAX_TEXT_CHARS = 700_000

    fun analyze(files: List<DiagnosticFile>): List<DuplicateGroup> {
        val groups = mutableListOf<DuplicateGroup>()
        val exact = linkedMapOf<String, MutableList<DuplicateOccurrence>>()
        val windows = linkedMapOf<String, MutableList<DuplicateOccurrence>>()
        files.asSequence().filter { it.text != null && it.text.length in 1..MAX_TEXT_CHARS }
            .forEach { file ->
                val source = file.text ?: return@forEach
                val normalizedWhole = normalizeText(source)
                if (normalizedWhole.length >= MIN_NORMALIZED_CHARS) {
                    exact.getOrPut(hash(normalizedWhole)) { mutableListOf() } +=
                        DuplicateOccurrence(file.path, 1, source.lineSequence().count().coerceAtLeast(1))
                }
                val lines = source.lines()
                var start = 0
                while (start + WINDOW_LINES <= lines.size) {
                    val normalized = normalizeText(lines.subList(start, start + WINDOW_LINES).joinToString("\n"))
                    if (normalized.length >= MIN_NORMALIZED_CHARS) {
                        windows.getOrPut(hash(normalized)) { mutableListOf() } +=
                            DuplicateOccurrence(file.path, start + 1, start + WINDOW_LINES)
                    }
                    start += STEP_LINES
                }
            }
        exact.values.filter { occurrences -> occurrences.map { it.path.lowercase() }.distinct().size > 1 }
            .take(MAX_GROUPS).forEach { occurrences ->
                groups += DuplicateGroup(hash(occurrences.joinToString { it.path }), occurrences.distinctBy { it.path.lowercase() },
                    normalizedLines = occurrences.maxOfOrNull { it.endLine } ?: 0, exactFile = true)
            }
        if (groups.size < MAX_GROUPS) {
            windows.entries.asSequence()
                .map { (fingerprint, occurrences) -> fingerprint to collapseOverlaps(occurrences) }
                .filter { (_, occurrences) -> occurrences.map { it.path.lowercase() }.distinct().size > 1 }
                .sortedByDescending { it.second.size }
                .take(MAX_GROUPS - groups.size)
                .forEach { (fingerprint, occurrences) ->
                    groups += DuplicateGroup(fingerprint.take(16), occurrences, WINDOW_LINES, exactFile = false)
                }
        }
        return groups
    }

    private fun collapseOverlaps(values: List<DuplicateOccurrence>): List<DuplicateOccurrence> {
        val result = mutableListOf<DuplicateOccurrence>()
        values.sortedWith(compareBy<DuplicateOccurrence> { it.path.lowercase() }.thenBy { it.startLine }).forEach { item ->
            val previous = result.lastOrNull()
            if (previous != null && previous.path.equals(item.path, true) && item.startLine <= previous.endLine) return@forEach
            result += item
        }
        return result
    }

    private fun normalizeText(source: String): String = source.lineSequence()
        .map { it.trim() }
        .filter { line -> line.isNotBlank() && !line.startsWith("//") && !line.startsWith("#") && !line.startsWith("*") }
        .joinToString("\n") { it.replace(Regex("\\s+"), " ") }

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
