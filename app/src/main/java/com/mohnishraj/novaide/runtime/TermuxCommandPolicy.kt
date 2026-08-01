package com.mohnishraj.novaide.runtime

object TermuxCommandPolicy {
    private const val MAX_ARGUMENTS = 64
    private const val MAX_ARGUMENT_CHARS = 16_384
    private val allowedExecutables = setOf(
        "npm", "npx", "pnpm", "yarn", "bun", "node", "python", "python3", "php", "hugo", "bundle", "jekyll"
    )
    private val forbiddenShellFragments = listOf("\u0000", "\n", "\r")

    fun validate(command: RuntimeCommand) {
        require(command.executable in allowedExecutables) { "Unsupported runtime executable: ${command.executable}" }
        require(command.arguments.size <= MAX_ARGUMENTS) { "Runtime command has too many arguments" }
        require(command.arguments.sumOf { it.length } <= MAX_ARGUMENT_CHARS) { "Runtime command is too long" }
        command.arguments.forEach { argument ->
            require(forbiddenShellFragments.none(argument::contains)) { "Runtime arguments cannot contain control lines" }
        }
        require(!command.destructive) { "Destructive runtime commands are blocked" }
    }

    fun shellScript(command: RuntimeCommand): String {
        validate(command)
        return (listOf(command.executable) + command.arguments).joinToString(" ") { shellQuote(it) }
    }

    fun shellQuote(value: String): String = "'" + value.replace("'", "'\"'\"'") + "'"
    fun shellDisplay(value: String): String = if (value.matches(Regex("[A-Za-z0-9_./:=@+-]+"))) value else shellQuote(value)

    fun safePort(value: Int): Int {
        require(value in 1024..65535) { "Port must be between 1024 and 65535" }
        return value
    }
}
