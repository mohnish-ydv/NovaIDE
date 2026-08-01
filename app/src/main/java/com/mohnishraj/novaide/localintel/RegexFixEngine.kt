package com.mohnishraj.novaide.localintel

import com.mohnishraj.novaide.editor.syntax.CodeLanguage
import com.mohnishraj.novaide.editor.syntax.LanguageDetector

data class RegexFixProposal(
    val id: String,
    val title: String,
    val description: String,
    val replacements: Int,
    val output: String
)

object RegexFixEngine {
    fun proposals(fileName: String, source: String): List<RegexFixProposal> {
        val output = mutableListOf<RegexFixProposal>()
        add(output, "trim-trailing", "Trim trailing whitespace", "Removes spaces and tabs at line endings", source) {
            it.replace(Regex("[ \\t]+(?=\\r?$)", setOf(RegexOption.MULTILINE)), "")
        }
        add(output, "normalize-lines", "Normalize line endings", "Converts CRLF/CR to LF", source) {
            it.replace("\r\n", "\n").replace('\r', '\n')
        }
        add(output, "blank-lines", "Collapse excessive blank lines", "Keeps at most two consecutive blank lines", source) {
            it.replace(Regex("\\n[ \\t]*\\n[ \\t]*\\n(?:[ \\t]*\\n)+"), "\n\n\n")
        }
        add(output, "tabs-to-spaces", "Tabs to four spaces", "Replaces tab characters with four spaces", source) {
            it.replace("\t", "    ")
        }
        add(output, "dedupe-imports", "Remove duplicate imports", "Keeps the first identical import declaration", source) {
            dedupeImports(fileName, it)
        }
        val language = LanguageDetector.fromFileName(fileName)
        if (language == CodeLanguage.JAVASCRIPT || language == CodeLanguage.TYPESCRIPT) {
            add(output, "strict-equality", "Use strict JavaScript equality", "Safely changes standalone == and != operators to === and !==", source) {
                it.replace(Regex("(?<![=!])==(?!=)"), "===")
                    .replace(Regex("(?<!!)!=(?!=)"), "!==")
            }
        }
        return output
    }

    private fun add(
        output: MutableList<RegexFixProposal>,
        id: String,
        title: String,
        description: String,
        source: String,
        transform: (String) -> String
    ) {
        val changed = transform(source)
        if (changed == source) return
        val replacements = differenceEstimate(source, changed)
        output += RegexFixProposal(id, title, description, replacements, changed)
    }

    private fun dedupeImports(fileName: String, source: String): String {
        val language = LanguageDetector.fromFileName(fileName)
        val importPattern = when (language) {
            CodeLanguage.KOTLIN, CodeLanguage.JAVA -> Regex("^\\s*import\\s+[^;]+;?\\s*$")
            CodeLanguage.PYTHON -> Regex("^\\s*(?:from\\s+\\S+\\s+)?import\\s+.+$")
            CodeLanguage.JAVASCRIPT, CodeLanguage.TYPESCRIPT -> Regex("^\\s*import\\s+.+$")
            else -> return source
        }
        val seen = linkedSetOf<String>()
        return source.lineSequence().filter { line ->
            if (!importPattern.matches(line)) true else seen.add(line.trim())
        }.joinToString("\n") + if (source.endsWith('\n')) "\n" else ""
    }

    private fun differenceEstimate(before: String, after: String): Int {
        val beforeLines = before.lines()
        val afterLines = after.lines()
        val common = minOf(beforeLines.size, afterLines.size)
        var changed = kotlin.math.abs(beforeLines.size - afterLines.size)
        for (index in 0 until common) if (beforeLines[index] != afterLines[index]) changed++
        return changed.coerceAtLeast(1)
    }
}
