package com.mohnishraj.novaide.gitlab

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

data class GitLabIdentity(
    val username: String,
    val name: String,
    val scopes: Set<String>,
    val expiresAt: String?,
    val active: Boolean
) {
    val canReadApi: Boolean get() = "api" in scopes || "read_api" in scopes
    val canWriteApi: Boolean get() = "api" in scopes
}

class GitLabApiClient(baseUrl: String, token: String) {
    private val origin: URI
    private val token: String = GitLabTokenNormalizer.normalize(token)

    init {
        val parsed = runCatching { URI(baseUrl.trim().trimEnd('/')) }
            .getOrElse { throw IllegalArgumentException("GitLab server URL is invalid") }
        require(parsed.scheme.equals("https", ignoreCase = true) && parsed.host != null) {
            "GitLab server must use HTTPS"
        }
        require(parsed.userInfo == null && parsed.query == null && parsed.fragment == null) {
            "GitLab server URL must not contain credentials, query parameters or fragments"
        }
        origin = parsed
    }

    fun verify(): GitLabIdentity {
        val user = requestJson("/api/v4/user")
        val self = runCatching { requestJson("/api/v4/personal_access_tokens/self") }.getOrNull()
        val scopes = parseScopes(self)
        return GitLabIdentity(
            username = user.optString("username", "unknown"),
            name = user.optString("name", user.optString("username", "Unknown")),
            scopes = scopes,
            expiresAt = self?.optString("expires_at", "")?.ifBlank { null },
            active = self?.optBoolean("active", true) ?: true
        )
    }

    fun requireRead(identity: GitLabIdentity) {
        if (!identity.canReadApi) {
            throw IOException("GitLab token is connected but lacks API read access. Recreate it with read_api or api scope.")
        }
    }

    fun requireWrite(identity: GitLabIdentity) {
        if (!identity.canWriteApi) {
            throw IOException("GitLab denied this write operation because the token lacks the api scope. Recreate it with api scope and ensure your project role allows writing.")
        }
    }

    private fun requestJson(path: String): JSONObject {
        val url = origin.resolve(path).toURL()
        val connection = open(url)
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText().take(2_000_000) }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(text).optString("message") }.getOrNull()
                throw IOException(when (status) {
                    401 -> "GitLab token is invalid, expired or revoked."
                    403 -> "GitLab denied this request. Check token scopes and your project role."
                    404 -> "GitLab API endpoint was not found. Confirm the server URL and GitLab version."
                    else -> message?.takeIf { it.isNotBlank() } ?: "GitLab request failed with HTTP $status"
                })
            }
            return runCatching { JSONObject(text) }.getOrElse { throw IOException("GitLab returned invalid JSON") }
        } catch (error: Exception) {
            throw friendly(error)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseScopes(json: JSONObject?): Set<String> {
        if (json == null) return emptySet()
        val output = linkedSetOf<String>()
        val scopes = json.optJSONArray("scopes") ?: JSONArray()
        for (index in 0 until scopes.length()) scopes.optString(index).takeIf { it.isNotBlank() }?.let(output::add)
        val granular = json.optJSONArray("granular_scopes") ?: JSONArray()
        for (index in 0 until granular.length()) {
            val item = granular.optJSONObject(index) ?: continue
            val name = item.optString("name", item.optString("scope", ""))
            if (name.isNotBlank()) output += name
        }
        return output
    }

    private fun open(url: URL): HttpURLConnection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 20_000
        readTimeout = 40_000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("User-Agent", "NovaIDE-Android-M6")
        if (url.host.equals(origin.host, ignoreCase = true)) {
            setRequestProperty("PRIVATE-TOKEN", token)
        }
    }

    private fun friendly(error: Exception): IOException = when (error) {
        is UnknownHostException -> IOException("Could not resolve ${origin.host}. Check internet, Private DNS, VPN or the GitLab server address.", error)
        is SocketTimeoutException -> IOException("GitLab connection timed out.", error)
        is SSLException -> IOException("Secure TLS connection to GitLab failed. Check device date/time, VPN or server certificate.", error)
        is IOException -> error
        else -> IOException(error.message ?: "GitLab request failed", error)
    }
}
