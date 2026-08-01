package com.mohnishraj.novaide.localintel

import com.mohnishraj.novaide.editor.navigation.SymbolExtractor
import com.mohnishraj.novaide.editor.syntax.CodeLanguage
import com.mohnishraj.novaide.editor.syntax.LanguageDetector

data class CompletionItem(
    val label: String,
    val insertText: String,
    val detail: String,
    val replaceStart: Int,
    val replaceEnd: Int
)

object AutocompleteEngine {
    private const val MAX_ITEMS = 60

    fun suggest(fileName: String, source: String, cursor: Int): List<CompletionItem> {
        val safeCursor = cursor.coerceIn(0, source.length)
        var start = safeCursor
        while (start > 0 && isIdentifierPart(source[start - 1])) start--
        val prefix = source.substring(start, safeCursor)
        val language = LanguageDetector.fromFileName(fileName)
        val candidates = linkedMapOf<String, String>()
        keywords(language).forEach { candidates[it] = "${language.label} keyword" }
        SymbolExtractor.extract(fileName, source.take(500_000)).forEach { symbol ->
            candidates.putIfAbsent(symbol.name, "${symbol.kind} • line ${symbol.line}")
        }
        Regex("[A-Za-z_$][A-Za-z0-9_$]{2,}").findAll(source.take(500_000)).forEach { match ->
            candidates.putIfAbsent(match.value, "document word")
        }
        common(language).forEach { candidates.putIfAbsent(it.first, it.second) }

        return candidates.asSequence()
            .filter { (name, _) -> prefix.isBlank() || name.startsWith(prefix, ignoreCase = true) }
            .filter { (name, _) -> name != prefix }
            .sortedWith(compareBy<Map.Entry<String, String>> {
                when {
                    prefix.isNotBlank() && it.key.startsWith(prefix) -> 0
                    prefix.isNotBlank() && it.key.startsWith(prefix, ignoreCase = true) -> 1
                    else -> 2
                }
            }.thenBy { it.key.length }.thenBy { it.key.lowercase() })
            .take(MAX_ITEMS)
            .map { CompletionItem(it.key, it.key, it.value, start, safeCursor) }
            .toList()
    }

    private fun isIdentifierPart(ch: Char): Boolean = ch.isLetterOrDigit() || ch == '_' || ch == '$'

    private fun keywords(language: CodeLanguage): Set<String> = when (language) {
        CodeLanguage.KOTLIN -> setOf("as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is", "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof", "val", "var", "when", "while", "by", "catch", "constructor", "delegate", "dynamic", "field", "file", "finally", "get", "import", "init", "param", "property", "receiver", "set", "setparam", "where", "actual", "abstract", "annotation", "companion", "const", "crossinline", "data", "enum", "expect", "external", "final", "infix", "inline", "inner", "internal", "lateinit", "noinline", "open", "operator", "out", "override", "private", "protected", "public", "reified", "sealed", "suspend", "tailrec", "vararg")
        CodeLanguage.JAVA -> setOf("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while", "record", "sealed", "permits")
        CodeLanguage.JAVASCRIPT, CodeLanguage.TYPESCRIPT -> setOf("await", "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else", "export", "extends", "false", "finally", "for", "function", "if", "import", "in", "instanceof", "let", "new", "null", "return", "static", "super", "switch", "this", "throw", "true", "try", "typeof", "var", "void", "while", "with", "yield", "async", "interface", "type", "enum", "implements", "private", "protected", "public", "readonly")
        CodeLanguage.PYTHON -> setOf("and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif", "else", "except", "False", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "None", "nonlocal", "not", "or", "pass", "raise", "return", "True", "try", "while", "with", "yield", "match", "case")
        CodeLanguage.C_FAMILY -> setOf("auto", "break", "case", "char", "const", "continue", "default", "do", "double", "else", "enum", "extern", "float", "for", "goto", "if", "inline", "int", "long", "register", "restrict", "return", "short", "signed", "sizeof", "static", "struct", "switch", "typedef", "union", "unsigned", "void", "volatile", "while", "class", "namespace", "public", "private", "protected", "template", "typename", "using")
        CodeLanguage.CSHARP -> setOf("abstract", "as", "async", "await", "base", "bool", "break", "byte", "case", "catch", "char", "checked", "class", "const", "continue", "decimal", "default", "delegate", "do", "double", "else", "enum", "event", "explicit", "extern", "false", "finally", "fixed", "float", "for", "foreach", "if", "implicit", "in", "int", "interface", "internal", "is", "lock", "long", "namespace", "new", "null", "object", "operator", "out", "override", "params", "private", "protected", "public", "readonly", "record", "ref", "return", "sealed", "short", "sizeof", "stackalloc", "static", "string", "struct", "switch", "this", "throw", "true", "try", "typeof", "uint", "ulong", "unchecked", "unsafe", "ushort", "using", "virtual", "void", "volatile", "while")
        CodeLanguage.GO -> setOf("break", "default", "func", "interface", "select", "case", "defer", "go", "map", "struct", "chan", "else", "goto", "package", "switch", "const", "fallthrough", "if", "range", "type", "continue", "for", "import", "return", "var")
        CodeLanguage.RUST -> setOf("as", "async", "await", "break", "const", "continue", "crate", "dyn", "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in", "let", "loop", "match", "mod", "move", "mut", "pub", "ref", "return", "self", "Self", "static", "struct", "super", "trait", "true", "type", "unsafe", "use", "where", "while")
        CodeLanguage.DART -> setOf("abstract", "as", "assert", "async", "await", "break", "case", "catch", "class", "const", "continue", "covariant", "default", "deferred", "do", "dynamic", "else", "enum", "export", "extends", "extension", "external", "factory", "false", "final", "finally", "for", "Function", "get", "hide", "if", "implements", "import", "in", "interface", "is", "late", "library", "mixin", "new", "null", "on", "operator", "part", "required", "rethrow", "return", "set", "show", "static", "super", "switch", "sync", "this", "throw", "true", "try", "typedef", "var", "void", "while", "with", "yield")
        CodeLanguage.SQL -> setOf("SELECT", "FROM", "WHERE", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "ON", "GROUP", "BY", "ORDER", "HAVING", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE", "CREATE", "TABLE", "VIEW", "INDEX", "DROP", "ALTER", "AS", "DISTINCT", "UNION", "ALL", "LIMIT", "OFFSET", "CASE", "WHEN", "THEN", "ELSE", "END", "WITH")
        else -> emptySet()
    }

    private fun common(language: CodeLanguage): List<Pair<String, String>> = when (language) {
        CodeLanguage.KOTLIN -> listOf("println" to "standard output", "listOf" to "immutable list", "mutableListOf" to "mutable list", "require" to "precondition", "runCatching" to "exception wrapper")
        CodeLanguage.JAVA -> listOf("System.out.println" to "standard output", "Objects.requireNonNull" to "null guard", "List.of" to "immutable list")
        CodeLanguage.JAVASCRIPT, CodeLanguage.TYPESCRIPT -> listOf("console.log" to "debug output", "Promise.all" to "parallel promises", "JSON.stringify" to "serialize JSON")
        CodeLanguage.PYTHON -> listOf("print" to "standard output", "len" to "length", "enumerate" to "indexed iteration", "dataclass" to "data class decorator")
        else -> emptyList()
    }
}
