package com.mohnishraj.novaide.gitlab

object GitLabTokenNormalizer {
    fun normalize(value: String): String {
        var token = value.trim().trim('"', '\'')
        token = token.replace(Regex("^(?i:private-token|authorization)\\s*:\\s*"), "").trim()
        token = token.replace(Regex("^(?i:bearer)\\s+"), "").trim()
        require(token.none { it.isWhitespace() }) { "GitLab token contains whitespace" }
        require(token.length in 8..2048) { "GitLab token length is invalid" }
        return token
    }
}
