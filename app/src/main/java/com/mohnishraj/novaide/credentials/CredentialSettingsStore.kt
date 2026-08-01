package com.mohnishraj.novaide.credentials

import android.content.Context
import java.net.URI

class CredentialSettingsStore(context: Context, private val secrets: SecureCredentialStore) {
    private val prefs = context.getSharedPreferences("nova_credentials_settings", Context.MODE_PRIVATE)

    var activeAiProvider: AiProviderId
        get() = runCatching { AiProviderId.valueOf(prefs.getString("active_ai_provider", null).orEmpty()) }
            .getOrDefault(AiProviderId.GEMINI)
        set(value) { prefs.edit().putString("active_ai_provider", value.name).apply() }

    fun aiConfig(provider: AiProviderId): AiProviderConfig {
        val base = prefs.getString("ai.${provider.name}.base", null)?.trim().orEmpty()
            .ifBlank { provider.defaultBaseUrl }
        val model = prefs.getString("ai.${provider.name}.model", null)?.trim().orEmpty()
            .ifBlank { provider.defaultModel }
        return AiProviderConfig(provider, base, model, secrets.has(provider.credentialId))
    }

    fun validatedAiConfig(provider: AiProviderId, baseUrl: String, model: String): AiProviderConfig {
        val normalized = validateBaseUrl(baseUrl.ifBlank { provider.defaultBaseUrl }, provider)
        return AiProviderConfig(provider, normalized, model.trim(), secrets.has(provider.credentialId))
    }

    fun saveAiConfig(provider: AiProviderId, baseUrl: String, model: String) {
        val validated = validatedAiConfig(provider, baseUrl, model)
        prefs.edit()
            .putString("ai.${provider.name}.base", validated.baseUrl)
            .putString("ai.${provider.name}.model", validated.model)
            .apply()
    }

    fun normalizeGitLabBaseUrl(value: String): String = validateHttpsOrigin(value)

    var gitLabBaseUrl: String
        get() = prefs.getString("gitlab.base", "https://gitlab.com").orEmpty().ifBlank { "https://gitlab.com" }
        set(value) { prefs.edit().putString("gitlab.base", validateHttpsOrigin(value)).apply() }

    private fun validateBaseUrl(value: String, provider: AiProviderId): String {
        val uri = runCatching { URI(value.trim().trimEnd('/')) }.getOrElse { throw IllegalArgumentException("Base URL is invalid") }
        require(uri.host != null) { "Base URL must include a host" }
        require(uri.scheme.equals("https", ignoreCase = true)) {
            if (provider == AiProviderId.CUSTOM) "Custom AI endpoints must use HTTPS" else "Provider base URL must use HTTPS"
        }
        require(uri.userInfo == null && uri.fragment == null) { "Base URL must not contain credentials or a fragment" }
        return uri.toString().trimEnd('/')
    }

    private fun validateHttpsOrigin(value: String): String {
        val normalized = value.trim().trimEnd('/')
        val uri = runCatching { URI(normalized) }.getOrElse { throw IllegalArgumentException("GitLab URL is invalid") }
        require(uri.scheme.equals("https", ignoreCase = true) && uri.host != null) { "GitLab URL must be an HTTPS address" }
        require(uri.userInfo == null && uri.query == null && uri.fragment == null) { "GitLab URL must be a clean server address" }
        return uri.toString().trimEnd('/')
    }
}
