package com.mohnishraj.novaide.plugins

import java.net.URI

object PluginPolicy {
    private const val MAX_INSERT_CHARS = 16_000
    private const val MAX_CONSOLE_CHARS = 4_000

    fun requiredPermission(action: PluginActionType): PluginPermission? = when (action) {
        PluginActionType.CONSOLE -> PluginPermission.READ_WORKSPACE
        PluginActionType.INSERT -> PluginPermission.EDITOR_WRITE
        PluginActionType.OPEN_URL -> PluginPermission.OPEN_EXTERNAL
        PluginActionType.COPY -> PluginPermission.CLIPBOARD_WRITE
        PluginActionType.MESSAGE -> null
    }

    fun validateCommand(command: PluginCommand, declared: Set<PluginPermission>) {
        val required = requiredPermission(command.action)
        require(required == null || required in declared) {
            "Command ${command.id} requires undeclared permission ${required?.name}"
        }
        when (command.action) {
            PluginActionType.CONSOLE -> {
                require(command.value.length <= MAX_CONSOLE_CHARS) { "Console command is too long" }
                require(!command.value.contains('\n')) { "Console action must contain one command" }
            }
            PluginActionType.INSERT, PluginActionType.COPY ->
                require(command.value.length <= MAX_INSERT_CHARS) { "Plugin text payload is too large" }
            PluginActionType.OPEN_URL -> validateHttpsUrl(command.value)
            PluginActionType.MESSAGE -> require(command.value.length <= 1_000) { "Plugin message is too long" }
        }
    }

    fun plan(plugin: InstalledPlugin, commandId: String): PluginExecutionPlan {
        require(plugin.enabled) { "Plugin is disabled" }
        val command = plugin.manifest.commands.firstOrNull { it.id == commandId }
            ?: error("Plugin command not found")
        validateCommand(command, plugin.manifest.permissions)
        return PluginExecutionPlan(
            pluginId = plugin.manifest.id,
            commandId = command.id,
            action = command.action,
            value = command.value,
            requiredPermission = requiredPermission(command.action)
        )
    }

    private fun validateHttpsUrl(raw: String) {
        val uri = runCatching { URI(raw) }.getOrElse { error("Plugin URL is invalid") }
        require(uri.scheme.equals("https", ignoreCase = true)) { "Plugin links must use HTTPS" }
        require(!uri.host.isNullOrBlank()) { "Plugin link must include a host" }
        require(uri.userInfo == null) { "Plugin links cannot include credentials" }
    }
}
