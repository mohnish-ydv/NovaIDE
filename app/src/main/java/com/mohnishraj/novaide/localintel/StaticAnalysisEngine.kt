package com.mohnishraj.novaide.localintel

import com.mohnishraj.novaide.ai.SecretRedactor
import java.security.MessageDigest

enum class StaticSeverity { HIGH, MEDIUM, LOW, INFO }

data class StaticFile(val path: String, val size: Long, val text: String?)
data class StaticFinding(val severity: StaticSeverity, val title: String, val path: String?, val details: String)
data class StaticAnalysisReport(val findings: List<StaticFinding>, val filesScanned: Int, val textFilesRead: Int)

object StaticAnalysisEngine {
    private const val MAX_FINDINGS = 500

    fun analyze(files: List<StaticFile>): StaticAnalysisReport {
        val findings = mutableListOf<StaticFinding>()
        val names = linkedMapOf<String, MutableList<String>>()
        val hashes = linkedMapOf<String, MutableList<String>>()
        var textCount = 0
        files.forEach { file ->
            if (findings.size >= MAX_FINDINGS) return@forEach
            val normalized = file.path.replace('\\', '/')
            val name = normalized.substringAfterLast('/').lowercase()
            names.getOrPut(name) { mutableListOf() } += normalized
            if (file.size > 5L * 1024L * 1024L) {
                findings += StaticFinding(StaticSeverity.MEDIUM, "Large project file", normalized, "${file.size / (1024 * 1024)} MB can slow indexing, commits and builds.")
            }
            if (SecretRedactor.isSensitivePath(normalized)) {
                findings += StaticFinding(StaticSeverity.HIGH, "Sensitive file in workspace", normalized, "Keep credentials and signing material out of source control and AI context.")
            }
            val text = file.text ?: return@forEach
            textCount++
            if (text.length <= 512_000) {
                val hash = sha256(text)
                hashes.getOrPut(hash) { mutableListOf() } += normalized
            }
            if (Regex("(?m)^<<<<<<< |^=======\\s*$|^>>>>>>> ").containsMatchIn(text)) {
                findings += StaticFinding(StaticSeverity.HIGH, "Unresolved merge conflict", normalized, "Conflict markers remain in the file.")
            }
            val redacted = SecretRedactor.redact(text)
            if (redacted.redactions > 0) {
                findings += StaticFinding(StaticSeverity.HIGH, "Possible embedded secret", normalized, "Detected ${redacted.redactions} credential-like value(s). Values are intentionally hidden.")
            }
            val workMarkers = Regex("\\b(TODO|FIXME|HACK)\\b", RegexOption.IGNORE_CASE).findAll(text).count()
            if (workMarkers > 0) findings += StaticFinding(StaticSeverity.INFO, "Unresolved work markers", normalized, "$workMarkers TODO/FIXME/HACK marker(s).")
            val debugCalls = Regex("(?m)\\b(console\\.(log|debug)|System\\.out\\.println|println\\(|print\\()")
                .findAll(text).count()
            if (debugCalls > 0) findings += StaticFinding(StaticSeverity.LOW, "Debug output", normalized, "$debugCalls potential debug-output call(s).")
        }
        names.filter { (name, paths) -> name.isNotBlank() && paths.size > 1 && name !in setOf("index.kt", "index.ts", "index.js", "readme.md", "androidmanifest.xml") }
            .values.take(80).forEach { paths ->
                findings += StaticFinding(StaticSeverity.INFO, "Duplicate filename", null, paths.joinToString())
            }
        hashes.values.filter { it.size > 1 }.take(80).forEach { paths ->
            findings += StaticFinding(StaticSeverity.MEDIUM, "Duplicate file content", null, paths.joinToString())
        }
        return StaticAnalysisReport(
            findings = findings.take(MAX_FINDINGS).sortedBy { it.severity.ordinal },
            filesScanned = files.size,
            textFilesRead = textCount
        )
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
