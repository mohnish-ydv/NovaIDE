package com.mohnishraj.novaide.productivity

import java.security.MessageDigest
import java.util.Base64

data class ConsoleFile(val path: String, val size: Long, val content: String?)
data class ConsoleContext(
    val files: List<ConsoleFile>,
    val projectName: String,
    val activeFile: String? = null,
    val selection: String = ""
)
data class ConsoleResult(val output: String, val success: Boolean = true)

object NovaConsoleEngine {
    const val MAX_OUTPUT_CHARS = 80_000
    const val MAX_MATCHES = 2_000
    private val supported = setOf("help", "pwd", "ls", "find", "grep", "cat", "head", "tail", "wc", "hash", "base64", "project-info", "echo", "clear")

    fun supportedCommands(): Set<String> = supported

    fun execute(raw: String, context: ConsoleContext): ConsoleResult {
        val args = tokenize(interpolate(raw, context))
        if (args.isEmpty()) return ConsoleResult("")
        return runCatching {
            when (args.first().lowercase()) {
                "help" -> ConsoleResult(help())
                "pwd" -> ConsoleResult("/${context.projectName}")
                "ls" -> ConsoleResult(list(context, args.drop(1)))
                "find" -> ConsoleResult(find(context, args.drop(1)))
                "grep" -> ConsoleResult(grep(context, args.drop(1)))
                "cat" -> ConsoleResult(read(context, args.getOrNull(1), null, false))
                "head" -> ConsoleResult(read(context, args.getOrNull(1), args.getOrNull(2)?.toIntOrNull() ?: 20, false))
                "tail" -> ConsoleResult(read(context, args.getOrNull(1), args.getOrNull(2)?.toIntOrNull() ?: 20, true))
                "wc" -> ConsoleResult(wordCount(context, args.getOrNull(1)))
                "hash" -> ConsoleResult(hash(context, args.getOrNull(1)))
                "base64" -> ConsoleResult(base64(args.drop(1).joinToString(" ")))
                "project-info" -> ConsoleResult(projectInfo(context))
                "echo" -> ConsoleResult(args.drop(1).joinToString(" "))
                "clear" -> ConsoleResult("__NOVA_CLEAR__")
                else -> ConsoleResult("Unknown safe command: ${args.first()}\nRun 'help' to see supported commands.", false)
            }
        }.getOrElse { ConsoleResult(it.message ?: "Command failed", false) }
    }

    private fun list(context: ConsoleContext, args: List<String>): String {
        val prefix = normalizePath(args.firstOrNull().orEmpty())
        val depth = prefix.count { it == '/' }
        return context.files.asSequence()
            .filter { prefix.isBlank() || it.path == prefix || it.path.startsWith("$prefix/") }
            .filter { it.path.count { char -> char == '/' } <= depth + 1 }
            .sortedBy { it.path }
            .joinToString("\n") { "${it.path}\t${it.size} B" }
            .bounded()
    }

    private fun find(context: ConsoleContext, args: List<String>): String {
        val query = args.joinToString(" ").trim().lowercase()
        require(query.isNotBlank()) { "Usage: find <name>" }
        return context.files.asSequence().map { it.path }.filter { it.lowercase().contains(query) }
            .take(MAX_MATCHES).joinToString("\n").ifBlank { "No matching files" }.bounded()
    }

    private fun grep(context: ConsoleContext, args: List<String>): String {
        val insensitive = args.firstOrNull() == "-i"
        val effective = if (insensitive) args.drop(1) else args
        val query = effective.firstOrNull()?.takeIf { it.isNotBlank() } ?: error("Usage: grep [-i] <text> [path]")
        val pathFilter = effective.getOrNull(1)?.let(::normalizePath)
        val results = mutableListOf<String>()
        for (file in context.files) {
            if (results.size >= MAX_MATCHES) break
            if (pathFilter != null && file.path != pathFilter && !file.path.startsWith("$pathFilter/")) continue
            val content = file.content ?: continue
            content.lineSequence().forEachIndexed { lineIndex, line ->
                val match = if (insensitive) line.contains(query, true) else line.contains(query)
                if (match && results.size < MAX_MATCHES) results += "${file.path}:${lineIndex + 1}: ${line.trim().take(240)}"
            }
        }
        return results.joinToString("\n").ifBlank { "No matches" }.bounded()
    }

    private fun read(context: ConsoleContext, rawPath: String?, count: Int?, tail: Boolean): String {
        val path = normalizePath(rawPath ?: error("File path is required"))
        val content = context.files.firstOrNull { it.path == path }?.content ?: error("Text file not found: $path")
        val lines = content.lines()
        val selected = when {
            count == null -> lines
            tail -> lines.takeLast(count.coerceIn(1, 500))
            else -> lines.take(count.coerceIn(1, 500))
        }
        return selected.joinToString("\n").bounded()
    }

    private fun wordCount(context: ConsoleContext, rawPath: String?): String {
        val path = normalizePath(rawPath ?: error("Usage: wc <path>"))
        val content = context.files.firstOrNull { it.path == path }?.content ?: error("Text file not found: $path")
        val lines = if (content.isEmpty()) 0 else content.count { it == '\n' } + 1
        val words = Regex("\\S+").findAll(content).count()
        return "$lines lines, $words words, ${content.length} characters — $path"
    }

    private fun hash(context: ConsoleContext, rawPath: String?): String {
        val path = normalizePath(rawPath ?: error("Usage: hash <path>"))
        val content = context.files.firstOrNull { it.path == path }?.content ?: error("Text file not found: $path")
        return MessageDigest.getInstance("SHA-256").digest(content.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun base64(text: String): String {
        require(text.length <= 32_000) { "Input is too large" }
        return Base64.getEncoder().encodeToString(text.toByteArray())
    }

    private fun projectInfo(context: ConsoleContext): String {
        val textFiles = context.files.count { it.content != null }
        val total = context.files.sumOf { it.size.coerceAtLeast(0) }
        val extensions = context.files.map { it.path.substringAfterLast('.', "(none)").lowercase() }
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(8)
            .joinToString { "${it.key}:${it.value}" }
        return "Project: ${context.projectName}\nFiles: ${context.files.size} ($textFiles indexed text)\nSize: $total B\nTop types: $extensions"
    }

    private fun interpolate(raw: String, context: ConsoleContext): String = raw
        .replace("\${project}", context.projectName)
        .replace("\${file}", context.activeFile.orEmpty())
        .replace("\${selection}", context.selection.take(8_000))

    fun tokenize(raw: String): List<String> {
        val args = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var escaped = false
        raw.forEach { char ->
            when {
                escaped -> { current.append(char); escaped = false }
                char == '\\' -> escaped = true
                quote != null && char == quote -> quote = null
                quote == null && (char == '\'' || char == '"') -> quote = char
                quote == null && char.isWhitespace() -> if (current.isNotEmpty()) { args += current.toString(); current.clear() }
                else -> current.append(char)
            }
        }
        require(quote == null) { "Unclosed quote" }
        if (escaped) current.append('\\')
        if (current.isNotEmpty()) args += current.toString()
        return args
    }

    private fun normalizePath(raw: String): String {
        val clean = raw.trim().replace('\\', '/').trimStart('/')
        require(clean.split('/').none { it == ".." }) { "Parent path segments are not allowed" }
        return clean.replace(Regex("/+"), "/").removeSuffix("/")
    }

    private fun String.bounded(): String = if (length <= MAX_OUTPUT_CHARS) this else take(MAX_OUTPUT_CHARS) + "\n…output truncated"

    private fun help(): String = """
        Nova Console uses safe built-in commands; it does not expose an Android shell.
        help
        pwd
        ls [path]
        find <name>
        grep [-i] <text> [path]
        cat <path>
        head <path> [lines]
        tail <path> [lines]
        wc <path>
        hash <path>
        base64 <text>
        project-info
        echo <text>
        clear

        Variables: ${'$'}{project}, ${'$'}{file}, ${'$'}{selection}
    """.trimIndent()
}
