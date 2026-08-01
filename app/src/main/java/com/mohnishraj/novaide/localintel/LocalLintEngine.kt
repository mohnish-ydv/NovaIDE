package com.mohnishraj.novaide.localintel

import com.mohnishraj.novaide.editor.syntax.CodeLanguage
import com.mohnishraj.novaide.editor.syntax.LanguageDetector

enum class LintSeverity { ERROR, WARNING, INFO }

data class LocalLintIssue(
    val severity: LintSeverity,
    val line: Int,
    val column: Int,
    val rule: String,
    val message: String,
    val quickFixId: String? = null
)

object LocalLintEngine {
    private const val MAX_ISSUES = 500

    fun analyze(fileName: String, source: String): List<LocalLintIssue> {
        val issues = mutableListOf<LocalLintIssue>()
        val language = LanguageDetector.fromFileName(fileName)
        val imports = linkedMapOf<String, Int>()
        source.lineSequence().forEachIndexed { index, line ->
            if (issues.size >= MAX_ISSUES) return@forEachIndexed
            val lineNo = index + 1
            if (line.endsWith(' ') || line.endsWith('\t')) {
                issues += LocalLintIssue(LintSeverity.INFO, lineNo, line.length, "trailing-whitespace", "Trailing whitespace", "trim-trailing")
            }
            if ('\t' in line && line.any { it == ' ' }) {
                issues += LocalLintIssue(LintSeverity.INFO, lineNo, 1, "mixed-indentation", "Line mixes tabs and spaces", "tabs-to-spaces")
            }
            if (line.length > 140) {
                issues += LocalLintIssue(LintSeverity.INFO, lineNo, 141, "long-line", "Line is ${line.length} characters long")
            }
            if (Regex("\\b(TODO|FIXME|HACK)\\b", RegexOption.IGNORE_CASE).containsMatchIn(line)) {
                issues += LocalLintIssue(LintSeverity.INFO, lineNo, 1, "work-marker", "Unresolved work marker in source")
            }
            val import = when (language) {
                CodeLanguage.KOTLIN, CodeLanguage.JAVA -> Regex("^\\s*import\\s+([^;]+);?\\s*$").find(line)?.groupValues?.get(1)
                CodeLanguage.PYTHON -> Regex("^\\s*(?:from\\s+\\S+\\s+)?import\\s+(.+)$").find(line)?.groupValues?.get(1)
                CodeLanguage.JAVASCRIPT, CodeLanguage.TYPESCRIPT -> Regex("^\\s*import\\s+.*?from\\s+['\"]([^'\"]+)['\"]").find(line)?.groupValues?.get(1)
                else -> null
            }
            if (import != null) {
                val previous = imports.putIfAbsent(import.trim(), lineNo)
                if (previous != null) issues += LocalLintIssue(LintSeverity.WARNING, lineNo, 1, "duplicate-import", "Duplicate import; first declared on line $previous", "dedupe-imports")
            }
            languageRules(language, line, lineNo, issues)
        }
        issues += delimiterIssues(source)
        return issues.take(MAX_ISSUES).sortedWith(compareBy<LocalLintIssue> { it.line }.thenBy { it.severity.ordinal })
    }

    private fun languageRules(language: CodeLanguage, line: String, lineNo: Int, output: MutableList<LocalLintIssue>) {
        fun add(severity: LintSeverity, rule: String, message: String, fix: String? = null) {
            if (output.size < MAX_ISSUES) output += LocalLintIssue(severity, lineNo, 1, rule, message, fix)
        }
        when (language) {
            CodeLanguage.KOTLIN -> {
                if (Regex("(?<![?!])!!(?![=!])").containsMatchIn(line)) add(LintSeverity.WARNING, "forced-null", "Forced non-null assertion can crash; prefer a safe check")
                if (Regex("\\bGlobalScope\\.").containsMatchIn(line)) add(LintSeverity.WARNING, "global-scope", "GlobalScope is lifecycle-unaware and can leak work")
                if (Regex("catch\\s*\\([^)]*\\)\\s*\\{\\s*}").containsMatchIn(line)) add(LintSeverity.WARNING, "empty-catch", "Empty catch block hides failures")
            }
            CodeLanguage.JAVA -> {
                if (Regex("catch\\s*\\([^)]*\\)\\s*\\{\\s*}").containsMatchIn(line)) add(LintSeverity.WARNING, "empty-catch", "Empty catch block hides failures")
                if ("System.out.println" in line) add(LintSeverity.INFO, "debug-output", "Debug output remains in source")
            }
            CodeLanguage.JAVASCRIPT, CodeLanguage.TYPESCRIPT -> {
                if (Regex("(^|[^=])==([^=]|$)").containsMatchIn(line) || Regex("(^|[^!])!=([^=]|$)").containsMatchIn(line)) {
                    add(LintSeverity.WARNING, "loose-equality", "Loose equality performs type coercion", "strict-equality")
                }
                if (Regex("\\bconsole\\.(log|debug)\\s*\\(").containsMatchIn(line)) add(LintSeverity.INFO, "debug-output", "Debug console call remains in source")
                if (language == CodeLanguage.TYPESCRIPT && Regex("\\bany\\b").containsMatchIn(line)) add(LintSeverity.INFO, "typescript-any", "'any' bypasses type safety")
            }
            CodeLanguage.PYTHON -> {
                if (Regex("^\\s*except\\s*:\\s*$").matches(line)) add(LintSeverity.WARNING, "bare-except", "Bare except catches system-exiting exceptions")
                if (Regex("^\\s*print\\s*\\(").containsMatchIn(line)) add(LintSeverity.INFO, "debug-output", "Print call may be debug output")
            }
            CodeLanguage.HTML -> {
                if (Regex("<img\\b", RegexOption.IGNORE_CASE).containsMatchIn(line) && !Regex("\\balt\\s*=", RegexOption.IGNORE_CASE).containsMatchIn(line)) {
                    add(LintSeverity.WARNING, "image-alt", "Image is missing alt text")
                }
                if (Regex("target\\s*=\\s*['\"]_blank['\"]", RegexOption.IGNORE_CASE).containsMatchIn(line) && !Regex("rel\\s*=\\s*['\"][^'\"]*noopener", RegexOption.IGNORE_CASE).containsMatchIn(line)) {
                    add(LintSeverity.WARNING, "noopener", "target=_blank should include rel=noopener")
                }
            }
            CodeLanguage.SHELL -> if (Regex("\\brm\\s+-rf\\s+[/~]?(?:\\s|$)").containsMatchIn(line)) {
                add(LintSeverity.ERROR, "dangerous-delete", "Potentially destructive recursive delete command")
            }
            else -> Unit
        }
    }

    private data class OpenDelimiter(val ch: Char, val offset: Int, val line: Int, val column: Int)

    private fun delimiterIssues(source: String): List<LocalLintIssue> {
        val stack = ArrayDeque<OpenDelimiter>()
        val output = mutableListOf<LocalLintIssue>()
        var line = 1
        var column = 0
        var quote: Char? = null
        var escaped = false
        var lineComment = false
        var blockComment = false
        var index = 0
        while (index < source.length && output.size < 40) {
            val ch = source[index]
            val next = source.getOrNull(index + 1)
            column++
            if (ch == '\n') {
                line++
                column = 0
                lineComment = false
                index++
                continue
            }
            if (lineComment) { index++; continue }
            if (blockComment) {
                if (ch == '*' && next == '/') { blockComment = false; index += 2; column++ } else index++
                continue
            }
            if (quote != null) {
                if (escaped) escaped = false
                else if (ch == '\\') escaped = true
                else if (ch == quote) quote = null
                index++
                continue
            }
            if (ch == '/' && next == '/') { lineComment = true; index += 2; column++; continue }
            if (ch == '/' && next == '*') { blockComment = true; index += 2; column++; continue }
            if (ch == '\'' || ch == '"' || ch == '`') { quote = ch; index++; continue }
            when (ch) {
                '(', '[', '{' -> stack.add(OpenDelimiter(ch, index, line, column))
                ')', ']', '}' -> {
                    val expected = when (ch) { ')' -> '('; ']' -> '['; else -> '{' }
                    val last = stack.removeLastOrNull()
                    if (last == null || last.ch != expected) {
                        output += LocalLintIssue(LintSeverity.ERROR, line, column, "unmatched-delimiter", "Unmatched '$ch'")
                        if (last != null && last.ch != expected) stack.add(last)
                    }
                }
            }
            index++
        }
        stack.takeLast(20).forEach { open ->
            output += LocalLintIssue(LintSeverity.ERROR, open.line, open.column, "unclosed-delimiter", "Unclosed '${open.ch}'")
        }
        return output
    }
}
