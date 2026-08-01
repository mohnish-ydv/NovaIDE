package com.mohnishraj.novaide.archive

object ZipSafety {
    private const val MAX_SEGMENT_LENGTH = 180

    fun safeSegments(rawName: String): List<String>? {
        if (rawName.isBlank() || rawName.indexOf('\u0000') >= 0) return null
        val normalized = rawName.replace('\\', '/').trim()
        if (normalized.startsWith('/') || Regex("^[A-Za-z]:/").containsMatchIn(normalized)) return null
        val segments = normalized.split('/').filter { it.isNotEmpty() }
        if (segments.isEmpty()) return null
        if (segments.any { segment ->
                segment == "." || segment == ".." || segment.length > MAX_SEGMENT_LENGTH || segment.any { it.isISOControl() }
            }) return null
        return segments
    }

    fun exportSegment(displayName: String): String {
        val cleaned = buildString {
            for (character in displayName) {
                append(if (character == '/' || character == '\\' || character.isISOControl()) '_' else character)
            }
        }.trim().take(MAX_SEGMENT_LENGTH)
        return when (cleaned) {
            "", ".", ".." -> "_"
            else -> cleaned
        }
    }

    fun archiveFolderName(fileName: String): String {
        val base = fileName.substringBeforeLast('.').trim()
            .replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .trim('.', ' ')
        return base.ifBlank { "Imported project" }.take(80)
    }
}
