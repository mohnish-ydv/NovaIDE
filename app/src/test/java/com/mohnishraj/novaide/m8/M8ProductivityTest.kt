package com.mohnishraj.novaide.m8

import com.mohnishraj.novaide.plugins.InstalledPlugin
import com.mohnishraj.novaide.plugins.PluginActionType
import com.mohnishraj.novaide.plugins.PluginManifestParser
import com.mohnishraj.novaide.plugins.PluginPermission
import com.mohnishraj.novaide.plugins.PluginPolicy
import com.mohnishraj.novaide.productivity.CommandPaletteEngine
import com.mohnishraj.novaide.productivity.ConsoleContext
import com.mohnishraj.novaide.productivity.ConsoleFile
import com.mohnishraj.novaide.productivity.NovaConsoleEngine
import com.mohnishraj.novaide.productivity.NovaTask
import com.mohnishraj.novaide.productivity.PaletteCommand
import com.mohnishraj.novaide.productivity.TaskRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M8ProductivityTest {
    private val context = ConsoleContext(
        files = listOf(
            ConsoleFile("README.md", 40, "# Demo\nTODO document this\nReady"),
            ConsoleFile("src/App.kt", 70, "fun main() {\n println(\"hello\")\n}\n// FIXME cleanup")
        ),
        projectName = "Demo",
        activeFile = "src/App.kt",
        selection = "println"
    )

    @Test fun pluginManifestParsesPermissionSandbox() {
        val raw = """{
          "id":"dev.demo.tools","name":"Demo Tools","version":"1.0.0","author":"Nova",
          "permissions":["READ_WORKSPACE","EDITOR_WRITE"],
          "commands":[
            {"id":"find-todos","title":"Find TODOs","action":"CONSOLE","value":"grep TODO"},
            {"id":"insert-log","title":"Insert log","action":"INSERT","value":"println(\\\"debug\\\")"}
          ]
        }""".trimIndent()
        val manifest = PluginManifestParser.parse(raw)
        assertEquals("dev.demo.tools", manifest.id)
        assertEquals(2, manifest.commands.size)
        assertTrue(PluginPermission.READ_WORKSPACE in manifest.permissions)
        val plan = PluginPolicy.plan(InstalledPlugin(manifest, raw), "find-todos")
        assertEquals(PluginActionType.CONSOLE, plan.action)
    }

    @Test fun pluginRejectsUndeclaredPermission() {
        val raw = """{"id":"dev.bad.link","name":"Bad","version":"1","permissions":[],"commands":[{"id":"open-site","title":"Open","action":"OPEN_URL","value":"https://example.com"}]}"""
        val error = runCatching { PluginManifestParser.parse(raw) }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("undeclared permission"))
    }

    @Test fun pluginRejectsInsecureLink() {
        val raw = """{"id":"dev.bad.http","name":"Bad","version":"1","permissions":["OPEN_EXTERNAL"],"commands":[{"id":"open-site","title":"Open","action":"OPEN_URL","value":"http://example.com"}]}"""
        assertTrue(runCatching { PluginManifestParser.parse(raw) }.isFailure)
    }

    @Test fun commandPaletteSupportsFuzzySearch() {
        val commands = listOf(
            PaletteCommand("save", "Save all files", "Persist changes", "File"),
            PaletteCommand("audit", "Run project health audit", "Diagnostics", "Analyze", listOf("security", "performance"))
        )
        assertEquals("audit", CommandPaletteEngine.search(commands, "prj hlth").first().id)
        assertEquals("save", CommandPaletteEngine.search(commands, "save").first().id)
    }

    @Test fun consoleGrepAndVariablesAreBounded() {
        val result = NovaConsoleEngine.execute("grep -i TODO", context)
        assertTrue(result.success)
        assertTrue(result.output.contains("README.md:2"))
        assertEquals("src/App.kt", NovaConsoleEngine.execute("echo ${'$'}{file}", context).output)
    }

    @Test fun consoleBlocksParentTraversal() {
        val result = NovaConsoleEngine.execute("cat ../secret.txt", context)
        assertFalse(result.success)
        assertTrue(result.output.contains("Parent path"))
    }

    @Test fun taskRunnerStopsOnFailure() {
        val task = NovaTask("dev.demo.task", "Demo", "", listOf("project-info", "cat missing.txt", "find .kt"))
        val result = TaskRunner.run(task, context)
        assertFalse(result.success)
        assertEquals(1, result.completedCommands)
    }

    @Test fun taskRunnerRejectsShellCommands() {
        val task = NovaTask("dev.demo.shell", "Bad", "", listOf("rm -rf /"))
        assertTrue(runCatching { TaskRunner.validate(task) }.isFailure)
    }

    @Test fun builtInTasksRemainSafe() {
        val tasks = TaskRunner.builtIns()
        assertTrue(tasks.size >= 4)
        assertTrue(tasks.all { runCatching { TaskRunner.validate(it) }.isSuccess })
    }
}
