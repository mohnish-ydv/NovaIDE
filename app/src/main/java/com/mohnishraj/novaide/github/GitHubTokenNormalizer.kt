package com.mohnishraj.novaide.github

/** Normalizes tokens copied from GitHub, terminals, or HTTP examples without weakening validation. */
object GitHubTokenNormalizer {
    fun normalize(value: String): String {
        var token = value.trim().trim('"', '\'')
        token = token.replace(Regex("^(?i:authorization)\\s*:\\s*"), "").trim()
        token = token.replace(Regex("^(?i:bearer|token)\\s+"), "").trim()
        require(token.none { it.isWhitespace() }) { "GitHub token contains whitespace" }
        require(token.length in 20..512) { "GitHub token length is invalid" }
        return token
    }
}
