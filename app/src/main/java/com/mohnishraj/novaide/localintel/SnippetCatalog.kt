package com.mohnishraj.novaide.localintel

import com.mohnishraj.novaide.editor.syntax.CodeLanguage
import com.mohnishraj.novaide.editor.syntax.LanguageDetector

data class CodeSnippet(val id: String, val title: String, val trigger: String, val template: String)
data class ExpandedSnippet(val text: String, val cursorOffset: Int)

object SnippetCatalog {
    private const val CURSOR = "__NOVA_CURSOR__"

    fun forFile(fileName: String): List<CodeSnippet> = snippets(LanguageDetector.fromFileName(fileName))

    fun expand(snippet: CodeSnippet, indent: String): ExpandedSnippet {
        val raw = snippet.template.trimIndent()
        val indented = raw.lineSequence().mapIndexed { index, line -> if (index == 0 || line.isBlank()) line else indent + line }.joinToString("\n")
        val cursor = indented.indexOf(CURSOR).let { if (it < 0) indented.length else it }
        return ExpandedSnippet(indented.replace(CURSOR, ""), cursor)
    }

    private fun snippets(language: CodeLanguage): List<CodeSnippet> = when (language) {
        CodeLanguage.KOTLIN -> listOf(
            CodeSnippet("kt.fun", "Function", "fun", "fun name() {\n    $CURSOR\n}"),
            CodeSnippet("kt.class", "Data class", "data", "data class Name(\n    val value: String\n)$CURSOR"),
            CodeSnippet("kt.when", "When expression", "when", "when (value) {\n    else -> $CURSOR\n}"),
            CodeSnippet("kt.try", "Try / catch", "try", "try {\n    $CURSOR\n} catch (error: Exception) {\n    // Handle error\n}"),
            CodeSnippet("kt.coroutine", "Suspend function", "sfun", "suspend fun name() {\n    $CURSOR\n}")
        )
        CodeLanguage.JAVA -> listOf(
            CodeSnippet("java.method", "Method", "method", "private void name() {\n    $CURSOR\n}"),
            CodeSnippet("java.class", "Class", "class", "public final class Name {\n    $CURSOR\n}"),
            CodeSnippet("java.try", "Try / catch", "try", "try {\n    $CURSOR\n} catch (Exception error) {\n    // Handle error\n}")
        )
        CodeLanguage.JAVASCRIPT, CodeLanguage.TYPESCRIPT -> listOf(
            CodeSnippet("js.function", "Arrow function", "fn", "const name = async () => {\n    $CURSOR\n};"),
            CodeSnippet("js.try", "Async try / catch", "try", "try {\n    $CURSOR\n} catch (error) {\n    console.error(error);\n}"),
            CodeSnippet("js.fetch", "Fetch JSON", "fetch", "const response = await fetch(url);\nif (!response.ok) throw new Error(`HTTP ${'$'}{response.status}`);\nconst data = await response.json();\n$CURSOR")
        )
        CodeLanguage.PYTHON -> listOf(
            CodeSnippet("py.function", "Function", "def", "def name():\n    $CURSOR"),
            CodeSnippet("py.main", "Main guard", "main", "def main():\n    $CURSOR\n\n\nif __name__ == \"__main__\":\n    main()"),
            CodeSnippet("py.try", "Try / except", "try", "try:\n    $CURSOR\nexcept Exception as error:\n    raise RuntimeError(\"Operation failed\") from error")
        )
        CodeLanguage.HTML -> listOf(
            CodeSnippet("html.doc", "HTML document", "html", "<!doctype html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"utf-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n    <title>Document</title>\n</head>\n<body>\n    $CURSOR\n</body>\n</html>"),
            CodeSnippet("html.section", "Accessible section", "section", "<section aria-labelledby=\"section-title\">\n    <h2 id=\"section-title\">Title</h2>\n    $CURSOR\n</section>")
        )
        CodeLanguage.CSS -> listOf(
            CodeSnippet("css.media", "Responsive media query", "media", "@media (max-width: 768px) {\n    $CURSOR\n}"),
            CodeSnippet("css.flex", "Flex container", "flex", "display: flex;\nalign-items: center;\njustify-content: space-between;\ngap: 1rem;\n$CURSOR")
        )
        CodeLanguage.JSON -> listOf(CodeSnippet("json.object", "JSON object", "obj", "{\n    \"key\": \"$CURSOR\"\n}"))
        CodeLanguage.XML -> listOf(CodeSnippet("xml.tag", "XML element", "tag", "<element>\n    $CURSOR\n</element>"))
        CodeLanguage.SHELL -> listOf(CodeSnippet("sh.safe", "Safe shell header", "safe", "#!/usr/bin/env bash\nset -euo pipefail\n\n$CURSOR"))
        CodeLanguage.SQL -> listOf(
            CodeSnippet("sql.select", "Select query", "select", "SELECT columns\nFROM table_name\nWHERE condition\nORDER BY column;\n$CURSOR"),
            CodeSnippet("sql.transaction", "Transaction", "tx", "BEGIN;\n\n$CURSOR\n\nCOMMIT;")
        )
        CodeLanguage.GO -> listOf(CodeSnippet("go.error", "Error guard", "iferr", "if err != nil {\n    return err\n}\n$CURSOR"))
        CodeLanguage.RUST -> listOf(CodeSnippet("rs.match", "Result match", "match", "match result {\n    Ok(value) => $CURSOR,\n    Err(error) => return Err(error.into()),\n}"))
        CodeLanguage.DART -> listOf(CodeSnippet("dart.future", "Async function", "future", "Future<void> name() async {\n    $CURSOR\n}"))
        CodeLanguage.C_FAMILY, CodeLanguage.CSHARP -> listOf(CodeSnippet("c.function", "Function", "fn", "void name() {\n    $CURSOR\n}"))
        CodeLanguage.YAML -> listOf(CodeSnippet("yaml.item", "YAML mapping", "map", "name:\n  key: $CURSOR"))
        else -> emptyList()
    }
}
