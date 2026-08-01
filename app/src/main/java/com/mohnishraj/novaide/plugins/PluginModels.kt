package com.mohnishraj.novaide.plugins

enum class PluginPermission(val label: String, val explanation: String) {
    READ_WORKSPACE("Read workspace", "May inspect indexed workspace file names and text through safe console commands."),
    EDITOR_WRITE("Edit active file", "May insert text into the currently open editor after you run its command."),
    OPEN_EXTERNAL("Open HTTPS links", "May open an explicitly declared secure web page in your browser."),
    CLIPBOARD_WRITE("Copy text", "May copy declared text to the clipboard after you run its command.")
}

enum class PluginActionType { CONSOLE, INSERT, OPEN_URL, MESSAGE, COPY }

data class PluginCommand(
    val id: String,
    val title: String,
    val description: String,
    val action: PluginActionType,
    val value: String,
    val keywords: List<String> = emptyList()
)

data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val permissions: Set<PluginPermission>,
    val commands: List<PluginCommand>
)

data class InstalledPlugin(val manifest: PluginManifest, val rawManifest: String, val enabled: Boolean = true)

data class PluginExecutionPlan(
    val pluginId: String,
    val commandId: String,
    val action: PluginActionType,
    val value: String,
    val requiredPermission: PluginPermission?
)
