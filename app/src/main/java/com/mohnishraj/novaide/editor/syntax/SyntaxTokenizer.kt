package com.mohnishraj.novaide.editor.syntax

enum class SyntaxKind {
    COMMENT, STRING, NUMBER, KEYWORD, TYPE, FUNCTION, ANNOTATION, TAG, ATTRIBUTE, HEADING
}

data class HighlightToken(val start: Int, val endExclusive: Int, val kind: SyntaxKind)

data class TokenizationResult(
    val tokens: List<HighlightToken>,
    val truncated: Boolean
)

object SyntaxTokenizer {
    const val MAX_HIGHLIGHT_CHARS = 320_000
    private const val MAX_TOKENS = 25_000

    private val commonNumbers = Regex("(?<![A-Za-z0-9_])(?:0[xX][0-9A-Fa-f_]+|0[bB][01_]+|\\d(?:[\\d_]*\\.?[\\d_]*)(?:[eE][+-]?\\d+)?)")
    private val annotations = Regex("@[A-Za-z_][A-Za-z0-9_.]*")
    private val functionCalls = Regex("\\b([A-Za-z_$][A-Za-z0-9_$]*)\\s*(?=\\()")

    fun tokenize(source: String, language: CodeLanguage): TokenizationResult {
        if (language == CodeLanguage.PLAIN_TEXT || source.isEmpty()) return TokenizationResult(emptyList(), false)
        val truncated = source.length > MAX_HIGHLIGHT_CHARS
        val text = if (truncated) source.substring(0, MAX_HIGHLIGHT_CHARS) else source
        val protected = BooleanArray(text.length)
        val tokens = ArrayList<HighlightToken>()

        fun addLexical(regex: Regex) {
            if (tokens.size >= MAX_TOKENS) return
            regex.findAll(text).forEach { match ->
                if (tokens.size >= MAX_TOKENS) return@forEach
                val start = match.range.first
                val end = match.range.last + 1
                if (start >= end || start !in protected.indices) return@forEach
                val kind = if (match.groups[1] != null) SyntaxKind.COMMENT else SyntaxKind.STRING
                for (index in start until end.coerceAtMost(protected.size)) protected[index] = true
                tokens += HighlightToken(start, end, kind)
            }
        }

        fun addSimple(
            regex: Regex,
            kind: SyntaxKind,
            group: Int = 0,
            excluded: Set<String> = emptySet()
        ) {
            if (tokens.size >= MAX_TOKENS) return
            regex.findAll(text).forEach { match ->
                if (tokens.size >= MAX_TOKENS) return@forEach
                val selected = match.groups[group] ?: return@forEach
                if (selected.value in excluded) return@forEach
                val range = selected.range
                val start = range.first
                val end = range.last + 1
                if (start >= end || start !in protected.indices) return@forEach
                var blocked = false
                for (index in start until end.coerceAtMost(protected.size)) {
                    if (protected[index]) {
                        blocked = true
                        break
                    }
                }
                if (!blocked) tokens += HighlightToken(start, end, kind)
            }
        }

        val lexicalRegex = when (language) {
            CodeLanguage.PYTHON -> Regex("(?ms)(#.*?$)|((?:[rRuUbBfF]{0,2})(?:\\\"\\\"\\\".*?\\\"\\\"\\\"|'''.*?'''|\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|'(?:\\\\.|[^'\\\\])*'))")
            CodeLanguage.SHELL, CodeLanguage.YAML -> Regex("(?m)(#.*?$)|(\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|'(?:\\\\.|[^'\\\\])*')")
            CodeLanguage.HTML, CodeLanguage.XML -> Regex("(?s)(<!--[\\s\\S]*?-->)|(\\\"[^\\\"]*\\\"|'[^']*')")
            CodeLanguage.SQL -> Regex("(?ms)(--.*?$|/\\*.*?\\*/)|(\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|'(?:\\\\.|[^'\\\\])*')")
            CodeLanguage.JAVASCRIPT, CodeLanguage.TYPESCRIPT -> Regex("(?ms)(//.*?$|/\\*.*?\\*/)|(\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|'(?:\\\\.|[^'\\\\])*'|`(?:\\\\.|[^`\\\\])*`)")
            else -> Regex("(?ms)(//.*?$|/\\*.*?\\*/)|(\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|'(?:\\\\.|[^'\\\\])*')")
        }
        addLexical(lexicalRegex)

        if (language in setOf(CodeLanguage.KOTLIN, CodeLanguage.JAVA, CodeLanguage.CSHARP, CodeLanguage.DART)) {
            addSimple(annotations, SyntaxKind.ANNOTATION)
        }
        addSimple(commonNumbers, SyntaxKind.NUMBER)

        val keywords = keywordSet(language)
        if (keywords.isNotEmpty()) {
            addSimple(wordRegex(keywords, ignoreCase = language == CodeLanguage.SQL), SyntaxKind.KEYWORD)
        }
        val types = typeSet(language)
        if (types.isNotEmpty()) {
            addSimple(wordRegex(types), SyntaxKind.TYPE)
        }

        when (language) {
            CodeLanguage.HTML, CodeLanguage.XML -> {
                addSimple(Regex("</?([A-Za-z][A-Za-z0-9:_-]*)"), SyntaxKind.TAG, 1)
                addSimple(Regex("\\s([A-Za-z_:][A-Za-z0-9:_.-]*)\\s*(?==)"), SyntaxKind.ATTRIBUTE, 1)
            }
            CodeLanguage.CSS -> {
                addSimple(Regex("(?m)^\\s*([^@\\s][^{]+)(?=\\s*\\{)"), SyntaxKind.TAG, 1)
                addSimple(Regex("(?m)([A-Za-z-]+)\\s*(?=:)"), SyntaxKind.ATTRIBUTE, 1)
            }
            CodeLanguage.MARKDOWN -> {
                addSimple(Regex("(?m)^\\s{0,3}(#{1,6}\\s+.+)$"), SyntaxKind.HEADING, 1)
            }
            else -> addSimple(functionCalls, SyntaxKind.FUNCTION, 1, excluded = keywords)
        }

        return TokenizationResult(tokens.sortedBy { it.start }, truncated)
    }

    private fun wordRegex(words: Set<String>, ignoreCase: Boolean = false): Regex {
        val body = words.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
        val options = if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
        return Regex("\\b(?:$body)\\b", options)
    }

    private fun keywordSet(language: CodeLanguage): Set<String> = when (language) {
        CodeLanguage.KOTLIN -> setOf("as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is", "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof", "val", "var", "when", "while", "by", "catch", "constructor", "delegate", "dynamic", "field", "file", "finally", "get", "import", "init", "param", "property", "receiver", "set", "setparam", "where", "actual", "abstract", "annotation", "companion", "const", "crossinline", "data", "enum", "expect", "external", "final", "infix", "inline", "inner", "internal", "lateinit", "noinline", "open", "operator", "out", "override", "private", "protected", "public", "reified", "sealed", "suspend", "tailrec", "vararg")
        CodeLanguage.JAVA -> setOf("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null", "record", "sealed", "permits", "yield")
        CodeLanguage.JAVASCRIPT, CodeLanguage.TYPESCRIPT -> setOf("async", "await", "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else", "export", "extends", "false", "finally", "for", "from", "function", "get", "if", "import", "in", "instanceof", "let", "new", "null", "of", "return", "set", "static", "super", "switch", "this", "throw", "true", "try", "typeof", "undefined", "var", "void", "while", "with", "yield", "interface", "type", "enum", "implements", "namespace", "private", "protected", "public", "readonly", "declare", "keyof", "infer", "unknown", "never")
        CodeLanguage.PYTHON -> setOf("and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif", "else", "except", "False", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "None", "nonlocal", "not", "or", "pass", "raise", "return", "True", "try", "while", "with", "yield", "match", "case")
        CodeLanguage.CSS -> setOf("important", "inherit", "initial", "unset", "revert", "none", "auto")
        CodeLanguage.JSON -> setOf("true", "false", "null")
        CodeLanguage.SQL -> setOf("SELECT", "FROM", "WHERE", "INSERT", "INTO", "UPDATE", "DELETE", "CREATE", "TABLE", "VIEW", "DROP", "ALTER", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "ON", "AS", "AND", "OR", "NOT", "NULL", "VALUES", "SET", "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "OFFSET", "DISTINCT", "UNION", "ALL", "CASE", "WHEN", "THEN", "ELSE", "END", "WITH")
        CodeLanguage.C_FAMILY -> setOf("auto", "break", "case", "char", "class", "const", "continue", "default", "delete", "do", "double", "else", "enum", "explicit", "extern", "false", "float", "for", "friend", "if", "inline", "int", "long", "namespace", "new", "nullptr", "operator", "private", "protected", "public", "register", "return", "short", "signed", "sizeof", "static", "struct", "switch", "template", "this", "throw", "true", "try", "typedef", "typename", "union", "unsigned", "using", "virtual", "void", "volatile", "while")
        CodeLanguage.CSHARP -> setOf("abstract", "as", "async", "await", "base", "bool", "break", "byte", "case", "catch", "char", "checked", "class", "const", "continue", "decimal", "default", "delegate", "do", "double", "else", "enum", "event", "explicit", "extern", "false", "finally", "fixed", "float", "for", "foreach", "get", "goto", "if", "implicit", "in", "int", "interface", "internal", "is", "lock", "long", "namespace", "new", "null", "object", "operator", "out", "override", "params", "private", "protected", "public", "readonly", "record", "ref", "return", "sbyte", "sealed", "set", "short", "sizeof", "stackalloc", "static", "string", "struct", "switch", "this", "throw", "true", "try", "typeof", "uint", "ulong", "unchecked", "unsafe", "ushort", "using", "var", "virtual", "void", "volatile", "while")
        CodeLanguage.GO -> setOf("break", "case", "chan", "const", "continue", "default", "defer", "else", "fallthrough", "for", "func", "go", "goto", "if", "import", "interface", "map", "package", "range", "return", "select", "struct", "switch", "type", "var", "true", "false", "nil")
        CodeLanguage.RUST -> setOf("as", "async", "await", "break", "const", "continue", "crate", "dyn", "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in", "let", "loop", "match", "mod", "move", "mut", "pub", "ref", "return", "self", "Self", "static", "struct", "super", "trait", "true", "type", "unsafe", "use", "where", "while")
        CodeLanguage.DART -> setOf("abstract", "as", "assert", "async", "await", "break", "case", "catch", "class", "const", "continue", "covariant", "default", "deferred", "do", "dynamic", "else", "enum", "export", "extends", "extension", "external", "factory", "false", "final", "finally", "for", "function", "get", "hide", "if", "implements", "import", "in", "interface", "is", "late", "library", "mixin", "new", "null", "on", "operator", "part", "required", "rethrow", "return", "set", "show", "static", "super", "switch", "sync", "this", "throw", "true", "try", "typedef", "var", "void", "while", "with", "yield")
        CodeLanguage.SHELL -> setOf("if", "then", "else", "elif", "fi", "for", "while", "in", "do", "done", "case", "esac", "function", "select", "until", "time")
        CodeLanguage.YAML -> setOf("true", "false", "null", "yes", "no", "on", "off")
        else -> emptySet()
    }

    private fun typeSet(language: CodeLanguage): Set<String> = when (language) {
        CodeLanguage.KOTLIN -> setOf("Any", "Boolean", "Byte", "Char", "Double", "Float", "Int", "Long", "Nothing", "Short", "String", "Unit", "Array", "List", "Map", "Set", "MutableList", "MutableMap", "MutableSet")
        CodeLanguage.JAVA -> setOf("Boolean", "Byte", "Character", "Double", "Float", "Integer", "Long", "Object", "Short", "String", "StringBuilder", "List", "Map", "Set", "Optional")
        CodeLanguage.JAVASCRIPT, CodeLanguage.TYPESCRIPT -> setOf("Array", "BigInt", "Boolean", "Date", "Error", "Function", "Map", "Number", "Object", "Promise", "RegExp", "Set", "String", "Symbol")
        CodeLanguage.PYTHON -> setOf("bool", "bytes", "dict", "float", "frozenset", "int", "list", "object", "set", "str", "tuple")
        CodeLanguage.GO -> setOf("bool", "byte", "complex64", "complex128", "error", "float32", "float64", "int", "int8", "int16", "int32", "int64", "rune", "string", "uint", "uint8", "uint16", "uint32", "uint64", "uintptr")
        CodeLanguage.RUST -> setOf("bool", "char", "f32", "f64", "i8", "i16", "i32", "i64", "i128", "isize", "str", "String", "u8", "u16", "u32", "u64", "u128", "usize", "Vec", "Option", "Result")
        CodeLanguage.DART -> setOf("bool", "double", "dynamic", "int", "num", "Object", "String", "List", "Map", "Set", "Future", "Stream")
        else -> emptySet()
    }
}
