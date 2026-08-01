package com.mohnishraj.novaide.m9

import com.mohnishraj.novaide.webpreview.WebConsoleBuffer
import com.mohnishraj.novaide.webpreview.WebConsoleEntry
import com.mohnishraj.novaide.webpreview.WebPreviewEngine
import com.mohnishraj.novaide.webpreview.WebPreviewKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M9WebPreviewTest {
    @Test fun pathNormalizationRejectsTraversal() {
        assertNull(WebPreviewEngine.normalizePath("assets/../.env"))
        assertNull(WebPreviewEngine.normalizePath("%00/secret".replace("%00", "\u0000")))
        assertEquals("assets/app.js", WebPreviewEngine.normalizePath("/assets//./app.js"))
    }

    @Test fun activeHtmlEntryHasPriority() {
        val plan = WebPreviewEngine.plan(listOf("index.html", "docs/demo.html"), "docs/demo.html")
        assertEquals("docs/demo.html", plan.entryPath)
        assertEquals(WebPreviewKind.STATIC_SITE, plan.kind)
    }

    @Test fun generatedBuildOutputBeatsToolingSource() {
        val plan = WebPreviewEngine.plan(listOf("package.json", "vite.config.ts", "index.html", "dist/index.html"))
        assertEquals("dist/index.html", plan.entryPath)
        assertEquals(WebPreviewKind.BUILD_OUTPUT, plan.kind)
    }

    @Test fun unbuiltToolingProjectExplainsMissingRuntime() {
        val plan = WebPreviewEngine.plan(listOf("package.json", "src/App.tsx", "vite.config.ts"))
        assertFalse(plan.canRun)
        assertTrue(plan.warning.orEmpty().contains("build tool"))
    }

    @Test fun sensitiveWorkspaceFilesNeverServe() {
        assertTrue(WebPreviewEngine.isSensitive(".env"))
        assertTrue(WebPreviewEngine.isSensitive("config/release.keystore"))
        assertTrue(WebPreviewEngine.isSensitive(".git/config"))
        assertFalse(WebPreviewEngine.isSensitive("assets/config.json"))
    }

    @Test fun browserMimeTypesAreExplicit() {
        assertEquals("application/javascript", WebPreviewEngine.mimeType("src/app.mjs"))
        assertEquals("font/woff2", WebPreviewEngine.mimeType("fonts/ui.woff2"))
        assertEquals("text/html", WebPreviewEngine.mimeType("index.html"))
    }

    @Test fun diagnosticsInjectionIsIdempotent() {
        val source = "<html><body><h1>Hi</h1></body></html>"
        val once = WebPreviewEngine.injectDiagnostics(source)
        val twice = WebPreviewEngine.injectDiagnostics(once)
        assertTrue(once.contains("data-nova-preview-runtime"))
        assertEquals(once, twice)
    }

    @Test fun spaFallbackOnlyAcceptsSafeExtensionlessRoutes() {
        assertTrue(WebPreviewEngine.shouldSpaFallback("dashboard/settings"))
        assertFalse(WebPreviewEngine.shouldSpaFallback("assets/app.js"))
        assertFalse(WebPreviewEngine.shouldSpaFallback("../secret"))
    }

    @Test fun consoleBufferIsBoundedAndDeduplicated() {
        val buffer = WebConsoleBuffer(20)
        repeat(30) { buffer.add(WebConsoleEntry("log", "message-$it")) }
        buffer.add(WebConsoleEntry("error", "same"))
        buffer.add(WebConsoleEntry("error", "same"))
        val entries = buffer.snapshot()
        assertEquals(20, entries.size)
        assertEquals(1, entries.count { it.message == "same" })
        assertTrue(buffer.render().contains("ERROR"))
    }
}
