package com.mohnishraj.novaide.diagnostics

object PerformanceAnalyzer {
    private const val MAX_FINDINGS = 250

    fun analyze(files: List<DiagnosticFile>): List<DiagnosticFinding> {
        val findings = mutableListOf<DiagnosticFinding>()
        files.forEach { file ->
            if (findings.size >= MAX_FINDINGS) return@forEach
            val source = file.text
            if (source == null) {
                if (file.sizeBytes > 2L * 1024L * 1024L && file.path.substringAfterLast('.', "").lowercase() in setOf("png", "jpg", "jpeg", "webp", "gif", "svg")) {
                    findings += finding(DiagnosticSeverity.MEDIUM, "Large image asset", "${file.sizeBytes / 1024} KB asset increases package size and memory pressure.",
                        "Resize/compress the asset and use density-specific or vector resources where appropriate.", file.path, null, 95)
                }
                return@forEach
            }
            val lines = source.lines()
            if (lines.size > 1_500) findings += finding(DiagnosticSeverity.LOW, "Very long source file", "${lines.size} lines make navigation, review, and incremental analysis harder.",
                "Split cohesive responsibilities into smaller modules without creating circular dependencies.", file.path, 1, 95)
            if (source.length > 600_000) findings += finding(DiagnosticSeverity.MEDIUM, "Large text source", "${source.length} characters can slow mobile editing and syntax analysis.",
                "Split generated data or large constants into streamed resources and exclude generated output from editing.", file.path, 1, 96)
            longFunctions(file, source).forEach { findings += it }
            deepNesting(file, lines).forEach { findings += it }
            patternFindings(file, source).forEach { findings += it }
        }
        return findings.take(MAX_FINDINGS)
    }

    private fun longFunctions(file: DiagnosticFile, source: String): List<DiagnosticFinding> {
        val starts = Regex("""(?m)^\s*(?:public\s+|private\s+|protected\s+|internal\s+|static\s+|suspend\s+|async\s+)*(?:fun|def|function)\s+([A-Za-z_][\w]*)[^\n{]*\{?""")
            .findAll(source).toList()
        return starts.mapNotNull { match ->
            val startLine = lineOf(source, match.range.first)
            val lineEnd = source.indexOf('\n', match.range.first).let { if (it < 0) source.length else it }
            val openBrace = source.indexOf('{', match.range.first).takeIf { it in match.range.first..lineEnd }
                ?: return@mapNotNull null
            var depth = 0
            var end = openBrace
            var index = openBrace
            while (index < source.length) {
                when (source[index]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) { end = index; break }
                    }
                }
                index++
            }
            val span = source.substring(match.range.first, end.coerceAtLeast(openBrace)).count { it == '\n' } + 1
            if (span > 120) finding(DiagnosticSeverity.MEDIUM, "Long function: ${match.groupValues[1]}", "$span lines increase cognitive load and make hot paths harder to optimize.",
                "Extract cohesive operations and preserve behavior with focused tests.", file.path, startLine, 82) else null
        }.take(40)
    }

    private fun deepNesting(file: DiagnosticFile, lines: List<String>): List<DiagnosticFinding> {
        var depth = 0
        val results = mutableListOf<DiagnosticFinding>()
        lines.forEachIndexed { index, line ->
            val withoutStrings = line.replace(Regex("\"(?:\\\\.|[^\"])*\"|'(?:\\\\.|[^'])*'"), "")
            depth += withoutStrings.count { it == '{' } - withoutStrings.count { it == '}' }
            if (depth > 7 && results.none { it.line == index + 1 }) {
                results += finding(DiagnosticSeverity.LOW, "Deep nesting", "Nesting depth reached $depth, which often hides expensive or hard-to-test branches.",
                    "Use guard clauses, extracted functions, or data-driven dispatch to flatten the path.", file.path, index + 1, 75)
            }
            depth = depth.coerceAtLeast(0)
        }
        return results.take(12)
    }

    private fun patternFindings(file: DiagnosticFile, source: String): List<DiagnosticFinding> {
        val patterns = listOf(
            Triple(Regex("""\b(Thread\.sleep|runBlocking)\s*\("""), "Blocking call", "Move blocking work away from UI/event threads and use cancellation-aware asynchronous APIs."),
            Triple(Regex("""\b(readBytes|readText|writeBytes|writeText)\s*\("""), "Whole-file I/O", "Stream or bound large file operations, especially on mobile and UI paths."),
            Triple(Regex("""(?s)\b(for|while)\b[^{}]*\{.{0,800}\b(for|while)\b"""), "Nested loop hotspot", "Check input bounds and replace repeated scans with indexing, hashing, or batching where possible."),
            Triple(Regex("""(?s)\b(for|while)\b[^{}]*\{.{0,500}\+="""), "Repeated concatenation in loop", "Use a StringBuilder or batched collection to avoid repeated allocations."),
            Triple(Regex("""\bnotifyDataSetChanged\s*\("""), "Full list refresh", "Use targeted updates/diffing for large lists instead of invalidating every row."),
            Triple(Regex("""\bBitmapFactory\.decode"""), "Unbounded bitmap decode", "Decode with bounds/downsampling and release references when the image is no longer visible."),
            Triple(Regex("""\bGlobalScope\b"""), "Unstructured coroutine scope", "Use lifecycle- or owner-bound structured concurrency to prevent leaks and orphan work.")
        )
        return patterns.flatMap { (regex, title, recommendation) ->
            regex.findAll(source).take(12).map { match ->
                finding(DiagnosticSeverity.MEDIUM, title, "Potential performance risk detected: ${match.value.take(120)}", recommendation,
                    file.path, lineOf(source, match.range.first), 70)
            }.toList()
        }
    }

    private fun finding(severity: DiagnosticSeverity, title: String, details: String, recommendation: String, path: String, line: Int?, confidence: Int) =
        DiagnosticFinding(DiagnosticCategory.PERFORMANCE, severity, title, details, recommendation, path, line, null, confidence)

    private fun lineOf(source: String, offset: Int): Int = source.take(offset.coerceAtLeast(0)).count { it == '\n' } + 1
}
