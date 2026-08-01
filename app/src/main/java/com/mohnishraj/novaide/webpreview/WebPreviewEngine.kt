package com.mohnishraj.novaide.webpreview

import java.util.ArrayDeque
import java.util.Locale

enum class WebPreviewKind {
    STATIC_SITE,
    BUILD_OUTPUT,
    TOOLING_SOURCE,
    NO_ENTRY
}

enum class PreviewViewport {
    RESPONSIVE,
    MOBILE,
    DESKTOP
}

data class WebPreviewPlan(
    val entryPath: String?,
    val candidates: List<String>,
    val kind: WebPreviewKind,
    val warning: String? = null
) {
    val canRun: Boolean get() = entryPath != null
}

object WebPreviewEngine {
    const val HOST = "nova.local"
    const val MAX_HTML_CHARS = 4_000_000

    private val htmlExtensions = setOf("html", "htm")
    private val previewTextExtensions = setOf(
        "html", "htm", "css", "js", "mjs", "cjs", "json", "map", "svg", "xml", "txt", "webmanifest"
    )
    private val blockedFileNames = setOf(
        ".env", ".env.local", ".env.production", ".env.development", ".npmrc", ".netrc",
        "local.properties", "gradle.properties", "key.properties", "google-services.json",
        "credentials.json", "service-account.json"
    )
    private val blockedExtensions = setOf("jks", "keystore", "p12", "pfx", "pem", "key")
    private val blockedSegments = setOf(".git", ".gradle", ".idea", "credentials", "secrets")

    fun plan(paths: Collection<String>, preferredPath: String? = null): WebPreviewPlan {
        val normalized = paths.mapNotNull(::normalizePath).distinct()
        val files = normalized.filterNot { it.endsWith('/') }
        val html = files.filter(::isHtml).sortedWith(compareBy<String> { candidateRank(it) }.thenBy { it.lowercase(Locale.ROOT) })
        val preferred = preferredPath?.let(::normalizePath)?.takeIf { it in html }
        val candidates = buildList {
            if (preferred != null) add(preferred)
            html.filterNot { it == preferred }.forEach(::add)
        }
        val buildOutput = candidates.firstOrNull { lower(it).startsWith("dist/") || lower(it).startsWith("build/") || lower(it).startsWith("out/") }
        val tooling = hasTooling(normalized)
        val selected = preferred ?: buildOutput ?: candidates.firstOrNull()
        return when {
            selected == null && tooling -> WebPreviewPlan(
                entryPath = null,
                candidates = emptyList(),
                kind = WebPreviewKind.NO_ENTRY,
                warning = "This project uses a build tool but no generated HTML output was found. Run its build workflow first, then preview dist/, build/, or out/."
            )
            selected == null -> WebPreviewPlan(
                entryPath = null,
                candidates = emptyList(),
                kind = WebPreviewKind.NO_ENTRY,
                warning = "No HTML entry file was found in this workspace."
            )
            buildOutput != null && selected == buildOutput -> WebPreviewPlan(selected, candidates, WebPreviewKind.BUILD_OUTPUT)
            tooling -> WebPreviewPlan(
                selected,
                candidates,
                WebPreviewKind.TOOLING_SOURCE,
                warning = "This looks like a Vite/React/Next/Angular/Webpack source project. NovaIDE can serve existing browser-ready files. Use Universal Runtime to build or start this tooling project through the user-approved Termux bridge."
            )
            else -> WebPreviewPlan(selected, candidates, WebPreviewKind.STATIC_SITE)
        }
    }

    fun normalizePath(raw: String): String? {
        val value = raw.replace('\\', '/').trim().trimStart('/')
        if (value.isBlank()) return ""
        if (value.indexOf('\u0000') >= 0 || value.length > 2_048) return null
        val output = ArrayDeque<String>()
        for (part in value.split('/')) {
            when {
                part.isBlank() || part == "." -> Unit
                part == ".." -> return null
                part.any { it.code < 32 } -> return null
                else -> output.addLast(part)
            }
        }
        return output.joinToString("/")
    }

    fun isHtml(path: String): Boolean = extension(path) in htmlExtensions

    fun isPreviewText(path: String): Boolean = extension(path) in previewTextExtensions

    fun isSensitive(path: String): Boolean {
        val normalized = normalizePath(path) ?: return true
        val segments = normalized.split('/').filter { it.isNotBlank() }
        val name = segments.lastOrNull()?.lowercase(Locale.ROOT).orEmpty()
        return name in blockedFileNames || name.startsWith(".env.") || name.contains("service-account") ||
            extension(name) in blockedExtensions || segments.any { it.lowercase(Locale.ROOT) in blockedSegments }
    }

    fun shouldSpaFallback(path: String): Boolean {
        val normalized = normalizePath(path) ?: return false
        val last = normalized.substringAfterLast('/', normalized)
        return normalized.isNotBlank() && !last.contains('.') && !isSensitive(normalized)
    }

    fun mimeType(path: String, fallback: String? = null): String = when (extension(path)) {
        "html", "htm" -> "text/html"
        "css" -> "text/css"
        "js", "mjs", "cjs" -> "application/javascript"
        "json", "map" -> "application/json"
        "svg" -> "image/svg+xml"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "avif" -> "image/avif"
        "ico" -> "image/x-icon"
        "woff" -> "font/woff"
        "woff2" -> "font/woff2"
        "ttf" -> "font/ttf"
        "otf" -> "font/otf"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        "wasm" -> "application/wasm"
        "xml" -> "application/xml"
        "webmanifest" -> "application/manifest+json"
        "txt" -> "text/plain"
        else -> fallback?.takeIf { it.isNotBlank() && it != "application/octet-stream" } ?: "application/octet-stream"
    }

    fun injectDiagnostics(html: String): String {
        if (html.length > MAX_HTML_CHARS || html.contains("data-nova-preview-runtime")) return html
        val runtime = """
            <script data-nova-preview-runtime>
            (() => {
              const emit = (kind, value) => {
                try { console[kind]('[Nova Preview] ' + value); } catch (_) {}
              };
              window.addEventListener('error', event => {
                const at = event.filename ? ` @ ${'$'}{event.filename}:${'$'}{event.lineno || 0}` : '';
                emit('error', `${'$'}{event.message || 'Runtime error'}${'$'}{at}`);
              });
              window.addEventListener('unhandledrejection', event => {
                const reason = event.reason && (event.reason.stack || event.reason.message || String(event.reason));
                emit('error', `Unhandled promise rejection: ${'$'}{reason || 'Unknown reason'}`);
              });
            })();
            </script>
        """.trimIndent()
        val body = Regex("</body\\s*>", RegexOption.IGNORE_CASE).find(html)
        if (body != null) return html.substring(0, body.range.first) + runtime + "\n" + html.substring(body.range.first)
        val head = Regex("</head\\s*>", RegexOption.IGNORE_CASE).find(html)
        if (head != null) return html.substring(0, head.range.first) + runtime + "\n" + html.substring(head.range.first)
        return "$html\n$runtime"
    }


    fun runtimeOrigin(rawUrl: String): String? {
        val uri = runCatching { java.net.URI(rawUrl.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
        if (scheme !in setOf("http", "https") || uri.userInfo != null || uri.fragment != null) return null
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        if (host !in setOf("127.0.0.1", "localhost", "::1")) return null
        val port = if (uri.port >= 0) uri.port else if (scheme == "https") 443 else 80
        if (port !in 1..65535) return null
        val displayHost = if (host.contains(':')) "[$host]" else host
        return "$scheme://$displayHost:$port"
    }

    fun isAllowedRuntimeUrl(rawUrl: String, allowedOrigin: String?): Boolean {
        val allowed = allowedOrigin?.let(::runtimeOrigin) ?: return false
        val actual = runtimeOrigin(rawUrl) ?: return false
        return actual == allowed
    }

    fun localUrl(path: String): String {
        val normalized = normalizePath(path) ?: ""
        val encoded = normalized.split('/').filter { it.isNotBlank() }.joinToString("/") { encodeSegment(it) }
        return "https://$HOST/${encoded}"
    }

    private fun hasTooling(paths: Collection<String>): Boolean {
        val names = paths.map(::lower)
        return names.any {
            it == "package.json" || it.startsWith("vite.config.") || it.startsWith("next.config.") ||
                it == "angular.json" || it.startsWith("webpack.config.") || it == "astro.config.mjs" ||
                it == "svelte.config.js" || it == "nuxt.config.ts"
        }
    }

    private fun candidateRank(path: String): Int {
        val p = lower(path)
        return when {
            p == "index.html" -> 0
            p == "dist/index.html" -> 1
            p == "build/index.html" -> 2
            p == "out/index.html" -> 3
            p == "public/index.html" -> 4
            p.endsWith("/index.html") -> 10 + p.count { it == '/' }
            else -> 30 + p.count { it == '/' }
        }
    }

    private fun extension(path: String): String = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
    private fun lower(value: String): String = value.lowercase(Locale.ROOT)
    private fun encodeSegment(value: String): String = buildString {
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            val number = byte.toInt() and 0xff
            val safe = number in 'a'.code..'z'.code || number in 'A'.code..'Z'.code || number in '0'.code..'9'.code || number in listOf('-'.code, '_'.code, '.'.code, '~'.code)
            if (safe) append(number.toChar()) else append('%').append(number.toString(16).uppercase(Locale.ROOT).padStart(2, '0'))
        }
    }
}
