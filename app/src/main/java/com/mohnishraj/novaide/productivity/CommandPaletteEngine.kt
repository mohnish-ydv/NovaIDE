package com.mohnishraj.novaide.productivity

data class PaletteCommand(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val keywords: List<String> = emptyList(),
    val shortcut: String = "",
    val source: String = "NovaIDE"
)

object CommandPaletteEngine {
    private const val MAX_RESULTS = 60

    fun search(commands: List<PaletteCommand>, query: String): List<PaletteCommand> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return commands.sortedWith(compareBy<PaletteCommand> { it.category }.thenBy { it.title }).take(MAX_RESULTS)
        return commands.mapNotNull { command ->
            val title = command.title.lowercase()
            val haystack = buildString {
                append(title).append(' ')
                append(command.description.lowercase()).append(' ')
                append(command.category.lowercase()).append(' ')
                append(command.keywords.joinToString(" ").lowercase())
            }
            val score = when {
                title == normalized -> 10_000
                title.startsWith(normalized) -> 8_000 - title.length
                haystack.contains(normalized) -> 6_000 - haystack.indexOf(normalized)
                else -> fuzzyScore(normalized, haystack)
            }
            if (score <= 0) null else command to score
        }.sortedWith(compareByDescending<Pair<PaletteCommand, Int>> { it.second }.thenBy { it.first.title })
            .take(MAX_RESULTS).map { it.first }
    }

    private fun fuzzyScore(needle: String, haystack: String): Int {
        var cursor = 0
        var score = 0
        var streak = 0
        for (char in needle) {
            val found = haystack.indexOf(char, cursor)
            if (found < 0) return 0
            streak = if (found == cursor) streak + 1 else 0
            score += 12 + streak * 4 - (found - cursor).coerceAtMost(10)
            cursor = found + 1
        }
        return score
    }
}
