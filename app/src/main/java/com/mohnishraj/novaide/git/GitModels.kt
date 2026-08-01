package com.mohnishraj.novaide.git

import android.net.Uri

data class GitHubRepository(
    val owner: String,
    val name: String,
    val branch: String = "main"
) {
    val slug: String get() = "$owner/$name"
    val webUrl: String get() = "https://github.com/$owner/$name"
}

data class GitChange(
    val path: String,
    val kind: GitChangeKind,
    val uri: Uri? = null,
    val size: Long = 0L,
    val baselineTextAvailable: Boolean = false
)

enum class GitChangeKind { ADDED, MODIFIED, DELETED }

data class GitStatus(
    val changes: List<GitChange>,
    val scannedFiles: Int,
    val skippedFiles: Int,
    val baselineExists: Boolean,
    val truncated: Boolean = false
) {
    val isClean: Boolean get() = baselineExists && changes.isEmpty()
    val added: Int get() = changes.count { it.kind == GitChangeKind.ADDED }
    val modified: Int get() = changes.count { it.kind == GitChangeKind.MODIFIED }
    val deleted: Int get() = changes.count { it.kind == GitChangeKind.DELETED }
}

data class GitHubRepoInfo(
    val fullName: String,
    val defaultBranch: String,
    val isPrivate: Boolean,
    val canPush: Boolean,
    val webUrl: String,
    val isEmpty: Boolean = false
)

data class GitBranch(val name: String, val sha: String, val protected: Boolean)

data class GitCommit(
    val sha: String,
    val message: String,
    val author: String,
    val date: String,
    val webUrl: String
)

data class WorkflowRun(
    val id: Long,
    val name: String,
    val status: String,
    val conclusion: String?,
    val branch: String,
    val event: String,
    val createdAt: String,
    val webUrl: String
)

data class GitCommitResult(val sha: String, val webUrl: String)

data class WorkflowArtifact(
    val id: Long,
    val name: String,
    val sizeBytes: Long,
    val expired: Boolean,
    val downloadUrl: String
)

data class ConflictBlock(
    val start: Int,
    val separator: Int,
    val endExclusive: Int,
    val ours: String,
    val theirs: String,
    val oursLabel: String,
    val theirsLabel: String
)

enum class ConflictResolution { OURS, THEIRS, BOTH }

data class UnifiedDiffResult(
    val text: String,
    val additions: Int,
    val deletions: Int,
    val truncated: Boolean
)
