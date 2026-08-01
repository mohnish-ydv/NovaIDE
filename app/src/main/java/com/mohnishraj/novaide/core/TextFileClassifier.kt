package com.mohnishraj.novaide.core

object TextFileClassifier {
    private val textExtensions = setOf(
        "txt", "md", "markdown", "kt", "kts", "java", "xml", "json", "yaml", "yml",
        "html", "htm", "css", "scss", "sass", "less", "js", "mjs", "cjs", "ts", "tsx",
        "jsx", "py", "rb", "php", "c", "h", "cpp", "hpp", "cc", "cs", "go", "rs", "swift",
        "dart", "gradle", "properties", "toml", "ini", "cfg", "conf", "sh", "bash", "zsh",
        "bat", "ps1", "sql", "graphql", "gql", "vue", "svelte", "lua", "r", "gitignore",
        "dockerfile", "makefile", "pro", "csv", "log", "svg"
    )

    fun isProbablyText(name: String, mimeType: String): Boolean {
        if (mimeType.startsWith("text/")) return true
        if (mimeType in setOf("application/json", "application/xml", "application/javascript")) return true
        val lower = name.lowercase()
        if (lower in setOf("dockerfile", "makefile", "gradle.properties", ".gitignore", ".env")) return true
        val extension = lower.substringAfterLast('.', "")
        return extension in textExtensions
    }
}
