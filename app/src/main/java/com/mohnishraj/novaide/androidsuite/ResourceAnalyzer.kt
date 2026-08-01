package com.mohnishraj.novaide.androidsuite

object ResourceAnalyzer {
    private val validName = Regex("^[a-z][a-z0-9_]*$")
    private val fileTypes = setOf("anim", "animator", "color", "drawable", "font", "interpolator", "layout", "menu", "mipmap", "navigation", "raw", "transition", "values", "xml")

    fun analyze(files: List<AndroidSourceFile>): ResourceReport {
        val items = files.mapNotNull { file ->
            val path = file.path.replace('\\', '/')
            val marker = "/src/main/res/"
            val index = path.indexOf(marker)
            if (index < 0) return@mapNotNull null
            val relative = path.substring(index + marker.length)
            val folder = relative.substringBefore('/', "")
            val fileName = relative.substringAfterLast('/')
            if (folder.isBlank() || fileName.isBlank()) return@mapNotNull null
            val baseType = folder.substringBefore('-')
            if (baseType !in fileTypes) return@mapNotNull null
            val qualifier = folder.substringAfter('-', "")
            val resourceName = if (baseType == "values") fileName.substringBeforeLast('.') else fileName.substringBeforeLast('.')
            ResourceItem(baseType, resourceName, path, file.sizeBytes.coerceAtLeast(0L), qualifier)
        }
        val issues = mutableListOf<AndroidProjectIssue>()
        val invalid = items.filter { !validName.matches(it.name) }
        invalid.forEach { issues += AndroidProjectIssue(AndroidIssueSeverity.ERROR, "Invalid resource filename", "${it.name} must use lowercase letters, digits, and underscores and start with a letter.", it.path) }
        val huge = items.filter { it.type in setOf("drawable", "mipmap", "raw") && it.sizeBytes > 2L * 1024L * 1024L }
        huge.forEach { issues += AndroidProjectIssue(AndroidIssueSeverity.WARNING, "Large packaged resource", "${it.path} is ${formatBytes(it.sizeBytes)}. Consider compression, WebP, vector assets, or downloading large media.", it.path) }
        val duplicates = items.groupBy { "${it.type}/${it.name}" }.filterValues { group -> group.map { it.qualifier }.distinct().size != group.size }
            .mapValues { (_, group) -> group.map { it.path } }
        duplicates.forEach { (key, paths) -> issues += AndroidProjectIssue(AndroidIssueSeverity.ERROR, "Duplicate resource in same qualifier", "$key is defined more than once with an overlapping qualifier: ${paths.joinToString()}") }
        val densityBitmaps = items.filter { it.type in setOf("drawable", "mipmap") && it.path.substringAfterLast('.').lowercase() in setOf("png", "jpg", "jpeg", "webp") }
        val densityGroups = densityBitmaps.groupBy { "${it.type}/${it.name}" }
        densityGroups.filterValues { group -> group.size == 1 && group.first().qualifier.isBlank() }.forEach { (key, _) ->
            issues += AndroidProjectIssue(AndroidIssueSeverity.INFO, "Unqualified bitmap", "$key has no density qualifier. Confirm that Android should scale this bitmap automatically.")
        }
        return ResourceReport(
            totalFiles = items.size,
            totalBytes = items.sumOf { it.sizeBytes },
            byType = items.groupingBy { it.type }.eachCount().toSortedMap(),
            byQualifier = items.groupingBy { it.qualifier.ifBlank { "default" } }.eachCount().toSortedMap(),
            largest = items.sortedByDescending { it.sizeBytes }.take(20),
            duplicateNames = duplicates,
            invalidNames = invalid,
            issues = issues
        )
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
