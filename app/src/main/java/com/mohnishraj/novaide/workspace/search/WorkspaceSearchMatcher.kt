package com.mohnishraj.novaide.workspace.search

data class WorkspaceSearchOptions(
    val caseSensitive: Boolean = false,
    val regex: Boolean = false,
    val includeGenerated: Boolean = false
)

data class LineMatch(val line: Int, val column: Int, val preview: String)

data class MatchResult(val matches: List<LineMatch>, val error: String? = null)

object WorkspaceSearchMatcher {
    private const val MAX_MATCHES_PER_FILE = 20
    private const val MAX_QUERY_LENGTH = 512
    private const val MAX_SEARCHABLE_LINE_CHARS = 16_384

    fun shouldSkip(path: String, includeGenerated: Boolean): Boolean {
        if (includeGenerated) return false
        val segments = path.replace('\\', '/').lowercase().split('/')
        return segments.any { it in setOf(".git", ".gradle", "build", "dist", "node_modules", ".idea", "target") }
    }

    fun compile(query: String, options: WorkspaceSearchOptions): Result<Regex> {
        if (query.isEmpty()) return Result.failure(IllegalArgumentException("Search query is empty"))
        if (query.length > MAX_QUERY_LENGTH) {
            return Result.failure(IllegalArgumentException("Search query is longer than $MAX_QUERY_LENGTH characters"))
        }
        return runCatching {
            val pattern = if (options.regex) query else Regex.escape(query)
            Regex(pattern, if (options.caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE))
        }
    }

    fun findLines(text: String, query: String, options: WorkspaceSearchOptions): MatchResult {
        val regex = compile(query, options).getOrElse {
            return MatchResult(emptyList(), it.message ?: "Invalid regular expression")
        }
        return findLines(text, regex)
    }

    fun findLines(text: String, regex: Regex): MatchResult {
        val output = mutableListOf<LineMatch>()
        text.lineSequence().forEachIndexed { index, line ->
            if (output.size >= MAX_MATCHES_PER_FILE) return@forEachIndexed
            val searchable = if (line.length > MAX_SEARCHABLE_LINE_CHARS) line.substring(0, MAX_SEARCHABLE_LINE_CHARS) else line
            val match = regex.find(searchable) ?: return@forEachIndexed
            val preview = searchable.trim().replace(Regex("\\s+"), " ").take(180)
            output += LineMatch(index + 1, match.range.first + 1, preview)
        }
        return MatchResult(output)
    }
}
