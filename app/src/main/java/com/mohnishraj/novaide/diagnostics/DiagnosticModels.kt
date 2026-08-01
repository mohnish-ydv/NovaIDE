package com.mohnishraj.novaide.diagnostics

enum class DiagnosticSeverity { CRITICAL, HIGH, MEDIUM, LOW, INFO }
enum class DiagnosticCategory { CRASH, DUPLICATION, DEAD_CODE, DEPENDENCY, PERFORMANCE, SECURITY }

data class DiagnosticFile(
    val path: String,
    val sizeBytes: Long,
    val text: String?
)

data class DiagnosticFinding(
    val category: DiagnosticCategory,
    val severity: DiagnosticSeverity,
    val title: String,
    val details: String,
    val recommendation: String,
    val path: String? = null,
    val line: Int? = null,
    val evidence: String? = null,
    val confidence: Int = 80
)

data class DuplicateOccurrence(val path: String, val startLine: Int, val endLine: Int)
data class DuplicateGroup(
    val fingerprint: String,
    val occurrences: List<DuplicateOccurrence>,
    val normalizedLines: Int,
    val exactFile: Boolean
)

data class DependencyEdge(val from: String, val to: String, val kind: String)
data class DependencyGraphReport(
    val nodes: Set<String>,
    val edges: List<DependencyEdge>,
    val cycles: List<List<String>>,
    val hubs: List<Pair<String, Int>>,
    val orphanSources: List<String>,
    val truncated: Boolean
)

data class ProjectAuditReport(
    val filesScanned: Int,
    val textFilesRead: Int,
    val findings: List<DiagnosticFinding>,
    val duplicateGroups: List<DuplicateGroup>,
    val dependencyGraph: DependencyGraphReport,
    val qualityScore: Int,
    val truncated: Boolean
) {
    fun summary(): String = buildString {
        append("PROJECT HEALTH • $qualityScore/100\n")
        append("Files: $filesScanned • Text analyzed: $textFilesRead")
        if (truncated) append(" • limits reached")
        append("\n")
        val counts = DiagnosticSeverity.entries.associateWith { severity -> findings.count { it.severity == severity } }
        append("Critical ${counts[DiagnosticSeverity.CRITICAL]} • High ${counts[DiagnosticSeverity.HIGH]} • ")
        append("Medium ${counts[DiagnosticSeverity.MEDIUM]} • Low ${counts[DiagnosticSeverity.LOW]} • Info ${counts[DiagnosticSeverity.INFO]}\n")
        append("Duplicate groups: ${duplicateGroups.size} • Dependency cycles: ${dependencyGraph.cycles.size}")
    }
}
