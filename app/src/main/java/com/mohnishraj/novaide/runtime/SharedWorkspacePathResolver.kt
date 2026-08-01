package com.mohnishraj.novaide.runtime

object SharedWorkspacePathResolver {
    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

    fun resolve(authority: String?, treeDocumentId: String?): String? {
        if (authority != EXTERNAL_STORAGE_AUTHORITY || treeDocumentId.isNullOrBlank()) return null
        val separator = treeDocumentId.indexOf(':')
        if (separator <= 0) return null
        val volume = treeDocumentId.substring(0, separator)
        val relative = treeDocumentId.substring(separator + 1).replace('\\', '/').trim('/')
        if (relative.split('/').any { it == ".." || it.any { char -> char.code < 32 } }) return null
        val root = when {
            volume.equals("primary", ignoreCase = true) -> "/storage/emulated/0"
            volume.matches(Regex("[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}")) -> "/storage/$volume"
            else -> return null
        }
        return if (relative.isBlank()) root else "$root/$relative"
    }
}
