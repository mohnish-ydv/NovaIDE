package com.mohnishraj.novaide.workspace.search

import com.mohnishraj.novaide.core.TextFileClassifier
import com.mohnishraj.novaide.files.FileRepository

data class WorkspaceSearchHit(
    val entry: FileRepository.WorkspaceEntry,
    val line: Int,
    val column: Int,
    val preview: String,
    val fileNameMatch: Boolean
)

data class WorkspaceSearchResult(val hits: List<WorkspaceSearchHit>, val truncated: Boolean, val error: String? = null)

class WorkspaceSearchEngine(private val repository: FileRepository) {
    fun search(
        entries: List<FileRepository.WorkspaceEntry>,
        query: String,
        options: WorkspaceSearchOptions,
        maxResults: Int = 500
    ): WorkspaceSearchResult {
        if (query.isBlank()) return WorkspaceSearchResult(emptyList(), false)
        val regex = WorkspaceSearchMatcher.compile(query, options).getOrElse {
            return WorkspaceSearchResult(emptyList(), false, it.message ?: "Invalid regular expression")
        }
        val hits = mutableListOf<WorkspaceSearchHit>()
        var truncated = false
        for (entry in entries) {
            if (Thread.currentThread().isInterrupted) break
            if (entry.node.isDirectory || WorkspaceSearchMatcher.shouldSkip(entry.relativePath, options.includeGenerated)) continue
            if (regex.containsMatchIn(entry.node.name)) {
                hits += WorkspaceSearchHit(entry, 0, 0, "File name match", true)
            }
            if (hits.size >= maxResults) {
                truncated = true
                break
            }
            if (!TextFileClassifier.isProbablyText(entry.node.name, entry.node.mimeType) ||
                entry.node.size > FileRepository.MAX_PREVIEW_BYTES
            ) continue
            val text = runCatching {
                repository.readText(entry.node.uri, FileRepository.MAX_PREVIEW_BYTES)
            }.getOrNull() ?: continue
            for (match in WorkspaceSearchMatcher.findLines(text, regex).matches) {
                hits += WorkspaceSearchHit(entry, match.line, match.column, match.preview, false)
                if (hits.size >= maxResults) {
                    truncated = true
                    break
                }
            }
            if (truncated) break
        }
        return WorkspaceSearchResult(hits, truncated)
    }
}
