package com.mohnishraj.novaide.diagnostics

import com.mohnishraj.novaide.ai.SecretRedactor

object SecurityAnalyzer {
    private const val MAX_FINDINGS = 300

    fun analyze(files: List<DiagnosticFile>): List<DiagnosticFinding> {
        val findings = mutableListOf<DiagnosticFinding>()
        files.forEach { file ->
            if (findings.size >= MAX_FINDINGS) return@forEach
            if (SecretRedactor.isSensitivePath(file.path)) {
                findings += issue(DiagnosticSeverity.HIGH, "Sensitive file present", "This path may contain credentials, signing material, or private configuration.",
                    "Keep it outside source control and AI context; rotate exposed credentials.", file.path, null, 98)
            }
            val source = file.text ?: return@forEach
            val redactions = SecretRedactor.redact(source).redactions
            if (redactions > 0) findings += issue(DiagnosticSeverity.CRITICAL, "Possible embedded secret", "$redactions credential-like value(s) detected; values are hidden.",
                "Remove the secret from source/history, rotate it, and load it from encrypted/local configuration.", file.path, 1, 92)
            val rules = listOf(
                Rule(Regex("""http://(?!localhost\b|127\.0\.0\.1\b|10\.0\.2\.2\b)[^\s\"']+"""), DiagnosticSeverity.HIGH, "Cleartext network endpoint", "Use HTTPS with certificate validation or explicitly document a trusted local-only exception.", 92),
                Rule(Regex("""\b(MessageDigest\.getInstance\s*\(\s*\"(?:MD5|SHA-?1)\"|DigestUtils\.(?:md5|sha1))""", RegexOption.IGNORE_CASE), DiagnosticSeverity.HIGH, "Weak cryptographic hash", "Use SHA-256+ for integrity or a password KDF such as Argon2/scrypt/PBKDF2 for passwords.", 96),
                Rule(Regex("""AES/ECB""", RegexOption.IGNORE_CASE), DiagnosticSeverity.HIGH, "Insecure AES mode", "Use an authenticated mode such as AES-GCM with a fresh random nonce.", 98),
                Rule(Regex("""\b(Random|Math\.random)\s*\("""), DiagnosticSeverity.MEDIUM, "Non-cryptographic randomness", "Use SecureRandom for tokens, reset codes, keys, or security decisions.", 72),
                Rule(Regex("""setJavaScriptEnabled\s*\(\s*true\s*\)"""), DiagnosticSeverity.MEDIUM, "WebView JavaScript enabled", "Disable JavaScript unless required; restrict navigation and avoid loading untrusted content.", 88),
                Rule(Regex("""addJavascriptInterface\s*\("""), DiagnosticSeverity.HIGH, "WebView JavaScript bridge", "Expose the minimum annotated surface and never combine the bridge with untrusted remote content.", 95),
                Rule(Regex("""HostnameVerifier\s*\{[^}]*true|checkServerTrusted\s*\([^)]*\)\s*\{\s*\}""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), DiagnosticSeverity.CRITICAL, "TLS verification bypass", "Remove trust-all certificate/hostname logic and use platform validation or certificate pinning with rotation.", 98),
                Rule(Regex("""\b(eval|exec)\s*\("""), DiagnosticSeverity.HIGH, "Dynamic code execution", "Avoid eval/exec for untrusted input; use a parser or allow-listed command model.", 78),
                Rule(Regex("""Access-Control-Allow-Origin\s*[:=]\s*[\"']\*[\"']""", RegexOption.IGNORE_CASE), DiagnosticSeverity.MEDIUM, "Wildcard CORS", "Restrict allowed origins and methods; never combine wildcard origins with credentials.", 94),
                Rule(Regex("""android:usesCleartextTraffic\s*=\s*\"true\""""), DiagnosticSeverity.HIGH, "Android cleartext traffic enabled", "Disable cleartext globally or use a narrow Network Security Config for a justified host.", 99),
                Rule(Regex("""android:exported\s*=\s*\"true\""""), DiagnosticSeverity.MEDIUM, "Exported Android component", "Verify every exported component needs external access and protect sensitive entry points with permissions/validation.", 76),
                Rule(Regex("""\b(?:implementation|api)\s*\(\s*[\"'][^\"']+:(?:\+|latest\.release|latest\.integration)[\"']""", RegexOption.IGNORE_CASE), DiagnosticSeverity.MEDIUM, "Unpinned dependency version", "Pin an audited version and use automated dependency updates with review.", 96)
            )
            rules.forEach { rule ->
                rule.pattern.findAll(source).take(20).forEach { match ->
                    findings += issue(rule.severity, rule.title, "Matched security-sensitive construct: ${match.value.take(160)}",
                        rule.recommendation, file.path, lineOf(source, match.range.first), rule.confidence)
                }
            }
        }
        return findings.take(MAX_FINDINGS)
    }

    private data class Rule(val pattern: Regex, val severity: DiagnosticSeverity, val title: String, val recommendation: String, val confidence: Int)
    private fun issue(severity: DiagnosticSeverity, title: String, details: String, recommendation: String, path: String, line: Int?, confidence: Int) =
        DiagnosticFinding(DiagnosticCategory.SECURITY, severity, title, details, recommendation, path, line, null, confidence)
    private fun lineOf(source: String, offset: Int): Int = source.take(offset.coerceAtLeast(0)).count { it == '\n' } + 1
}
