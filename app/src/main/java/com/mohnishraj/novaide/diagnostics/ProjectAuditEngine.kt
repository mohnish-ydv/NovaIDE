package com.mohnishraj.novaide.diagnostics

object ProjectAuditEngine {
    const val MAX_FILES = 8_000

    fun analyze(files: List<DiagnosticFile>, truncated: Boolean = false): ProjectAuditReport {
        val bounded = files.take(MAX_FILES)
        val duplicates = DuplicateCodeAnalyzer.analyze(bounded)
        val graph = DependencyGraphAnalyzer.analyze(bounded)
        val findings = mutableListOf<DiagnosticFinding>()
        findings += DeadCodeAnalyzer.analyze(bounded)
        findings += PerformanceAnalyzer.analyze(bounded)
        findings += SecurityAnalyzer.analyze(bounded)
        duplicates.take(80).forEach { group ->
            findings += DiagnosticFinding(
                DiagnosticCategory.DUPLICATION,
                if (group.exactFile) DiagnosticSeverity.MEDIUM else DiagnosticSeverity.LOW,
                if (group.exactFile) "Duplicate file content" else "Repeated code block",
                group.occurrences.joinToString { "${it.path}:${it.startLine}-${it.endLine}" },
                "Consolidate only when the code represents the same responsibility; preserve intentional platform-specific copies.",
                group.occurrences.firstOrNull()?.path,
                group.occurrences.firstOrNull()?.startLine,
                confidence = if (group.exactFile) 99 else 82
            )
        }
        graph.cycles.take(50).forEach { cycle ->
            findings += DiagnosticFinding(
                DiagnosticCategory.DEPENDENCY, DiagnosticSeverity.MEDIUM, "Dependency cycle",
                cycle.joinToString(" → "),
                "Move shared contracts downward, introduce an interface, or split the cycle before it grows.",
                cycle.firstOrNull(), null, confidence = 88
            )
        }
        graph.orphanSources.take(60).forEach { path ->
            findings += DiagnosticFinding(
                DiagnosticCategory.DEPENDENCY, DiagnosticSeverity.INFO, "Disconnected source file",
                "No resolvable project import edge enters or leaves this file.",
                "Confirm whether it is an entry point, reflection target, generated file, or obsolete source.", path, null, confidence = 58
            )
        }
        val ordered = findings.distinctBy { listOf(it.category, it.title, it.path, it.line, it.details).joinToString("|") }
            .sortedWith(compareBy<DiagnosticFinding> { it.severity.ordinal }.thenByDescending { it.confidence }).take(600)
        return ProjectAuditReport(
            filesScanned = bounded.size,
            textFilesRead = bounded.count { it.text != null },
            findings = ordered,
            duplicateGroups = duplicates,
            dependencyGraph = graph,
            qualityScore = score(ordered),
            truncated = truncated || files.size > MAX_FILES || graph.truncated
        )
    }

    fun renderFindings(report: ProjectAuditReport, category: DiagnosticCategory? = null): String = buildString {
        append(report.summary()).append("\n\n")
        val selected = report.findings.filter { category == null || it.category == category }
        if (selected.isEmpty()) append("No findings in this category.\n")
        selected.forEachIndexed { index, finding ->
            append("${index + 1}. [${finding.severity}] ${finding.title}\n")
            finding.path?.let { append("   $it${finding.line?.let { line -> ":$line" } ?: ""}\n") }
            append("   ${finding.details}\n")
            append("   Fix: ${finding.recommendation}\n")
            append("   Confidence: ${finding.confidence}%\n\n")
        }
    }

    private fun score(findings: List<DiagnosticFinding>): Int {
        val penalty = findings.fold(0) { total, finding -> total + when (finding.severity) {
            DiagnosticSeverity.CRITICAL -> 18
            DiagnosticSeverity.HIGH -> 9
            DiagnosticSeverity.MEDIUM -> 4
            DiagnosticSeverity.LOW -> 1
            DiagnosticSeverity.INFO -> 0
        } }
        return (100 - penalty.coerceAtMost(100)).coerceIn(0, 100)
    }
}
