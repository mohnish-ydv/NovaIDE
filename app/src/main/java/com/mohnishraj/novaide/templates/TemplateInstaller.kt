package com.mohnishraj.novaide.templates

import android.net.Uri
import com.mohnishraj.novaide.files.FileRepository
import java.io.IOException

class TemplateInstaller(private val repository: FileRepository) {
    data class Result(val projectFolder: String, val filesCreated: Int)

    fun install(parent: Uri, folderName: String, template: ProjectTemplate): Result {
        val clean = folderName.trim().replace(Regex("[\\/:*?\"<>|]"), "_").trim('.', ' ').take(80).ifBlank { template.name }
        val root = repository.createFolder(parent, clean) ?: throw IOException("Could not create project folder")
        val folders = mutableMapOf("" to root)
        var files = 0
        for (file in template.files) {
            val parts = file.path.split('/').filter { it.isNotBlank() }
            var path = ""
            var current = root
            for (segment in parts.dropLast(1)) {
                path = if (path.isEmpty()) segment else "$path/$segment"
                current = folders[path] ?: repository.createFolder(current, segment)?.also { folders[path] = it }
                    ?: throw IOException("Could not create $path")
            }
            val name = parts.lastOrNull() ?: continue
            val uri = repository.createFile(current, name, mime(name)) ?: throw IOException("Could not create $name")
            repository.writeText(uri, file.content)
            files++
        }
        return Result(runCatching { repository.metadata(root).name }.getOrDefault(clean), files)
    }

    private fun mime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "html" -> "text/html"; "css" -> "text/css"; "js" -> "application/javascript"; "json" -> "application/json"; "md" -> "text/markdown"; else -> "text/plain"
    }
}
