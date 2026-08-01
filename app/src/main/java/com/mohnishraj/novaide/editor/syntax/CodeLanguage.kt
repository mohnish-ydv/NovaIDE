package com.mohnishraj.novaide.editor.syntax

enum class CodeLanguage(val label: String) {
    KOTLIN("Kotlin"),
    JAVA("Java"),
    JAVASCRIPT("JavaScript"),
    TYPESCRIPT("TypeScript"),
    PYTHON("Python"),
    HTML("HTML"),
    CSS("CSS"),
    JSON("JSON"),
    XML("XML"),
    MARKDOWN("Markdown"),
    SHELL("Shell"),
    SQL("SQL"),
    C_FAMILY("C/C++"),
    CSHARP("C#"),
    GO("Go"),
    RUST("Rust"),
    DART("Dart"),
    YAML("YAML"),
    PLAIN_TEXT("Plain text")
}

object LanguageDetector {
    fun fromFileName(fileName: String): CodeLanguage {
        val lower = fileName.lowercase()
        val extension = lower.substringAfterLast('.', "")
        return when {
            lower == "dockerfile" || extension in setOf("sh", "bash", "zsh") -> CodeLanguage.SHELL
            lower == "makefile" -> CodeLanguage.PLAIN_TEXT
            extension in setOf("kt", "kts", "gradle") -> CodeLanguage.KOTLIN
            extension == "java" -> CodeLanguage.JAVA
            extension in setOf("js", "mjs", "cjs", "jsx") -> CodeLanguage.JAVASCRIPT
            extension in setOf("ts", "tsx") -> CodeLanguage.TYPESCRIPT
            extension == "py" -> CodeLanguage.PYTHON
            extension in setOf("html", "htm") -> CodeLanguage.HTML
            extension in setOf("css", "scss", "sass", "less") -> CodeLanguage.CSS
            extension == "json" -> CodeLanguage.JSON
            extension == "xml" -> CodeLanguage.XML
            extension in setOf("md", "markdown") -> CodeLanguage.MARKDOWN
            extension == "sql" -> CodeLanguage.SQL
            extension in setOf("c", "h", "cpp", "hpp", "cc") -> CodeLanguage.C_FAMILY
            extension == "cs" -> CodeLanguage.CSHARP
            extension == "go" -> CodeLanguage.GO
            extension == "rs" -> CodeLanguage.RUST
            extension == "dart" -> CodeLanguage.DART
            extension in setOf("yaml", "yml") -> CodeLanguage.YAML
            else -> CodeLanguage.PLAIN_TEXT
        }
    }
}
