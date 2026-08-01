package com.mohnishraj.novaide.git

object GitUrlParser {
    private val ownerRepo = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")

    fun parse(value: String, branch: String = "main"): GitHubRepository? {
        val segments = normalizedSegments(value) ?: return null
        if (segments.size < 2) return null
        val owner = segments[0]
        val repository = segments[1].removeSuffix(".git")
        if (!ownerRepo.matches("$owner/$repository")) return null

        var selectedBranch = branch.trim().ifEmpty { "main" }
        if (segments.size > 2) {
            if (segments.getOrNull(2) != "tree" || segments.size < 4) return null
            if (branch.isBlank() || branch == "main") selectedBranch = segments.drop(3).joinToString("/")
        }
        if (!isValidRef(selectedBranch)) return null
        return GitHubRepository(owner, repository, selectedBranch)
    }

    /** Returns a branch only for a copied GitHub /tree/<branch> URL. */
    fun branchFromTreeUrl(value: String): String? {
        val segments = normalizedSegments(value) ?: return null
        if (segments.size < 4 || segments[2] != "tree") return null
        return segments.drop(3).joinToString("/").takeIf(::isValidRef)
    }

    fun parseRemoteConfig(config: String): GitHubRepository? {
        val url = Regex("(?m)^\\s*url\\s*=\\s*(.+?)\\s*$")
            .find(config)?.groupValues?.getOrNull(1) ?: return null
        return parse(url)
    }

    fun isValidRef(ref: String): Boolean {
        if (ref.isBlank() || ref == "@" || ref.length > 240) return false
        if (ref.startsWith('/') || ref.endsWith('/') || ref.endsWith('.') || ref.contains("..")) return false
        if (ref.contains("@{") || ref.any { it <= ' ' || it in "~^:?*[\\" }) return false
        return ref.split('/').none { it.isBlank() || it.startsWith('.') || it.endsWith(".lock") }
    }

    private fun normalizedSegments(value: String): List<String>? {
        var input = value.trim().trim('"', '\'')
        if (input.isBlank()) return null
        input = input.substringBefore('?').substringBefore('#').removeSuffix("/")
        val path = when {
            input.startsWith("git@github.com:", ignoreCase = true) -> input.substringAfter(':')
            input.startsWith("ssh://git@github.com/", ignoreCase = true) -> input.substringAfter("ssh://git@github.com/")
            input.startsWith("https://github.com/", ignoreCase = true) -> input.substringAfter("https://github.com/")
            input.startsWith("http://github.com/", ignoreCase = true) -> input.substringAfter("http://github.com/")
            input.startsWith("https://www.github.com/", ignoreCase = true) -> input.substringAfter("https://www.github.com/")
            input.startsWith("http://www.github.com/", ignoreCase = true) -> input.substringAfter("http://www.github.com/")
            input.startsWith("github.com/", ignoreCase = true) -> input.substringAfter("github.com/")
            input.startsWith("www.github.com/", ignoreCase = true) -> input.substringAfter("www.github.com/")
            else -> input
        }.trim('/')
        val segments = path.split('/').filter { it.isNotBlank() }
        // Unknown URL schemes/hosts are rejected instead of being interpreted as owner/repository.
        if (segments.firstOrNull()?.contains(':') == true) return null
        return segments
    }
}
