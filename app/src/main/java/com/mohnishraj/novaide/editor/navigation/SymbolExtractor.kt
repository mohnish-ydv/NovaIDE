package com.mohnishraj.novaide.editor.navigation

import com.mohnishraj.novaide.editor.syntax.CodeLanguage
import com.mohnishraj.novaide.editor.syntax.LanguageDetector

data class CodeSymbol(
    val name: String,
    val kind: String,
    val offset: Int,
    val line: Int
)

object SymbolExtractor {
    private const val MAX_SYMBOLS = 1_000

    fun extract(fileName: String, source: String): List<CodeSymbol> {
        val language = LanguageDetector.fromFileName(fileName)
        val found = mutableListOf<CodeSymbol>()
        val patterns = patternsFor(language)
        var lineOffset = 0
        source.lineSequence().forEachIndexed { index, line ->
            if (found.size < MAX_SYMBOLS) {
                patterns.forEach { pattern ->
                    if (found.size >= MAX_SYMBOLS) return@forEach
                    pattern.regex.findAll(line).forEach { match ->
                        val name = match.groups[pattern.nameGroup]?.value?.trim().orEmpty()
                        if (name.isNotEmpty() && found.size < MAX_SYMBOLS) {
                            found += CodeSymbol(name, pattern.kind, lineOffset + match.range.first, index + 1)
                        }
                    }
                }
            }
            lineOffset += line.length + 1
        }
        return found.distinctBy { Triple(it.name, it.kind, it.offset) }.sortedBy { it.offset }
    }

    private data class Pattern(val kind: String, val regex: Regex, val nameGroup: Int = 1)

    private fun patternsFor(language: CodeLanguage): List<Pattern> = when (language) {
        CodeLanguage.KOTLIN -> listOf(
            Pattern("class", Regex("\\b(?:data\\s+|sealed\\s+|enum\\s+|annotation\\s+)?class\\s+([A-Za-z_][A-Za-z0-9_]*)")),
            Pattern("interface", Regex("\\binterface\\s+([A-Za-z_][A-Za-z0-9_]*)")),
            Pattern("object", Regex("\\bobject\\s+([A-Za-z_][A-Za-z0-9_]*)")),
            Pattern("function", Regex("\\bfun\\s+(?:<[^>]+>\\s*)?([A-Za-z_][A-Za-z0-9_]*)\\s*\\(")),
            Pattern("property", Regex("\\b(?:val|var)\\s+([A-Za-z_][A-Za-z0-9_]*)"))
        )
        CodeLanguage.JAVA, CodeLanguage.CSHARP, CodeLanguage.C_FAMILY -> listOf(
            Pattern("type", Regex("\\b(?:class|interface|enum|struct|record)\\s+([A-Za-z_][A-Za-z0-9_]*)")),
            Pattern("method", Regex("(?:^|\\s)(?:public|private|protected|static|final|virtual|override|suspend|async|inline|\\w+[<>, ?\\[\\]]*)+\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\([^;]*\\)\\s*(?:\\{|=>)"))
        )
        CodeLanguage.JAVASCRIPT, CodeLanguage.TYPESCRIPT -> listOf(
            Pattern("class", Regex("\\bclass\\s+([A-Za-z_$][A-Za-z0-9_$]*)")),
            Pattern("function", Regex("\\bfunction\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(")),
            Pattern("function", Regex("\\b(?:const|let|var)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*(?:async\\s*)?(?:\\([^)]*\\)|[A-Za-z_$][A-Za-z0-9_$]*)\\s*=>")),
            Pattern("method", Regex("^\\s*(?:async\\s+)?([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\([^)]*\\)\\s*\\{"))
        )
        CodeLanguage.PYTHON -> listOf(
            Pattern("class", Regex("^\\s*class\\s+([A-Za-z_][A-Za-z0-9_]*)")),
            Pattern("function", Regex("^\\s*(?:async\\s+)?def\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\("))
        )
        CodeLanguage.GO -> listOf(
            Pattern("type", Regex("^\\s*type\\s+([A-Za-z_][A-Za-z0-9_]*)")),
            Pattern("function", Regex("^\\s*func\\s+(?:\\([^)]*\\)\\s*)?([A-Za-z_][A-Za-z0-9_]*)\\s*\\("))
        )
        CodeLanguage.RUST -> listOf(
            Pattern("type", Regex("^\\s*(?:pub\\s+)?(?:struct|enum|trait)\\s+([A-Za-z_][A-Za-z0-9_]*)")),
            Pattern("function", Regex("^\\s*(?:pub(?:\\([^)]*\\))?\\s+)?(?:async\\s+)?fn\\s+([A-Za-z_][A-Za-z0-9_]*)"))
        )
        CodeLanguage.DART -> listOf(
            Pattern("class", Regex("\\bclass\\s+([A-Za-z_][A-Za-z0-9_]*)")),
            Pattern("function", Regex("^\\s*(?:[A-Za-z_<>,?\\[\\] ]+\\s+)?([A-Za-z_][A-Za-z0-9_]*)\\s*\\([^;]*\\)\\s*(?:async\\s*)?\\{"))
        )
        CodeLanguage.HTML, CodeLanguage.XML -> listOf(
            Pattern("id", Regex("\\bid\\s*=\\s*[\"']([^\"']+)[\"']")),
            Pattern("tag", Regex("<([A-Za-z][A-Za-z0-9:_-]*)\\b"))
        )
        CodeLanguage.CSS -> listOf(Pattern("selector", Regex("^\\s*([^@][^{]+)\\s*\\{")))
        CodeLanguage.MARKDOWN -> listOf(Pattern("heading", Regex("^\\s{0,3}#{1,6}\\s+(.+?)\\s*$")))
        CodeLanguage.SQL -> listOf(Pattern("statement", Regex("^\\s*(?:CREATE\\s+(?:TABLE|VIEW|FUNCTION|PROCEDURE)|WITH)\\s+([A-Za-z_][A-Za-z0-9_.]*)", RegexOption.IGNORE_CASE)))
        else -> emptyList()
    }
}
