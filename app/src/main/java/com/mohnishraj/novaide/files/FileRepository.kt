package com.mohnishraj.novaide.files

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.mohnishraj.novaide.model.DocumentNode
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.ArrayDeque

class FileRepository(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    companion object {
        const val DIRECTORY_MIME = DocumentsContract.Document.MIME_TYPE_DIR
        const val MAX_EDITABLE_BYTES = 2L * 1024L * 1024L
        const val MAX_PREVIEW_BYTES = 768L * 1024L
    }

    data class WorkspaceEntry(val node: DocumentNode, val relativePath: String)
    data class ScanResult(
        val entries: List<WorkspaceEntry>,
        val truncated: Boolean,
        val totalBytes: Long,
        val folderCount: Int
    )

    fun rootNode(treeUri: Uri): DocumentNode {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        val metadata = queryMetadata(documentUri)
        return DocumentNode(
            uri = documentUri,
            name = metadata.name.ifBlank { "Workspace" },
            mimeType = metadata.mimeType,
            isDirectory = true,
            depth = -1,
            size = metadata.size,
            lastModified = metadata.lastModified,
            isExpanded = true,
            childrenLoaded = true
        )
    }

    fun listChildren(parent: DocumentNode): List<DocumentNode> {
        if (!parent.isDirectory) return emptyList()
        val parentId = DocumentsContract.getDocumentId(parent.uri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parent.uri, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        val children = mutableListOf<DocumentNode>()
        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIndex)
                val name = cursor.getString(nameIndex) ?: "Untitled"
                val mime = cursor.getString(mimeIndex) ?: "application/octet-stream"
                val childUri = DocumentsContract.buildDocumentUriUsingTree(parent.uri, id)
                children += DocumentNode(
                    uri = childUri,
                    name = name,
                    mimeType = mime,
                    isDirectory = mime == DIRECTORY_MIME,
                    depth = parent.depth + 1,
                    size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L,
                    lastModified = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) else 0L,
                    parentUri = parent.uri
                )
            }
        }
        return children.sortedWith(compareBy<DocumentNode> { !it.isDirectory }.thenBy { it.name.lowercase() })
    }

    fun scan(root: DocumentNode, maxEntries: Int = 8_000, maxDepth: Int = 40): ScanResult {
        val entries = mutableListOf<WorkspaceEntry>()
        val sourceQueue = ArrayDeque<Pair<DocumentNode, String>>()
        val generatedQueue = ArrayDeque<Pair<DocumentNode, String>>()
        sourceQueue.add(root to "")
        val generatedNames = setOf(".git", ".gradle", "build", "dist", "node_modules", ".idea", "target")
        var totalBytes = 0L
        var folders = 0
        var truncated = false
        while (sourceQueue.isNotEmpty() || generatedQueue.isNotEmpty()) {
            if (Thread.currentThread().isInterrupted) {
                truncated = true
                break
            }
            val (parent, parentPath) = if (sourceQueue.isNotEmpty()) sourceQueue.removeFirst() else generatedQueue.removeFirst()
            val parentIsGenerated = parentPath.split('/').any { it.lowercase() in generatedNames }
            val children = runCatching { listChildren(parent) }.getOrElse { emptyList() }
            for (child in children) {
                if (entries.size >= maxEntries) {
                    truncated = true
                    sourceQueue.clear()
                    generatedQueue.clear()
                    break
                }
                val path = if (parentPath.isEmpty()) child.name else "$parentPath/${child.name}"
                entries += WorkspaceEntry(child, path)
                if (child.isDirectory) {
                    folders++
                    if (child.depth < maxDepth) {
                        val deferred = parentIsGenerated || child.name.lowercase() in generatedNames
                        if (deferred) generatedQueue.add(child to path) else sourceQueue.add(child to path)
                    }
                } else totalBytes += child.size.coerceAtLeast(0L)
            }
        }
        return ScanResult(entries, truncated, totalBytes, folders)
    }

    @Throws(IOException::class)
    fun readText(uri: Uri, maxBytes: Long = MAX_EDITABLE_BYTES): String {
        val metadata = queryMetadata(uri)
        if (metadata.size > maxBytes) throw IOException("File is larger than ${maxBytes / (1024 * 1024)} MB")
        resolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                val builder = StringBuilder()
                val buffer = CharArray(8192)
                var totalChars = 0L
                while (true) {
                    val read = reader.read(buffer)
                    if (read < 0) break
                    totalChars += read
                    if (totalChars > maxBytes) throw IOException("File is too large to edit safely")
                    builder.append(buffer, 0, read)
                }
                return builder.toString()
            }
        }
        throw IOException("Unable to open file")
    }

    fun openInput(uri: Uri): InputStream? = resolver.openInputStream(uri)
    fun openOutput(uri: Uri, mode: String = "w"): OutputStream? = resolver.openOutputStream(uri, mode)

    @Throws(IOException::class)
    fun writeText(uri: Uri, content: String) {
        resolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
            writer.write(content)
            writer.flush()
            return
        }
        throw IOException("Unable to write file")
    }

    fun createFile(parentUri: Uri, name: String, mimeType: String = "text/plain"): Uri? =
        runCatching { DocumentsContract.createDocument(resolver, parentUri, mimeType, name) }.getOrNull()

    fun createFolder(parentUri: Uri, name: String): Uri? =
        runCatching { DocumentsContract.createDocument(resolver, parentUri, DIRECTORY_MIME, name) }.getOrNull()

    fun rename(uri: Uri, newName: String): Uri? =
        runCatching { DocumentsContract.renameDocument(resolver, uri, newName) }.getOrNull()

    fun delete(uri: Uri): Boolean =
        runCatching { DocumentsContract.deleteDocument(resolver, uri) }.getOrDefault(false)

    fun metadata(uri: Uri): Metadata = queryMetadata(uri)

    private fun queryMetadata(uri: Uri): Metadata {
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)) ?: "Untitled"
                val mime = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)) ?: "application/octet-stream"
                val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                return Metadata(
                    name,
                    mime,
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L,
                    if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) else 0L
                )
            }
        }
        return Metadata("Workspace", DIRECTORY_MIME, 0L, 0L)
    }

    data class Metadata(val name: String, val mimeType: String, val size: Long, val lastModified: Long)
}
