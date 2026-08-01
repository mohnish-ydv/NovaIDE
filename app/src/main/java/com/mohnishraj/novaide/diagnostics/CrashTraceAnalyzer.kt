package com.mohnishraj.novaide.diagnostics

import com.mohnishraj.novaide.ai.SecretRedactor
import java.security.MessageDigest

data class CrashFrame(
    val className: String,
    val methodName: String,
    val fileName: String?,
    val line: Int?,
    val projectPath: String?
)

data class CrashTraceReport(
    val kind: String,
    val exceptionType: String?,
    val message: String?,
    val rootCause: String,
    val frames: List<CrashFrame>,
    val suspectedPath: String?,
    val suspectedLine: Int?,
    val recommendations: List<String>,
    val fingerprint: String,
    val redactions: Int,
    val truncated: Boolean
)

object CrashTraceAnalyzer {
    const val MAX_LOG_CHARS = 2_000_000
    private val frameRegex = Regex("""^\s*at\s+([\w.$]+)\.([\w$<>]+)\(([^():]+)?(?::(\d+))?\)\s*$""")
    private val exceptionRegex = Regex("""(?:Caused by:\s*)?([\w.$]+(?:Exception|Error|Throwable))(?::\s*(.*))?""")

    fun analyze(rawLog: String, projectPaths: List<String>): CrashTraceReport {
        val clipped = rawLog.take(MAX_LOG_CHARS)
        val redacted = SecretRedactor.redact(clipped)
        val lines = redacted.text.lines()
        val exceptionLines = lines.mapNotNull { line ->
            exceptionRegex.find(line.trim())?.let { match ->
                match.groupValues[1] to match.groupValues.getOrNull(2)?.trim().orEmpty()
            }
        }
        val deepest = exceptionLines.lastOrNull()
        val frames = lines.mapNotNull { line ->
            val match = frameRegex.matchEntire(line) ?: return@mapNotNull null
            val fileName = match.groupValues[3].ifBlank { null }
            val lineNumber = match.groupValues[4].toIntOrNull()
            val className = match.groupValues[1]
            CrashFrame(
                className = className,
                methodName = match.groupValues[2],
                fileName = fileName,
                line = lineNumber,
                projectPath = resolveProjectPath(fileName, className, projectPaths)
            )
        }.take(160)
        val firstProject = frames.firstOrNull { it.projectPath != null }
        val exceptionType = deepest?.first ?: exceptionLines.firstOrNull()?.first
        val message = deepest?.second?.takeIf { it.isNotBlank() }
        val kind = detectKind(lines, exceptionType)
        val cause = buildString {
            append(exceptionType?.substringAfterLast('.') ?: kind)
            if (!message.isNullOrBlank()) append(": ${message.take(500)}")
        }
        val recommendations = recommendations(kind, exceptionType, message, lines)
        val fingerprintSource = listOfNotNull(exceptionType, message) + frames.take(5).map { "${it.className}.${it.methodName}:${it.line ?: 0}" }
        return CrashTraceReport(
            kind = kind,
            exceptionType = exceptionType,
            message = message,
            rootCause = cause,
            frames = frames,
            suspectedPath = firstProject?.projectPath,
            suspectedLine = firstProject?.line,
            recommendations = recommendations,
            fingerprint = sha256(fingerprintSource.joinToString("|" )).take(16),
            redactions = redacted.redactions,
            truncated = rawLog.length > MAX_LOG_CHARS
        )
    }

    private fun resolveProjectPath(fileName: String?, className: String, paths: List<String>): String? {
        if (fileName != null) {
            val matches = paths.filter { it.substringAfterLast('/').equals(fileName, ignoreCase = true) }
            if (matches.size == 1) return matches.first()
            if (matches.isNotEmpty()) {
                val packageHint = className.substringBeforeLast('.', "").replace('.', '/')
                return matches.maxByOrNull { commonSuffixScore(it.substringBeforeLast('/'), packageHint) }
            }
        }
        val simple = className.substringAfterLast('.').substringBefore('$')
        return paths.firstOrNull { path ->
            val base = path.substringAfterLast('/').substringBeforeLast('.')
            base.equals(simple, ignoreCase = true)
        }
    }

    private fun commonSuffixScore(path: String, hint: String): Int {
        val a = path.lowercase().split('/').reversed()
        val b = hint.lowercase().split('/').reversed()
        return a.zip(b).takeWhile { it.first == it.second }.size
    }

    private fun detectKind(lines: List<String>, exception: String?): String {
        val joined = lines.take(6000).joinToString("\n").lowercase()
        return when {
            "anr in" in joined || "input dispatching timed out" in joined -> "ANR"
            "fatal exception" in joined -> "Android crash"
            exception?.endsWith("OutOfMemoryError") == true -> "Out of memory"
            exception?.endsWith("StackOverflowError") == true -> "Stack overflow"
            "build failed" in joined || "compilation error" in joined -> "Build failure"
            exception != null -> "Runtime exception"
            else -> "Diagnostic log"
        }
    }

    private fun recommendations(kind: String, exception: String?, message: String?, lines: List<String>): List<String> {
        val value = (exception.orEmpty() + " " + message.orEmpty() + " " + lines.take(500).joinToString(" ")).lowercase()
        val result = linkedSetOf<String>()
        when {
            "nullpointer" in value || "lateinit property" in value -> result += "Trace the first project frame and validate lifecycle/nullability before dereferencing the value."
            "classcastexception" in value -> result += "Check the cast at the first project frame; use a safe cast or correct the inflated/view/model type."
            "securityexception" in value || "permission denial" in value -> result += "Declare the required permission and request runtime consent where Android requires it; also verify provider/exported access."
            "outofmemory" in value -> result += "Reduce retained bitmaps/buffers, stream large files, cap caches, and inspect the largest allocation path."
            "networkonmainthread" in value -> result += "Move network or blocking I/O off the main thread and return results through a lifecycle-safe callback."
            "transactiontoolarge" in value -> result += "Do not place large objects in Bundles/Intents; persist or stream them and pass a compact identifier."
            "stackoverflow" in value -> result += "Inspect recursive calls and cyclic callbacks; add a termination condition or iterative implementation."
            "anr" in kind.lowercase() -> result += "Inspect the main-thread stack, remove blocking I/O/locks, and split work into bounded background tasks."
            "manifest merger" in value -> result += "Open the merged-manifest error lines and resolve duplicate attributes with tools:replace/tools:node only where intentional."
            "could not resolve" in value -> result += "Verify repository declarations, dependency coordinates, network access, and avoid dynamic versions."
        }
        result += "Start from the earliest project-owned frame, not the final framework frame."
        result += "Reproduce with the same inputs and keep the full caused-by chain when comparing fixes."
        return result.take(5)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
