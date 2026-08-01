package com.mohnishraj.novaide.webpreview

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.mohnishraj.novaide.files.FileRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class WorkspaceWebServer(private val repository: FileRepository) {
    data class Resource(val path: String, val uri: Uri, val mimeType: String, val size: Long)

    companion object {
        const val MAX_TEXT_BYTES = 6L * 1024L * 1024L
        const val MAX_RESOURCE_BYTES = 100L * 1024L * 1024L
    }

    @Volatile private var resources: Map<String, Resource> = emptyMap()
    @Volatile private var textOverrides: Map<String, String> = emptyMap()
    @Volatile var entryPath: String = ""
    @Volatile var allowExternalNetwork: Boolean = false
    @Volatile var spaFallback: Boolean = true
    @Volatile var allowedRuntimeOrigin: String? = null

    fun updateWorkspace(entries: List<FileRepository.WorkspaceEntry>) {
        resources = entries.asSequence()
            .filterNot { it.node.isDirectory }
            .mapNotNull { entry ->
                val path = WebPreviewEngine.normalizePath(entry.relativePath) ?: return@mapNotNull null
                path to Resource(path, entry.node.uri, entry.node.mimeType, entry.node.size)
            }
            .toMap()
    }

    fun updateOverrides(overrides: Map<String, String>) {
        textOverrides = overrides.mapNotNull { (path, content) ->
            WebPreviewEngine.normalizePath(path)?.let { it to content }
        }.toMap()
    }

    fun hasPath(path: String): Boolean = WebPreviewEngine.normalizePath(path)?.let(resources::containsKey) == true

    fun resource(path: String): Resource? = WebPreviewEngine.normalizePath(path)?.let(resources::get)

    fun intercept(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url
        if (!url.host.equals(WebPreviewEngine.HOST, ignoreCase = true)) {
            val rawUrl = url.toString()
            val allowedRuntime = WebPreviewEngine.isAllowedRuntimeUrl(rawUrl, allowedRuntimeOrigin)
            val externalHttps = url.scheme.equals("https", ignoreCase = true) && url.userInfo.isNullOrBlank()
            return if (allowedRuntime || allowExternalNetwork && externalHttps) null
            else blocked("External network requests are disabled. Only the explicitly selected loopback runtime or HTTPS resources are allowed.")
        }
        val rawPath = url.pathSegments.joinToString("/")
        var path = WebPreviewEngine.normalizePath(rawPath) ?: return blocked("Unsafe preview path")
        if (path.isBlank()) path = entryPath
        if (path.endsWith('/')) path += "index.html"
        if (WebPreviewEngine.isSensitive(path)) return blocked("Sensitive workspace files are never exposed to Web Preview.")
        var item = resource(path)
        if (item == null && request.isForMainFrame && spaFallback && WebPreviewEngine.shouldSpaFallback(path)) {
            path = entryPath
            item = resource(path)
        }
        item ?: return response(404, "Not Found", "text/plain", "UTF-8", "NovaIDE Web Preview could not find: $path".toByteArray())
        if (item.size > MAX_RESOURCE_BYTES) return response(413, "Payload Too Large", "text/plain", "UTF-8", "Resource exceeds the 100 MB preview limit.".toByteArray())
        return runCatching { load(item) }.getOrElse { error ->
            response(500, "Preview Error", "text/plain", "UTF-8", (error.message ?: "Could not read resource").toByteArray())
        }
    }

    private fun load(item: Resource): WebResourceResponse {
        val key = item.path
        val mime = WebPreviewEngine.mimeType(item.path, item.mimeType)
        val override = textOverrides[key]
        if (override != null && WebPreviewEngine.isPreviewText(item.path)) {
            val content = if (WebPreviewEngine.isHtml(item.path)) WebPreviewEngine.injectDiagnostics(override) else override
            return response(200, "OK", mime, "UTF-8", content.toByteArray(Charsets.UTF_8))
        }
        if (WebPreviewEngine.isPreviewText(item.path)) {
            val input = repository.openInput(item.uri) ?: throw IOException("Could not open ${item.path}")
            val bytes = input.use { readBounded(it, MAX_TEXT_BYTES) }
            val content = bytes.toString(Charsets.UTF_8)
            val output = if (WebPreviewEngine.isHtml(item.path)) WebPreviewEngine.injectDiagnostics(content).toByteArray(Charsets.UTF_8) else bytes
            return response(200, "OK", mime, "UTF-8", output)
        }
        val stream = repository.openInput(item.uri) ?: throw IOException("Could not open ${item.path}")
        return WebResourceResponse(mime, null, 200, "OK", commonHeaders(), BoundedInputStream(stream, MAX_RESOURCE_BYTES))
    }

    private fun blocked(message: String): WebResourceResponse = response(403, "Blocked", "text/plain", "UTF-8", message.toByteArray())

    private fun response(status: Int, reason: String, mime: String, encoding: String?, bytes: ByteArray): WebResourceResponse =
        WebResourceResponse(mime, encoding, status, reason, commonHeaders(), ByteArrayInputStream(bytes))

    private fun commonHeaders(): Map<String, String> = mapOf(
        "Cache-Control" to "no-store, max-age=0",
        "X-Content-Type-Options" to "nosniff",
        "Referrer-Policy" to "no-referrer"
    )

    private class BoundedInputStream(
        private val delegate: java.io.InputStream,
        private val maxBytes: Long
    ) : java.io.InputStream() {
        private var consumed = 0L

        override fun read(): Int {
            val value = delegate.read()
            if (value >= 0) account(1)
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val count = delegate.read(buffer, offset, length)
            if (count > 0) account(count.toLong())
            return count
        }

        override fun skip(count: Long): Long {
            val allowed = (maxBytes - consumed).coerceAtLeast(0L).coerceAtMost(count)
            val skipped = delegate.skip(allowed)
            if (skipped > 0) account(skipped)
            return skipped
        }

        override fun available(): Int = delegate.available()
        override fun close() = delegate.close()

        private fun account(count: Long) {
            consumed += count
            if (consumed > maxBytes) {
                runCatching { delegate.close() }
                throw IOException("Resource exceeds the 100 MB preview limit")
            }
        }
    }

    private fun readBounded(input: java.io.InputStream, maxBytes: Long): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16_384)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) throw IOException("Text resource exceeds the ${maxBytes / (1024 * 1024)} MB preview limit")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
