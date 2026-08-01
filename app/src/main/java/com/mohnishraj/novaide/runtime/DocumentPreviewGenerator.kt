package com.mohnishraj.novaide.runtime

object DocumentPreviewGenerator {
    const val MAX_SOURCE_CHARS = 1_000_000

    fun markdown(title: String, source: String): String {
        require(source.length <= MAX_SOURCE_CHARS) { "Markdown is larger than 1 MB" }
        val body = buildString {
            var inCode = false
            val paragraph = mutableListOf<String>()
            fun flushParagraph() {
                if (paragraph.isNotEmpty()) {
                    append("<p>").append(inline(paragraph.joinToString(" "))).append("</p>\n")
                    paragraph.clear()
                }
            }
            source.replace("\r\n", "\n").lineSequence().forEach { raw ->
                val line = raw.trimEnd()
                if (line.trimStart().startsWith("```")) {
                    flushParagraph(); inCode = !inCode
                    append(if (inCode) "<pre><code>" else "</code></pre>\n")
                } else if (inCode) {
                    append(escape(line)).append('\n')
                } else when {
                    line.isBlank() -> flushParagraph()
                    line.startsWith("### ") -> { flushParagraph(); append("<h3>").append(inline(line.drop(4))).append("</h3>\n") }
                    line.startsWith("## ") -> { flushParagraph(); append("<h2>").append(inline(line.drop(3))).append("</h2>\n") }
                    line.startsWith("# ") -> { flushParagraph(); append("<h1>").append(inline(line.drop(2))).append("</h1>\n") }
                    line.startsWith("> ") -> { flushParagraph(); append("<blockquote>").append(inline(line.drop(2))).append("</blockquote>\n") }
                    line.matches(Regex("^[-*+]\\s+.*")) -> { flushParagraph(); append("<ul><li>").append(inline(line.drop(2))).append("</li></ul>\n") }
                    line.matches(Regex("^\\d+[.]\\s+.*")) -> { flushParagraph(); append("<ol><li>").append(inline(line.substringAfter(' '))).append("</li></ol>\n") }
                    line == "---" || line == "***" -> { flushParagraph(); append("<hr>\n") }
                    else -> paragraph += line.trim()
                }
            }
            flushParagraph()
            if (inCode) append("</code></pre>\n")
        }.replace("</ul>\n<ul>", "").replace("</ol>\n<ol>", "")
        return shell(title, body)
    }

    fun mermaid(title: String, source: String): String {
        require(source.length <= MAX_SOURCE_CHARS) { "Mermaid document is larger than 1 MB" }
        val body = """
            <div class="notice">Mermaid rendering uses the official browser module from an external HTTPS CDN. Enable external preview resources before running this document.</div>
            <pre class="mermaid">${escape(source)}</pre>
            <script type="module">
              import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs';
              mermaid.initialize({ startOnLoad: true, securityLevel: 'strict', theme: 'default' });
            </script>
        """.trimIndent()
        return shell(title, body)
    }

    private fun shell(title: String, body: String): String = """
        <!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
        <title>${escape(title)}</title><style>
        :root{color-scheme:light dark}body{font:16px/1.65 system-ui,sans-serif;max-width:900px;margin:auto;padding:24px;background:#fff;color:#17202a}
        pre{overflow:auto;padding:16px;border-radius:12px;background:#111827;color:#e5e7eb}code{font-family:ui-monospace,monospace}
        blockquote{border-left:4px solid #6c63ff;margin-left:0;padding-left:16px;color:#5f6470}.notice{padding:12px;border:1px solid #f59e0b;border-radius:10px;background:#fffbeb;color:#78350f}
        a{color:#4f46e5}@media(prefers-color-scheme:dark){body{background:#111318;color:#e6e8ee}.notice{background:#2b2110;color:#fde68a}}
        </style></head><body>$body</body></html>
    """.trimIndent()

    private fun inline(value: String): String {
        var output = escape(value)
        output = output.replace(Regex("`([^`]+)`")) { "<code>${it.groupValues[1]}</code>" }
        output = output.replace(Regex("\\*\\*([^*]+)\\*\\*")) { "<strong>${it.groupValues[1]}</strong>" }
        output = output.replace(Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)")) { "<em>${it.groupValues[1]}</em>" }
        output = output.replace(Regex("\\[([^]]+)]\\((https?://[^)]+)\\)")) { match ->
            "<a href=\"${escapeAttribute(match.groupValues[2])}\" rel=\"noreferrer\">${match.groupValues[1]}</a>"
        }
        return output
    }

    private fun escape(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    private fun escapeAttribute(value: String): String = escape(value).replace("'", "&#39;")
}
