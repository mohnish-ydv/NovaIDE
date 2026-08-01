package com.mohnishraj.novaide.ai

import java.io.IOException

object AiResponseParser {
    private const val MAX_PATCH_FILES = 20
    private const val MAX_PATCH_TOTAL_CHARS = 1_500_000

    fun extractCode(text: String): String? {
        val blocks = Regex("```(?:[A-Za-z0-9_+.-]+)?\\s*\\n([\\s\\S]*?)```", RegexOption.MULTILINE)
            .findAll(text)
            .map { it.groupValues[1].trimEnd() }
            .filter { it.isNotBlank() }
            .toList()
        return blocks.maxByOrNull { it.length }
    }

    @Throws(IOException::class)
    fun filePatches(text: String): List<NovaFilePatch> {
        val regex = Regex(":::nova-file\\s+path=\"([^\"]+)\"\\s*\\n([\\s\\S]*?)\\n:::end")
        val matches = regex.findAll(text).toList()
        if (matches.size > MAX_PATCH_FILES) throw IOException("AI patch contains too many files")
        var total = 0
        val seen = linkedSetOf<String>()
        return matches.map { match ->
            val path = normalizePath(match.groupValues[1])
            if (!seen.add(path.lowercase())) throw IOException("AI patch contains duplicate path: $path")
            val content = match.groupValues[2]
            total += content.length
            if (total > MAX_PATCH_TOTAL_CHARS) throw IOException("AI patch exceeds the mobile safety limit")
            NovaFilePatch(path, content)
        }
    }

    private fun normalizePath(value: String): String {
        val normalized = value.trim().replace('\\', '/').trim('/')
        require(normalized.isNotBlank()) { "AI patch path is blank" }
        require(!normalized.startsWith('/') && !Regex("^[A-Za-z]:").containsMatchIn(normalized)) { "AI patch path must be relative" }
        val parts = normalized.split('/')
        require(parts.none { it.isBlank() || it == "." || it == ".." || it.any { ch -> ch.code < 32 || ch.code == 127 } }) { "AI patch path is unsafe" }
        require(parts.size <= 40 && normalized.length <= 512) { "AI patch path is too deep or long" }
        return parts.joinToString("/")
    }
}
