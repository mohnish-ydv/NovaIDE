package com.mohnishraj.novaide.git

import java.io.IOException

enum class GitCommitMode { INITIAL, UPDATE }

data class GitCommitPlan(val mode: GitCommitMode, val parent: String?)

/** Pure safety gate shared by the GitHub writer and JVM tests. */
object GitCommitPlanner {
    @Throws(IOException::class)
    fun plan(remoteHead: String?, expectedHead: String?, changes: List<GitChange>): GitCommitPlan {
        if (changes.isEmpty()) throw IOException("There are no local changes")
        if (expectedHead != null && remoteHead != expectedHead) {
            throw IOException(
                if (remoteHead == null) "Remote branch no longer exists. Reconnect or pull before committing."
                else "Remote branch changed since the last pull. Pull latest changes before committing."
            )
        }
        if (remoteHead == null && changes.any { it.kind == GitChangeKind.DELETED }) {
            throw IOException("Initial commit cannot contain deleted paths")
        }
        return if (remoteHead == null) GitCommitPlan(GitCommitMode.INITIAL, null)
        else GitCommitPlan(GitCommitMode.UPDATE, remoteHead)
    }
}
