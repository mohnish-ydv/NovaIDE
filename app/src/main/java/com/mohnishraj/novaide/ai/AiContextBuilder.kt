package com.mohnishraj.novaide.ai

import com.mohnishraj.novaide.core.TextFileClassifier
import com.mohnishraj.novaide.files.FileRepository

object AiContextBuilder {
    private const val MAX_CONTEXT_CHARS = 220_000
    private const val MAX_ACTIVE_CHARS = 90_000
    private const val MAX_RELEVANT_FILE_CHARS = 36_000
    private const val MAX_RELEVANT_FILES = 8
    private const val MAX_TREE_PATHS = 1_200

    fun build(
        query: String,
        activeFileName: String?,
        activeContent: String?,
        selection: String?,
        entries: List<FileRepository.WorkspaceEntry>,
        repository: FileRepository
    ): AiProjectContext {
        val builder = StringBuilder()
        val included = mutableListOf<String>()
        var redactions = 0
        val selectionText = selection?.takeIf { it.isNotBlank() }
        if (selectionText != null) {
            val redacted = SecretRedactor.redact(selectionText.take(MAX_ACTIVE_CHARS))
            redactions += redacted.redactions
            builder.append("## Selected code\n```\n").append(redacted.text).append("\n```\n\n")
        }
        if (!activeFileName.isNullOrBlank() && activeContent != null) {
            val redacted = SecretRedactor.redact(activeContent.take(MAX_ACTIVE_CHARS))
            redactions += redacted.redactions
            builder.append("## Active file: ").append(activeFileName).append("\n```\n")
                .append(redacted.text).append("\n```\n\n")
            included += activeFileName
        }

        val safePaths = entries.asSequence()
            .filter { !it.node.isDirectory }
            .map { it.relativePath }
            .filterNot(SecretRedactor::isSensitivePath)
            .take(MAX_TREE_PATHS)
            .toList()
        builder.append("## Project file map\n")
        safePaths.forEach { builder.append("- ").append(it).append('\n') }
        builder.append('\n')

        val terms = query.lowercase().split(Regex("[^a-z0-9_./-]+"))
            .filter { it.length >= 2 }.toSet()
        val priorityNames = setOf(
            "androidmanifest.xml", "build.gradle.kts", "build.gradle", "settings.gradle.kts",
            "package.json", "pubspec.yaml", "readme.md", "mainactivity.kt", "app.tsx", "index.html"
        )
        val candidates = entries.asSequence()
            .filter { !it.node.isDirectory }
            .filterNot { SecretRedactor.isSensitivePath(it.relativePath) }
            .filter { it.node.size in 1..(512L * 1024L) }
            .filter { TextFileClassifier.isProbablyText(it.node.name, it.node.mimeType) }
            .filterNot { activeFileName != null && (it.relativePath == activeFileName || it.node.name == activeFileName) }
            .map { entry ->
                val lower = entry.relativePath.lowercase()
                var score = terms.count { lower.contains(it) } * 10
                if (entry.node.name.lowercase() in priorityNames) score += 4
                if (lower.contains("src/main") || lower.contains("src/")) score += 2
                entry to score
            }
            .sortedWith(compareByDescending<Pair<FileRepository.WorkspaceEntry, Int>> { it.second }
                .thenBy { it.first.relativePath.length })
            .take(MAX_RELEVANT_FILES)
            .toList()

        for ((entry, _) in candidates) {
            if (builder.length >= MAX_CONTEXT_CHARS) break
            val content = runCatching { repository.readText(entry.node.uri, MAX_RELEVANT_FILE_CHARS.toLong()) }.getOrNull() ?: continue
            val redacted = SecretRedactor.redact(content.take(MAX_RELEVANT_FILE_CHARS))
            redactions += redacted.redactions
            val section = "## Relevant file: ${entry.relativePath}\n```\n${redacted.text}\n```\n\n"
            if (builder.length + section.length > MAX_CONTEXT_CHARS) break
            builder.append(section)
            included += entry.relativePath
        }

        return AiProjectContext(
            text = builder.toString().take(MAX_CONTEXT_CHARS),
            includedFiles = included.distinct(),
            omittedFiles = (entries.count { !it.node.isDirectory } - safePaths.size).coerceAtLeast(0),
            redactions = redactions
        )
    }
}
