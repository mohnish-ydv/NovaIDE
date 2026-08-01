package com.mohnishraj.novaide.m7

import com.mohnishraj.novaide.diagnostics.CrashTraceAnalyzer
import com.mohnishraj.novaide.diagnostics.DeadCodeAnalyzer
import com.mohnishraj.novaide.diagnostics.DependencyGraphAnalyzer
import com.mohnishraj.novaide.diagnostics.DiagnosticCategory
import com.mohnishraj.novaide.diagnostics.DiagnosticFile
import com.mohnishraj.novaide.diagnostics.DiagnosticSeverity
import com.mohnishraj.novaide.diagnostics.DuplicateCodeAnalyzer
import com.mohnishraj.novaide.diagnostics.PerformanceAnalyzer
import com.mohnishraj.novaide.diagnostics.ProjectAuditEngine
import com.mohnishraj.novaide.diagnostics.SecurityAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M7DiagnosticsTest {
    @Test fun crashTraceResolvesDeepCauseAndProjectFrame() {
        val log = """
            FATAL EXCEPTION: main
            java.lang.RuntimeException: wrapper
                at android.app.ActivityThread.main(ActivityThread.java:1)
            Caused by: java.lang.ClassCastException: bad view
                at demo.ui.MainActivity.render(MainActivity.kt:42)
        """.trimIndent()
        val report = CrashTraceAnalyzer.analyze(log, listOf("app/src/main/java/demo/ui/MainActivity.kt"))
        assertEquals("java.lang.ClassCastException", report.exceptionType)
        assertEquals("app/src/main/java/demo/ui/MainActivity.kt", report.suspectedPath)
        assertEquals(42, report.suspectedLine)
        assertTrue(report.recommendations.any { it.contains("cast", ignoreCase = true) })
        assertEquals(16, report.fingerprint.length)
    }

    @Test fun crashTraceRedactsSecrets() {
        val report = CrashTraceAnalyzer.analyze("api_key='super-secret-value'\njava.lang.IllegalStateException: failed", emptyList())
        assertTrue(report.redactions > 0)
        assertFalse(report.rootCause.contains("super-secret-value"))
    }

    @Test fun duplicateAnalyzerFindsExactAndRepeatedBlocks() {
        val block = (1..12).joinToString("\n") { "val item$it = service.load($it)" }
        val groups = DuplicateCodeAnalyzer.analyze(listOf(
            DiagnosticFile("src/A.kt", block.length.toLong(), block),
            DiagnosticFile("src/B.kt", block.length.toLong(), block),
            DiagnosticFile("src/C.kt", (block + "\nval extra = 1").length.toLong(), block + "\nval extra = 1")
        ))
        assertTrue(groups.any { it.exactFile && it.occurrences.size == 2 })
        assertTrue(groups.any { !it.exactFile && it.occurrences.map { occurrence -> occurrence.path }.distinct().size >= 2 })
    }

    @Test fun deadCodeFindsUnusedPrivateSymbolAndImport() {
        val source = """
            import sample.Unused
            private fun abandonedFeature() = 1
            fun live() = 2
        """.trimIndent()
        val findings = DeadCodeAnalyzer.analyze(listOf(DiagnosticFile("src/Demo.kt", source.length.toLong(), source)))
        assertTrue(findings.any { it.title.contains("abandonedFeature") })
        assertTrue(findings.any { it.title == "Unused import: Unused" })
        assertTrue(findings.all { it.category == DiagnosticCategory.DEAD_CODE })
    }

    @Test fun dependencyAnalyzerMapsImportsAndCycles() {
        val files = listOf(
            DiagnosticFile("src/demo/A.kt", 1, "package demo\nimport demo.B\nclass A"),
            DiagnosticFile("src/demo/B.kt", 1, "package demo\nimport demo.A\nclass B"),
            DiagnosticFile("src/demo/Unused.kt", 1, "package demo\nclass Unused")
        )
        val report = DependencyGraphAnalyzer.analyze(files)
        assertEquals(2, report.edges.size)
        assertTrue(report.cycles.any { it.toSet() == setOf("src/demo/A.kt", "src/demo/B.kt") })
        assertTrue("src/demo/Unused.kt" in report.orphanSources)
        assertTrue(DependencyGraphAnalyzer.render(report).contains("CYCLES"))
    }

    @Test fun performanceAnalyzerFindsBlockingAndLargeAsset() {
        val source = "fun load() { Thread.sleep(100); val text = file.readText() }"
        val findings = PerformanceAnalyzer.analyze(listOf(
            DiagnosticFile("src/Loader.kt", source.length.toLong(), source),
            DiagnosticFile("app/src/main/res/drawable/hero.png", 3L * 1024L * 1024L, null)
        ))
        assertTrue(findings.any { it.title == "Blocking call" })
        assertTrue(findings.any { it.title == "Whole-file I/O" })
        assertTrue(findings.any { it.title == "Large image asset" })
    }

    @Test fun securityAnalyzerFindsCleartextAndWeakCrypto() {
        val source = """
            val endpoint = "http://example.com/api"
            MessageDigest.getInstance("MD5")
            web.settings.setJavaScriptEnabled(true)
        """.trimIndent()
        val findings = SecurityAnalyzer.analyze(listOf(DiagnosticFile("src/Net.kt", source.length.toLong(), source)))
        assertTrue(findings.any { it.title == "Cleartext network endpoint" })
        assertTrue(findings.any { it.title == "Weak cryptographic hash" && it.severity == DiagnosticSeverity.HIGH })
        assertTrue(findings.any { it.title == "WebView JavaScript enabled" })
    }

    @Test fun projectAuditProducesScoreAndCategoryReports() {
        val source = "private fun unusedHelper() = 1\nval url = \"http://example.com\""
        val report = ProjectAuditEngine.analyze(listOf(DiagnosticFile("src/Test.kt", source.length.toLong(), source)))
        assertTrue(report.qualityScore in 0..99)
        assertTrue(report.findings.any { it.category == DiagnosticCategory.SECURITY })
        assertTrue(report.findings.any { it.category == DiagnosticCategory.DEAD_CODE })
        assertTrue(ProjectAuditEngine.renderFindings(report).contains("PROJECT HEALTH"))
        assertNotNull(report.dependencyGraph)
    }

    @Test fun auditMarksTruncatedInput() {
        val report = ProjectAuditEngine.analyze(emptyList(), truncated = true)
        assertTrue(report.truncated)
        assertEquals(100, report.qualityScore)
    }
}
