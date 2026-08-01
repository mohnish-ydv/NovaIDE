package com.mohnishraj.novaide.plugins

object PluginManifestParser {
    private const val MAX_COMMANDS = 24
    private val idPattern = Regex("[a-z0-9][a-z0-9._-]{2,63}")

    fun parse(raw: String): PluginManifest {
        val root = MiniJson.parseObject(raw)
        val id = root.string("id", 64).lowercase()
        require(idPattern.matches(id)) { "Plugin id must use 3-64 lowercase letters, numbers, dots, dashes or underscores" }
        val name = root.string("name", 60)
        val version = root.string("version", 24)
        val description = root.optionalString("description", 240)
        val author = root.optionalString("author", 80)
        val permissions = root.list("permissions").map { value ->
            val key = (value as? String)?.trim()?.uppercase() ?: error("Plugin permissions must be strings")
            runCatching { PluginPermission.valueOf(key) }.getOrElse { error("Unknown plugin permission: $key") }
        }.toSet()
        val commandValues = root.list("commands")
        require(commandValues.isNotEmpty()) { "Plugin must declare at least one command" }
        require(commandValues.size <= MAX_COMMANDS) { "Plugin may declare at most $MAX_COMMANDS commands" }
        val commandIds = mutableSetOf<String>()
        val commands = commandValues.map { item ->
            @Suppress("UNCHECKED_CAST")
            val obj = item as? Map<String, Any?> ?: error("Plugin command must be an object")
            val commandId = obj.string("id", 48).lowercase()
            require(idPattern.matches(commandId)) { "Invalid command id: $commandId" }
            require(commandIds.add(commandId)) { "Duplicate plugin command id: $commandId" }
            val actionName = obj.string("action", 24).uppercase()
            val action = runCatching { PluginActionType.valueOf(actionName) }
                .getOrElse { error("Unsupported plugin action: $actionName") }
            val value = obj.string("value", 16_000)
            val command = PluginCommand(
                id = commandId,
                title = obj.string("title", 70),
                description = obj.optionalString("description", 180),
                action = action,
                value = value,
                keywords = obj.optionalList("keywords").map { it.toString().take(40) }.take(12)
            )
            PluginPolicy.validateCommand(command, permissions)
            command
        }
        return PluginManifest(id, name, version, description, author, permissions, commands)
    }

    private fun Map<String, Any?>.string(key: String, max: Int): String {
        val value = this[key] as? String ?: error("Missing string field: $key")
        val trimmed = value.trim()
        require(trimmed.isNotEmpty()) { "$key cannot be blank" }
        require(trimmed.length <= max) { "$key is too long" }
        return trimmed
    }

    private fun Map<String, Any?>.optionalString(key: String, max: Int): String {
        val value = this[key] as? String ?: return ""
        require(value.length <= max) { "$key is too long" }
        return value.trim()
    }

    private fun Map<String, Any?>.list(key: String): List<Any?> =
        this[key] as? List<Any?> ?: error("Missing array field: $key")

    private fun Map<String, Any?>.optionalList(key: String): List<Any?> =
        this[key] as? List<Any?> ?: emptyList()
}
