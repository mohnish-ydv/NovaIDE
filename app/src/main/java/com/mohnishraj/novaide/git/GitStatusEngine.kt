package com.mohnishraj.novaide.git

import android.net.Uri
import com.mohnishraj.novaide.files.FileRepository

class GitStatusEngine(
    private val repository: FileRepository,
    private val snapshots: GitSnapshotStore
) {
    fun status(workspaceUri: Uri, entries: List<FileRepository.WorkspaceEntry>): GitStatus {
        val current = entries.asSequence()
            .filter { !it.node.isDirectory && snapshots.shouldTrack(it.relativePath) }
            .associateBy { it.relativePath }
        val baseline = snapshots.load(workspaceUri)
        if (baseline == null) {
            var skipped = 0
            val initial = current.values.mapNotNull { entry ->
                if (entry.node.size > 25L * 1024L * 1024L) {
                    skipped++
                    null
                } else GitChange(entry.relativePath, GitChangeKind.ADDED, entry.node.uri, entry.node.size)
            }
            return GitStatus(
                changes = initial.sortedBy { it.path.lowercase() },
                scannedFiles = initial.size,
                skippedFiles = skipped,
                baselineExists = false,
                truncated = current.size > 6_000
            )
        }
        val changes = mutableListOf<GitChange>()
        var scanned = 0
        var skipped = 0
        for ((path, entry) in current) {
            if (Thread.currentThread().isInterrupted) break
            if (entry.node.size > 25L * 1024L * 1024L) {
                skipped++
                continue
            }
            scanned++
            val old = baseline.entries[path]
            if (old == null) {
                changes += GitChange(path, GitChangeKind.ADDED, entry.node.uri, entry.node.size)
            } else {
                val hash = runCatching { snapshots.sha256(entry.node.uri) }.getOrNull()
                if (hash == null) skipped++
                else if (hash != old.sha256) changes += GitChange(
                    path, GitChangeKind.MODIFIED, entry.node.uri, entry.node.size, old.textCache != null
                )
            }
        }
        baseline.entries.keys.filterNot(current::containsKey).forEach { path ->
            changes += GitChange(path, GitChangeKind.DELETED, baselineTextAvailable = baseline.entries[path]?.textCache != null)
        }
        return GitStatus(
            changes = changes.sortedWith(compareBy<GitChange> { it.kind.ordinal }.thenBy { it.path.lowercase() }),
            scannedFiles = scanned,
            skippedFiles = skipped,
            baselineExists = true,
            truncated = entries.count { !it.node.isDirectory && snapshots.shouldTrack(it.relativePath) } > 6_000
        )
    }
}
