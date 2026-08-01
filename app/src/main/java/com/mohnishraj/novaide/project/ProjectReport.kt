package com.mohnishraj.novaide.project

data class ProjectReport(
    val detection: ProjectDetection,
    val fileCount: Int,
    val folderCount: Int,
    val totalBytes: Long,
    val truncated: Boolean,
    val indexedAt: Long = System.currentTimeMillis()
) {
    val formattedSize: String
        get() {
            val kb = totalBytes / 1024.0
            return when {
                kb < 1 -> "$totalBytes B"
                kb < 1024 -> "%.1f KB".format(kb)
                kb < 1024 * 1024 -> "%.1f MB".format(kb / 1024.0)
                else -> "%.2f GB".format(kb / (1024.0 * 1024.0))
            }
        }
}
