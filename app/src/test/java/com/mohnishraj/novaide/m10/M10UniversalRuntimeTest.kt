package com.mohnishraj.novaide.m10

import com.mohnishraj.novaide.runtime.DocumentPreviewGenerator
import com.mohnishraj.novaide.runtime.PackageJsonReader
import com.mohnishraj.novaide.runtime.RuntimeAction
import com.mohnishraj.novaide.runtime.RuntimeKind
import com.mohnishraj.novaide.runtime.SharedWorkspacePathResolver
import com.mohnishraj.novaide.runtime.TermuxCommandPolicy
import com.mohnishraj.novaide.runtime.UniversalRuntimeEngine
import com.mohnishraj.novaide.webpreview.WebPreviewEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M10UniversalRuntimeTest {
    @Test fun viteProjectGetsInstallBuildAndDevCommands() {
        val pkg = """{"name":"demo","scripts":{"dev":"vite --port 4173","build":"vite build"},"devDependencies":{"vite":"1"}}"""
        val runtime = UniversalRuntimeEngine.detect(listOf("package.json", "vite.config.ts", "src/main.ts", "package-lock.json"), mapOf("package.json" to pkg))
        assertEquals(RuntimeKind.VITE, runtime.kind)
        assertEquals("npm", runtime.packageManager)
        assertEquals("npm ci", runtime.command(RuntimeAction.INSTALL)?.display())
        assertEquals(4173, runtime.command(RuntimeAction.DEVELOP)?.defaultPort)
        assertTrue(runtime.command(RuntimeAction.BUILD)?.display().orEmpty().contains("run build"))
    }

    @Test fun generatedAngularBrowserOutputIsDetected() {
        val pkg = """{"name":"hello-app","scripts":{"build":"ng build"},"dependencies":{"@angular/core":"20"}}"""
        val runtime = UniversalRuntimeEngine.detect(
            listOf("package.json", "angular.json", "dist/hello-app/browser/index.html"),
            mapOf("package.json" to pkg)
        )
        assertEquals(RuntimeKind.ANGULAR, runtime.kind)
        assertEquals("dist/hello-app/browser/index.html", runtime.detectedOutput)
    }

    @Test fun packageJsonRejectsDuplicateKeys() {
        val error = runCatching { PackageJsonReader.parse("""{"scripts":{},"scripts":{}}""") }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("Duplicate"))
    }

    @Test fun sharedStoragePathResolverIsConservative() {
        assertEquals("/storage/emulated/0/Download/Nova", SharedWorkspacePathResolver.resolve("com.android.externalstorage.documents", "primary:Download/Nova"))
        assertEquals("/storage/AB12-CD34/Code", SharedWorkspacePathResolver.resolve("com.android.externalstorage.documents", "AB12-CD34:Code"))
        assertNull(SharedWorkspacePathResolver.resolve("com.google.android.apps.docs.storage", "root:project"))
        assertNull(SharedWorkspacePathResolver.resolve("com.android.externalstorage.documents", "primary:Download/../secret"))
    }

    @Test fun termuxCommandPolicyQuotesArgumentsAndBlocksUnknownExecutables() {
        assertEquals("'a'\"'\"'b'", TermuxCommandPolicy.shellQuote("a'b"))
        val runtime = UniversalRuntimeEngine.detect(listOf("app.py"))
        assertEquals("python app.py", runtime.command(RuntimeAction.RUN)?.display())
        val error = runCatching {
            TermuxCommandPolicy.validate(com.mohnishraj.novaide.runtime.RuntimeCommand(RuntimeAction.RUN, "Bad", "sh", listOf("-c", "rm -rf /"), "bad"))
        }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("Unsupported"))
    }

    @Test fun markdownPreviewEscapesHtmlAndRendersStructure() {
        val html = DocumentPreviewGenerator.markdown("Readme", "# Hello\n\n**safe** <script>alert(1)</script>")
        assertTrue(html.contains("<h1>Hello</h1>"))
        assertTrue(html.contains("<strong>safe</strong>"))
        assertFalse(html.contains("<script>alert"))
        assertTrue(html.contains("&lt;script&gt;"))
    }

    @Test fun mermaidPreviewUsesStrictSecurityAndExternalHttpsModule() {
        val html = DocumentPreviewGenerator.mermaid("Flow", "graph TD; A-->B")
        assertTrue(html.contains("securityLevel: 'strict'"))
        assertTrue(html.contains("https://cdn.jsdelivr.net/"))
        assertTrue(html.contains("graph TD; A--&gt;B"))
    }

    @Test fun loopbackRuntimePolicyAllowsOnlyExplicitOrigin() {
        assertEquals("http://127.0.0.1:5173", WebPreviewEngine.runtimeOrigin("http://127.0.0.1:5173/app"))
        assertTrue(WebPreviewEngine.isAllowedRuntimeUrl("http://127.0.0.1:5173/assets/app.js", "http://127.0.0.1:5173"))
        assertFalse(WebPreviewEngine.isAllowedRuntimeUrl("http://127.0.0.1:3000/", "http://127.0.0.1:5173"))
        assertNull(WebPreviewEngine.runtimeOrigin("http://192.168.1.2:5173"))
        assertNull(WebPreviewEngine.runtimeOrigin("http://user:pass@localhost:5173"))
    }

    @Test fun nextSourceWithoutExportExplainsRuntimeNeed() {
        val pkg = """{"scripts":{"dev":"next dev","build":"next build"},"dependencies":{"next":"15","react":"19"}}"""
        val runtime = UniversalRuntimeEngine.detect(listOf("package.json", "next.config.ts", "app/page.tsx"), mapOf("package.json" to pkg))
        assertEquals(RuntimeKind.NEXT_JS, runtime.kind)
        assertNull(runtime.detectedOutput)
        assertTrue(runtime.warning.orEmpty().contains("static export"))
    }
    @Test
    fun viteSourceIndexIsNotMistakenForGeneratedOutput() {
        val project = UniversalRuntimeEngine.detect(
            listOf("package.json", "vite.config.ts", "index.html", "src/main.ts"),
            mapOf("package.json" to """{"scripts":{"build":"vite build"},"devDependencies":{"vite":"latest"}}""")
        )
        assertEquals(RuntimeKind.VITE, project.kind)
        assertNull(project.detectedOutput)
        assertTrue(project.warning != null)
    }

}
