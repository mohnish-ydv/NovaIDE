package com.mohnishraj.novaide.archive

import android.net.Uri
import com.mohnishraj.novaide.files.FileRepository
import com.mohnishraj.novaide.model.DocumentNode
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ZipWorkspaceManager(private val repository: FileRepository) {
    data class ImportResult(val folderName: String, val files: Int, val folders: Int, val bytes: Long)
    data class ExportResult(val files: Int, val folders: Int, val bytes: Long)

    companion object {
        private const val MAX_ENTRIES = 10_000
        private const val MAX_TOTAL_BYTES = 512L * 1024L * 1024L
        private const val MAX_FILE_BYTES = 128L * 1024L * 1024L
    }

    fun importZip(zipUri: Uri, destinationParent: Uri, archiveName: String): ImportResult {
        val requestedRootName = ZipSafety.archiveFolderName(archiveName)
        val rootFolder = repository.createFolder(destinationParent, requestedRootName)
            ?: throw IOException("Could not create import folder")
        val folders = mutableMapOf<String, Uri>("" to rootFolder)
        var fileCount = 0
        var folderCount = 1
        var totalBytes = 0L
        var entries = 0

        try {
            repository.openInput(zipUri)?.use { raw ->
                ZipInputStream(raw.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        entries++
                        if (entries > MAX_ENTRIES) {
                            throw IOException("Archive has more than $MAX_ENTRIES entries")
                        }
                        val segments = ZipSafety.safeSegments(entry.name)
                            ?: throw IOException("Unsafe ZIP path: ${entry.name.take(80)}")
                        val isDirectory = entry.isDirectory || entry.name.endsWith('/')
                        var currentPath = ""
                        var parent = rootFolder
                        val folderSegments = if (isDirectory) segments else segments.dropLast(1)

                        for (segment in folderSegments) {
                            currentPath = if (currentPath.isEmpty()) segment else "$currentPath/$segment"
                            parent = folders[currentPath]
                                ?: repository.createFolder(parent, segment)?.also {
                                    folders[currentPath] = it
                                    folderCount++
                                }
                                ?: throw IOException("Could not create folder $segment")
                        }

                        if (!isDirectory) {
                            val name = segments.last()
                            val target = repository.createFile(parent, name, mimeForName(name))
                                ?: throw IOException("Could not create $name")
                            repository.openOutput(target, "w")?.use { output ->
                                val buffer = ByteArray(16 * 1024)
                                var fileBytes = 0L
                                while (true) {
                                    val read = zip.read(buffer)
                                    if (read < 0) break
                                    fileBytes += read
                                    totalBytes += read
                                    if (fileBytes > MAX_FILE_BYTES) {
                                        throw IOException("$name exceeds the per-file import limit")
                                    }
                                    if (totalBytes > MAX_TOTAL_BYTES) {
                                        throw IOException("Archive exceeds the 512 MB import limit")
                                    }
                                    output.write(buffer, 0, read)
                                }
                            } ?: throw IOException("Could not write $name")
                            fileCount++
                        }
                        zip.closeEntry()
                    }
                }
            } ?: throw IOException("Could not open ZIP")
        } catch (error: Exception) {
            repository.delete(rootFolder)
            throw error
        }

        val actualRootName = runCatching { repository.metadata(rootFolder).name }
            .getOrDefault(requestedRootName)
        return ImportResult(actualRootName, fileCount, folderCount, totalBytes)
    }

    fun exportWorkspace(root: DocumentNode, destinationZip: Uri): ExportResult {
        var files = 0
        var folders = 0
        var bytes = 0L
        repository.openOutput(destinationZip, "w")?.use { raw ->
            ZipOutputStream(raw.buffered()).use { zip ->
                fun writeFolder(folder: DocumentNode, path: String) {
                    for (child in repository.listChildren(folder)) {
                        if (child.uri == destinationZip) continue
                        if (files + folders >= MAX_ENTRIES) {
                            throw IOException("Workspace exceeds export entry limit")
                        }
                        val safeName = ZipSafety.exportSegment(child.name)
                        val childPath = if (path.isEmpty()) safeName else "$path/$safeName"
                        if (child.isDirectory) {
                            zip.putNextEntry(ZipEntry("$childPath/"))
                            zip.closeEntry()
                            folders++
                            writeFolder(child, childPath)
                        } else {
                            zip.putNextEntry(ZipEntry(childPath))
                            repository.openInput(child.uri)?.use { input ->
                                val buffer = ByteArray(16 * 1024)
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    bytes += read
                                    if (bytes > MAX_TOTAL_BYTES) {
                                        throw IOException("Workspace exceeds 512 MB export limit")
                                    }
                                    zip.write(buffer, 0, read)
                                }
                            } ?: throw IOException("Could not read ${child.name}")
                            zip.closeEntry()
                            files++
                        }
                    }
                }
                writeFolder(root, "")
            }
        } ?: throw IOException("Could not create ZIP")
        return ExportResult(files, folders, bytes)
    }

    private fun mimeForName(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "html", "htm" -> "text/html"
        "css" -> "text/css"
        "js", "mjs", "cjs" -> "application/javascript"
        "json" -> "application/json"
        "xml" -> "application/xml"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "svg" -> "image/svg+xml"
        else -> "application/octet-stream"
    }
}
