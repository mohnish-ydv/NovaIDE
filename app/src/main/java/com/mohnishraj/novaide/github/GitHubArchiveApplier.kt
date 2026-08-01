package com.mohnishraj.novaide.github

import android.net.Uri
import com.mohnishraj.novaide.archive.ZipSafety
import com.mohnishraj.novaide.files.FileRepository
import com.mohnishraj.novaide.git.GitSnapshotStore
import com.mohnishraj.novaide.model.DocumentNode
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.ZipInputStream

class GitHubArchiveApplier(
    private val repository: FileRepository,
    private val snapshots: GitSnapshotStore
) {
    companion object {
        private const val MAX_ENTRIES = 12_000
        private const val MAX_FILE_BYTES = 40L * 1024L * 1024L
        private const val MAX_TOTAL_BYTES = 700L * 1024L * 1024L
    }

    data class ApplyResult(
        val filesWritten: Int,
        val foldersCreated: Int,
        val filesDeleted: Int,
        val bytesWritten: Long,
        val remotePaths: Set<String>
    )

    @Throws(IOException::class)
    fun apply(
        archive: File,
        workspaceRoot: DocumentNode,
        baseline: GitSnapshotStore.Baseline?
    ): ApplyResult {
        validateArchive(archive)
        val scan = repository.scan(workspaceRoot, maxEntries = MAX_ENTRIES, maxDepth = 60)
        val existingFiles = scan.entries.filter { !it.node.isDirectory }.associateBy { it.relativePath }.toMutableMap()
        val folderUris = mutableMapOf<String, Uri>("" to workspaceRoot.uri)
        scan.entries.filter { it.node.isDirectory }.forEach { folderUris[it.relativePath] = it.node.uri }
        val remotePaths = linkedSetOf<String>()
        var rootPrefix: String? = null
        var filesWritten = 0
        var foldersCreated = 0
        var totalBytes = 0L

        ZipInputStream(FileInputStream(archive).buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val safe = ZipSafety.safeSegments(entry.name) ?: throw IOException("Unsafe path in GitHub archive")
                if (rootPrefix == null) rootPrefix = safe.first()
                if (safe.first() != rootPrefix) throw IOException("GitHub archive contains multiple roots")
                val segments = safe.drop(1)
                if (segments.isEmpty()) {
                    zip.closeEntry()
                    continue
                }
                val relativePath = segments.joinToString("/")
                if (entry.isDirectory) {
                    if (existingFiles.containsKey(relativePath)) {
                        throw IOException("Path type conflict at $relativePath: local file, remote folder")
                    }
                    ensureFolder(segments, folderUris).also { created -> foldersCreated += created }
                } else {
                    val parentSegments = segments.dropLast(1)
                    foldersCreated += ensureFolder(parentSegments, folderUris)
                    val parentPath = parentSegments.joinToString("/")
                    val parent = folderUris[parentPath] ?: throw IOException("Could not resolve $parentPath")
                    if (folderUris.containsKey(relativePath)) {
                        throw IOException("Path type conflict at $relativePath: local folder, remote file")
                    }
                    val name = segments.last()
                    val target = existingFiles[relativePath]?.node?.uri
                        ?: repository.createFile(parent, name, mimeForName(name))
                        ?: throw IOException("Could not create $relativePath")
                    repository.openOutput(target, "w")?.use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var fileBytes = 0L
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            fileBytes += read
                            totalBytes += read
                            if (fileBytes > MAX_FILE_BYTES) throw IOException("$relativePath exceeds the 40 MB pull limit")
                            if (totalBytes > MAX_TOTAL_BYTES) throw IOException("Repository archive exceeds the 700 MB pull limit")
                            output.write(buffer, 0, read)
                        }
                    } ?: throw IOException("Could not write $relativePath")
                    remotePaths += relativePath
                    filesWritten++
                }
                zip.closeEntry()
            }
        }

        var deleted = 0
        baseline?.entries?.keys
            ?.filter { snapshots.shouldTrack(it) && it !in remotePaths }
            ?.sortedByDescending { it.count { character -> character == '/' } }
            ?.forEach { path ->
                val node = existingFiles[path]?.node ?: return@forEach
                if (repository.delete(node.uri)) deleted++
            }
        return ApplyResult(filesWritten, foldersCreated, deleted, totalBytes, remotePaths)
    }

    private fun validateArchive(archive: File) {
        var entries = 0
        var total = 0L
        var root: String? = null
        val seenPaths = mutableSetOf<String>()
        ZipInputStream(FileInputStream(archive).buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries++
                if (entries > MAX_ENTRIES) throw IOException("Repository has more than $MAX_ENTRIES archive entries")
                val safe = ZipSafety.safeSegments(entry.name) ?: throw IOException("Unsafe path in GitHub archive")
                if (root == null) root = safe.first()
                if (safe.first() != root) throw IOException("GitHub archive contains multiple roots")
                val relative = safe.drop(1).joinToString("/")
                if (relative.isNotEmpty() && !seenPaths.add(relative)) {
                    throw IOException("GitHub archive contains duplicate path: $relative")
                }
                if (!entry.isDirectory) {
                    val buffer = ByteArray(32 * 1024)
                    var fileBytes = 0L
                    while (true) {
                        val read = zip.read(buffer)
                        if (read < 0) break
                        fileBytes += read
                        total += read
                        if (fileBytes > MAX_FILE_BYTES) throw IOException("Archive file exceeds the 40 MB pull limit")
                        if (total > MAX_TOTAL_BYTES) throw IOException("Repository archive exceeds the 700 MB pull limit")
                    }
                }
                zip.closeEntry()
            }
        }
        if (entries == 0) throw IOException("GitHub archive is empty")
    }

    private fun ensureFolder(segments: List<String>, folders: MutableMap<String, Uri>): Int {
        var parent = folders[""] ?: throw IOException("Workspace root is unavailable")
        var current = ""
        var created = 0
        for (segment in segments) {
            current = if (current.isEmpty()) segment else "$current/$segment"
            val existing = folders[current]
            if (existing != null) {
                parent = existing
                continue
            }
            val folder = repository.createFolder(parent, segment)
                ?: throw IOException("Could not create folder $current")
            folders[current] = folder
            parent = folder
            created++
        }
        return created
    }

    private fun mimeForName(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "html", "htm" -> "text/html"
        "css" -> "text/css"
        "js", "mjs", "cjs" -> "application/javascript"
        "json" -> "application/json"
        "xml" -> "application/xml"
        "yaml", "yml", "md", "txt", "kt", "kts", "java", "py", "dart", "gradle", "properties" -> "text/plain"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "svg" -> "image/svg+xml"
        "zip" -> "application/zip"
        else -> "application/octet-stream"
    }
}
