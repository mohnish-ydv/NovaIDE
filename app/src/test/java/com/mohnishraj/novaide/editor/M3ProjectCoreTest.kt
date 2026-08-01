package com.mohnishraj.novaide.editor

import com.mohnishraj.novaide.archive.ZipSafety
import com.mohnishraj.novaide.project.ProjectDetector
import com.mohnishraj.novaide.project.ProjectKind
import com.mohnishraj.novaide.templates.TemplateCatalog
import com.mohnishraj.novaide.workspace.search.WorkspaceSearchMatcher
import com.mohnishraj.novaide.workspace.search.WorkspaceSearchOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3ProjectCoreTest {
    @Test
    fun detectsAndroidProjectAheadOfGenericGradle() {
        val detection = ProjectDetector.detect(
            listOf("settings.gradle.kts", "app/build.gradle.kts", "app/src/main/AndroidManifest.xml")
        )
        assertEquals(ProjectKind.ANDROID, detection.kind)
        assertTrue(detection.confidence >= 90)
    }

    @Test
    fun detectsPhaserFromPackageHint() {
        val detection = ProjectDetector.detect(
            listOf("package.json", "src/game.js", "index.html"),
            mapOf("package.json" to "{\"dependencies\":{\"phaser\":\"^3.90.0\"}}")
        )
        assertEquals(ProjectKind.PHASER, detection.kind)
    }

    @Test
    fun zipSafetyRejectsTraversalAndAbsolutePaths() {
        assertTrue(ZipSafety.safeSegments("../secret.txt") == null)
        assertTrue(ZipSafety.safeSegments("/absolute/file.txt") == null)
        assertTrue(ZipSafety.safeSegments("C:/windows/file.txt") == null)
        assertTrue(ZipSafety.safeSegments("bad\nname.txt") == null)
        assertEquals(listOf("src", "main.kt"), ZipSafety.safeSegments("src/main.kt"))
        assertEquals("_", ZipSafety.exportSegment(".."))
        assertEquals("bad_name", ZipSafety.exportSegment("bad/name"))
    }

    @Test
    fun workspaceSearchSupportsRegexAndSkipsGeneratedFolders() {
        val result = WorkspaceSearchMatcher.findLines(
            "alpha\nBeta 123\ngamma 456",
            "[0-9]+",
            WorkspaceSearchOptions(regex = true)
        )
        assertTrue(result.error == null)
        assertEquals(2, result.matches.size)
        assertEquals(2, result.matches.first().line)
        assertTrue(WorkspaceSearchMatcher.shouldSkip("node_modules/pkg/index.js", false))
        assertTrue(!WorkspaceSearchMatcher.shouldSkip("src/index.js", false))
        assertTrue(WorkspaceSearchMatcher.compile("x".repeat(513), WorkspaceSearchOptions()).isFailure)
    }

    @Test
    fun templatesContainRealRunnableFiles() {
        assertEquals(4, TemplateCatalog.all.size)
        val web = TemplateCatalog.all.first { it.id == "web" }
        val html = web.files.first { it.path == "index.html" }.content
        assertTrue(html.contains("<!doctype html>"))
        assertTrue(html.contains("\n<html"))
        assertTrue(!html.contains("\\n<html"))
        val node = TemplateCatalog.all.first { it.id == "node" }
        assertNotNull(node.files.firstOrNull { it.path == "package.json" })
    }
}
