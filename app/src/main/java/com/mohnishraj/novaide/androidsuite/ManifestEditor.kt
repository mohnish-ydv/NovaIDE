package com.mohnishraj.novaide.androidsuite

object ManifestEditor {
    private val permissionName = Regex("^android\\.permission\\.[A-Z0-9_]+$")

    fun listPermissions(source: String): List<String> =
        Regex("""<uses-permission(?:-sdk-23|-sdk-m)?\b[^>]*\bandroid:name\s*=\s*[\"']([^\"']+)[\"'][^>]*/?>""")
            .findAll(source).map { it.groupValues[1] }.distinct().sorted().toList()

    fun addPermission(source: String, permission: String): String {
        require(permissionName.matches(permission)) { "Invalid Android permission name" }
        if (listPermissions(source).contains(permission)) return source
        val manifestMatch = Regex("""<manifest\b[^>]*>""").find(source)
            ?: throw IllegalArgumentException("Manifest opening tag was not found")
        val newline = if (source.contains("\r\n")) "\r\n" else "\n"
        val indent = detectChildIndent(source, manifestMatch.range.last + 1)
        val insertion = "$newline$indent<uses-permission android:name=\"$permission\" />"
        return source.substring(0, manifestMatch.range.last + 1) + insertion + source.substring(manifestMatch.range.last + 1)
    }

    fun removePermission(source: String, permission: String): String {
        val escaped = Regex.escape(permission)
        val pattern = Regex("""(?m)^[\t ]*<uses-permission(?:-sdk-23|-sdk-m)?\b(?=[^>]*\bandroid:name\s*=\s*[\"']$escaped[\"'])[^>]*/?>[\t ]*(?:\r?\n)?""")
        return source.replace(pattern, "")
    }

    private fun detectChildIndent(source: String, start: Int): String {
        val after = source.substring(start.coerceIn(0, source.length))
        return Regex("""\r?\n([\t ]+)<""").find(after)?.groupValues?.getOrNull(1) ?: "    "
    }
}
