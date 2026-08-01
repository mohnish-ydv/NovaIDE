package com.mohnishraj.novaide.editor.search

import java.util.regex.Matcher

data class TextRange(val start: Int, val endExclusive: Int) {
    init {
        require(start >= 0) { "start must be non-negative" }
        require(endExclusive >= start) { "end must not precede start" }
    }

    val length: Int get() = endExclusive - start
}

data class SearchOptions(
    val matchCase: Boolean = false,
    val wholeWord: Boolean = false,
    val regex: Boolean = false
)

data class SearchResult(
    val matches: List<TextRange>,
    val error: String? = null,
    val truncated: Boolean = false
)

data class ReplacementResult(
    val text: String,
    val replacementCount: Int,
    val error: String? = null
)

object SearchEngine {
    const val MAX_MATCHES = 5_000

    fun findAll(text: String, query: String, options: SearchOptions): SearchResult {
        if (query.isEmpty()) return SearchResult(emptyList())
        val regex = buildRegex(query, options) ?: return SearchResult(emptyList(), "Invalid regular expression")
        return try {
            val matches = ArrayList<TextRange>()
            var truncated = false
            for (match in regex.findAll(text)) {
                if (match.value.isEmpty()) continue
                if (matches.size >= MAX_MATCHES) {
                    truncated = true
                    break
                }
                matches += TextRange(match.range.first, match.range.last + 1)
            }
            SearchResult(matches, truncated = truncated)
        } catch (error: RuntimeException) {
            SearchResult(emptyList(), error.message ?: "Search failed")
        }
    }

    fun replacementForMatch(
        source: String,
        range: TextRange,
        query: String,
        replacement: String,
        options: SearchOptions
    ): String {
        if (!options.regex) return replacement
        val regex = buildRegex(query, options) ?: return replacement
        return runCatching {
            val matcher = regex.toPattern().matcher(source)
            while (matcher.find()) {
                if (matcher.start() == range.start && matcher.end() == range.endExclusive) {
                    val output = StringBuffer()
                    matcher.appendReplacement(output, replacement)
                    return@runCatching output.substring(range.start)
                }
                if (matcher.start() > range.start) break
            }
            replacement
        }.getOrDefault(replacement)
    }

    fun replaceAll(
        source: String,
        query: String,
        replacement: String,
        options: SearchOptions
    ): ReplacementResult {
        val search = findAll(source, query, options)
        if (search.error != null) return ReplacementResult(source, 0, search.error)
        if (search.truncated) {
            return ReplacementResult(
                source,
                0,
                "Replace All is limited to $MAX_MATCHES matches for editor safety. Narrow the search first."
            )
        }
        if (search.matches.isEmpty()) return ReplacementResult(source, 0)

        val regex = buildRegex(query, options)
            ?: return ReplacementResult(source, 0, "Invalid regular expression")
        return try {
            val matcher = regex.toPattern().matcher(source)
            val output = StringBuffer(source.length)
            val safeReplacement = if (options.regex) replacement else Matcher.quoteReplacement(replacement)
            var count = 0
            while (matcher.find()) {
                if (matcher.start() == matcher.end()) continue
                matcher.appendReplacement(output, safeReplacement)
                count++
            }
            matcher.appendTail(output)
            ReplacementResult(output.toString(), count)
        } catch (error: RuntimeException) {
            ReplacementResult(source, 0, error.message ?: "Invalid replacement expression")
        }
    }

    private fun buildRegex(query: String, options: SearchOptions): Regex? {
        val body = if (options.regex) query else Regex.escape(query)
        val pattern = if (options.wholeWord) "(?<![A-Za-z0-9_])(?:$body)(?![A-Za-z0-9_])" else body
        val flags = buildSet {
            add(RegexOption.MULTILINE)
            if (!options.matchCase) add(RegexOption.IGNORE_CASE)
        }
        return runCatching { Regex(pattern, flags) }.getOrNull()
    }
}
