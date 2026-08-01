package com.mohnishraj.novaide.ai

import android.net.Uri
import com.mohnishraj.novaide.files.FileRepository
import com.mohnishraj.novaide.model.DocumentNode
import java.io.IOException

class AiWorkspacePatcher(private val repository: FileRepository) {
    data class Result(val created: Int, val updated: Int, val paths: List<String>)

    @Throws(IOException::class)
    fun apply(root: DocumentNode, patches: List<NovaFilePatch>, dirtyPaths: Set<String>): Result {
        if (patches.isEmpty()) throw IOException("AI response contains no NovaIDE file patches")
        val blocked = patches.map { it.path }.filter { path -> dirtyPaths.any { it.equals(path, ignoreCase = true) } }
        if (blocked.isNotEmpty()) throw IOException("Save or close unsaved files before applying: ${blocked.joinToString()}")
        var created = 0
        var updated = 0
        val changed = mutableListOf<String>()
        for (patch in patches) {
            if (Thread.currentThread().isInterrupted) throw IOException("AI patch cancelled")
            val parts = patch.path.split('/')
            var parent = root
            for (folderName in parts.dropLast(1)) {
                parent = findChild(parent, folderName, directory = true)
                    ?: repository.createFolder(parent.uri, folderName)?.let { uri ->
                        DocumentNode(uri, folderName, "vnd.android.document/directory", true, parent.depth + 1, parentUri = parent.uri)
                    }
                    ?: throw IOException("Could not create folder $folderName")
            }
            val fileName = parts.last()
            val existing = findChild(parent, fileName, directory = false)
            val uri: Uri = existing?.uri ?: repository.createFile(parent.uri, fileName, mimeFor(fileName))
                ?: throw IOException("Could not create ${patch.path}")
            repository.writeText(uri, patch.content)
            if (existing == null) created++ else updated++
            changed += patch.path
        }
        return Result(created, updated, changed)
    }

    private fun findChild(parent: DocumentNode, name: String, directory: Boolean): DocumentNode? =
        repository.listChildren(parent).firstOrNull { it.isDirectory == directory && it.name.equals(name, ignoreCase = true) }

    private fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "json" -> "application/json"
        "xml" -> "application/xml"
        "html", "htm" -> "text/html"
        "css" -> "text/css"
        "js", "mjs", "cjs" -> "text/javascript"
        "svg" -> "image/svg+xml"
        else -> "text/plain"
    }
}
