package com.mohnishraj.novaide.ai

object SecretRedactor {
    private val tokenPatterns = listOf(
        Regex("(?i)\\b(github_pat_[A-Za-z0-9_]{20,}|gh[pousr]_[A-Za-z0-9]{20,})\\b"),
        Regex("(?i)\\b(glpat-[A-Za-z0-9_-]{12,})\\b"),
        Regex("(?i)\\b(sk-[A-Za-z0-9_-]{16,})\\b"),
        Regex("(?i)\\b(AIza[A-Za-z0-9_-]{20,})\\b"),
        Regex("(?i)(api[_-]?key|access[_-]?token|secret|password)\\s*[:=]\\s*[\"']([^\"'\\n]{6,})[\"']")
    )

    data class Result(val text: String, val redactions: Int)

    fun redact(source: String): Result {
        var output = source
        var count = 0
        tokenPatterns.forEach { regex ->
            output = regex.replace(output) { match ->
                count++
                if (match.groupValues.size >= 3) {
                    match.value.replace(match.groupValues[2], "[REDACTED]")
                } else "[REDACTED_SECRET]"
            }
        }
        return Result(output, count)
    }

    fun isSensitivePath(path: String): Boolean {
        val lower = path.replace('\\', '/').lowercase()
        val name = lower.substringAfterLast('/')
        if (name in setOf(".env", ".env.local", ".env.production", "local.properties", "gradle.properties", "google-services.json")) return true
        if (name.endsWith(".jks") || name.endsWith(".keystore") || name.endsWith(".p12") || name.endsWith(".pfx") || name.endsWith(".pem") || name.endsWith(".key")) return true
        if (lower.contains("/credentials/") || lower.contains("/secrets/") || lower.contains("/.git/")) return true
        return false
    }
}
