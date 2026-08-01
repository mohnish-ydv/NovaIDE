package com.mohnishraj.novaide.runtime

enum class RuntimeKind(val label: String) {
    STATIC_WEB("Static Web"),
    VITE("Vite"),
    REACT("React"),
    NEXT_JS("Next.js"),
    VUE("Vue"),
    SVELTE("Svelte / SvelteKit"),
    ASTRO("Astro"),
    ANGULAR("Angular"),
    NUXT("Nuxt"),
    NODE("Node.js"),
    PYTHON("Python"),
    PHP("PHP"),
    HUGO("Hugo"),
    JEKYLL("Jekyll"),
    MARKDOWN("Markdown"),
    MERMAID("Mermaid"),
    GENERIC("Generic project")
}

enum class RuntimeAction { INSTALL, BUILD, DEVELOP, RUN, TEST }

data class RuntimeCommand(
    val action: RuntimeAction,
    val label: String,
    val executable: String,
    val arguments: List<String>,
    val description: String,
    val opensServer: Boolean = false,
    val defaultPort: Int? = null,
    val destructive: Boolean = false
) {
    fun display(): String = (listOf(executable) + arguments).joinToString(" ") { TermuxCommandPolicy.shellDisplay(it) }
}

data class RuntimeProject(
    val kind: RuntimeKind,
    val confidence: Int,
    val packageManager: String?,
    val scripts: Map<String, String>,
    val commands: List<RuntimeCommand>,
    val buildOutputs: List<String>,
    val detectedOutput: String?,
    val evidence: List<String>,
    val warning: String? = null
) {
    val canUseWebPreview: Boolean get() = detectedOutput != null || kind in setOf(RuntimeKind.STATIC_WEB, RuntimeKind.MARKDOWN, RuntimeKind.MERMAID)
    fun command(action: RuntimeAction): RuntimeCommand? = commands.firstOrNull { it.action == action }
}

data class RuntimeEnvironment(
    val termuxInstalled: Boolean,
    val runPermissionGranted: Boolean,
    val sharedWorkspacePath: String?,
    val allowExternalAppsConfirmed: Boolean
) {
    val canExecute: Boolean get() = termuxInstalled && runPermissionGranted && sharedWorkspacePath != null && allowExternalAppsConfirmed

    fun blockers(): List<String> = buildList {
        if (!termuxInstalled) add("Install Termux from its official GitHub or F-Droid release.")
        if (!runPermissionGranted) add("Grant NovaIDE the additional ‘Run commands in Termux environment’ permission.")
        if (sharedWorkspacePath == null) add("Open the project from shared internal storage so Termux can access its real filesystem path.")
        if (!allowExternalAppsConfirmed) add("Set allow-external-apps=true in ~/.termux/termux.properties, restart Termux, then confirm setup here.")
    }
}

data class TermuxRunResult(
    val executionId: Int,
    val label: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val errorCode: Int,
    val errorMessage: String,
    val finishedAt: Long = System.currentTimeMillis()
) {
    val succeeded: Boolean get() = errorCode == -1 && exitCode == 0

    fun render(maxChars: Int = 120_000): String {
        val output = buildString {
            append(if (succeeded) "SUCCESS" else "FAILED")
            append(" · ").append(label).append('\n')
            append("Exit code: ").append(exitCode).append('\n')
            if (errorCode != -1 || errorMessage.isNotBlank()) {
                append("Termux error: ").append(errorCode).append(' ').append(errorMessage).append('\n')
            }
            if (stdout.isNotBlank()) append("\nSTDOUT\n").append(stdout.trimEnd()).append('\n')
            if (stderr.isNotBlank()) append("\nSTDERR\n").append(stderr.trimEnd()).append('\n')
        }
        return if (output.length <= maxChars) output else output.takeLast(maxChars).let { "[Earlier output truncated]\n$it" }
    }
}
