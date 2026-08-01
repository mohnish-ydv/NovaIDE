package com.mohnishraj.novaide.git

import android.content.Context
import android.net.Uri
import com.mohnishraj.novaide.core.TextFileClassifier
import com.mohnishraj.novaide.files.FileRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class GitSnapshotStore(private val context: Context, private val repository: FileRepository) {
    companion object {
        private const val MANIFEST_VERSION = 1
        private const val MAX_TRACKED_FILES = 6_000
        private const val MAX_HASH_BYTES = 25L * 1024L * 1024L
        private const val MAX_BASELINE_TEXT_BYTES = 512L * 1024L
        private const val MAX_BASELINE_TEXT_TOTAL = 12L * 1024L * 1024L
    }

    data class BaselineEntry(
        val path: String,
        val sha256: String,
        val size: Long,
        val textCache: String?
    )

    data class Baseline(
        val repository: GitHubRepository,
        val commitSha: String?,
        val capturedAt: Long,
        val entries: Map<String, BaselineEntry>
    )

    data class CaptureResult(val files: Int, val cachedTextFiles: Int, val skipped: Int)

    fun load(workspaceUri: Uri): Baseline? {
        val manifest = manifestFile(workspaceUri)
        if (!manifest.isFile) return null
        return runCatching {
            val root = JSONObject(manifest.readText(Charsets.UTF_8))
            if (root.optInt("version") != MANIFEST_VERSION) return null
            val repo = GitHubRepository(
                root.getString("owner"),
                root.getString("repository"),
                root.optString("branch", "main")
            )
            val entries = linkedMapOf<String, BaselineEntry>()
            val array = root.getJSONArray("entries")
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val path = item.getString("path")
                entries[path] = BaselineEntry(
                    path = path,
                    sha256 = item.getString("sha256"),
                    size = item.optLong("size", 0L),
                    textCache = item.optString("textCache", "").ifBlank { null }
                )
            }
            Baseline(
                repository = repo,
                commitSha = root.optString("commitSha", "").ifBlank { null },
                capturedAt = root.optLong("capturedAt", 0L),
                entries = entries
            )
        }.getOrNull()
    }

    @Throws(IOException::class)
    fun capture(
        workspaceUri: Uri,
        remote: GitHubRepository,
        commitSha: String?,
        entries: List<FileRepository.WorkspaceEntry>
    ): CaptureResult {
        val candidates = entries.asSequence()
            .filter { !it.node.isDirectory }
            .filter { shouldTrack(it.relativePath) }
            .take(MAX_TRACKED_FILES)
            .toList()
        val cacheDir = cacheDirectory(workspaceUri).apply {
            deleteRecursively()
            if (!mkdirs() && !isDirectory) throw IOException("Could not create Git baseline cache")
        }
        val output = JSONArray()
        var cachedBytes = 0L
        var cachedFiles = 0
        var skipped = 0
        candidates.forEach { entry ->
            if (Thread.currentThread().isInterrupted) throw IOException("Snapshot cancelled")
            if (entry.node.size > MAX_HASH_BYTES) {
                skipped++
                return@forEach
            }
            val hash = sha256(entry.node.uri)
            var cacheName: String? = null
            if (entry.node.size in 0..MAX_BASELINE_TEXT_BYTES &&
                cachedBytes + entry.node.size <= MAX_BASELINE_TEXT_TOTAL &&
                TextFileClassifier.isProbablyText(entry.node.name, entry.node.mimeType)
            ) {
                val content = runCatching { repository.readText(entry.node.uri, MAX_BASELINE_TEXT_BYTES) }.getOrNull()
                if (content != null) {
                    cacheName = pathHash(entry.relativePath) + ".txt"
                    File(cacheDir, cacheName).writeText(content, Charsets.UTF_8)
                    cachedBytes += entry.node.size.coerceAtLeast(content.length.toLong())
                    cachedFiles++
                }
            }
            output.put(JSONObject().apply {
                put("path", entry.relativePath)
                put("sha256", hash)
                put("size", entry.node.size)
                if (cacheName != null) put("textCache", cacheName)
            })
        }
        val manifest = JSONObject().apply {
            put("version", MANIFEST_VERSION)
            put("owner", remote.owner)
            put("repository", remote.name)
            put("branch", remote.branch)
            put("commitSha", commitSha ?: "")
            put("capturedAt", System.currentTimeMillis())
            put("entries", output)
        }
        val target = manifestFile(workspaceUri)
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, target.name + ".tmp")
        temp.writeText(manifest.toString(), Charsets.UTF_8)
        if (target.exists() && !target.delete()) throw IOException("Could not replace Git baseline")
        if (!temp.renameTo(target)) throw IOException("Could not save Git baseline")
        return CaptureResult(output.length(), cachedFiles, skipped + (entries.count { !it.node.isDirectory && shouldTrack(it.relativePath) } - candidates.size))
    }

    fun readBaselineText(workspaceUri: Uri, entry: BaselineEntry): String? {
        val name = entry.textCache ?: return null
        val file = File(cacheDirectory(workspaceUri), name)
        return runCatching { file.takeIf { it.isFile }?.readText(Charsets.UTF_8) }.getOrNull()
    }

    fun clear(workspaceUri: Uri) {
        manifestFile(workspaceUri).delete()
        cacheDirectory(workspaceUri).deleteRecursively()
    }

    fun shouldTrack(path: String): Boolean {
        val parts = path.replace('\\', '/').split('/').map { it.lowercase() }
        if (parts.any { it in setOf(".git", ".gradle", ".idea", "build", "dist", "node_modules", "target", ".dart_tool") }) return false
        return !path.endsWith("-NovaIDE.zip", ignoreCase = true)
    }

    fun sha256(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        repository.openInput(uri)?.use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        } ?: throw IOException("Could not read file for Git status")
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun baseDirectory(workspaceUri: Uri): File = File(context.filesDir, "git_snapshots/${workspaceKey(workspaceUri)}")
    private fun manifestFile(workspaceUri: Uri): File = File(baseDirectory(workspaceUri), "manifest.json")
    private fun cacheDirectory(workspaceUri: Uri): File = File(baseDirectory(workspaceUri), "text")

    private fun workspaceKey(uri: Uri): String = pathHash(uri.toString()).take(32)
    private fun pathHash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
