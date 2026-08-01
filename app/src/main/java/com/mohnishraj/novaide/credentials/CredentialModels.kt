package com.mohnishraj.novaide.credentials

enum class CredentialId(val storageKey: String) {
    GITHUB("scm.github"),
    GITLAB("scm.gitlab"),
    OPENAI("ai.openai"),
    GEMINI("ai.gemini"),
    GROQ("ai.groq"),
    OPENROUTER("ai.openrouter"),
    CUSTOM_AI("ai.custom")
}

enum class AiProviderId(
    val label: String,
    val credentialId: CredentialId,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val keyRequired: Boolean = true
) {
    OPENAI("OpenAI", CredentialId.OPENAI, "https://api.openai.com/v1", "gpt-5-mini"),
    GEMINI("Google Gemini", CredentialId.GEMINI, "https://generativelanguage.googleapis.com/v1beta", "gemini-2.5-flash"),
    GROQ("Groq", CredentialId.GROQ, "https://api.groq.com/openai/v1", "openai/gpt-oss-20b"),
    OPENROUTER("OpenRouter", CredentialId.OPENROUTER, "https://openrouter.ai/api/v1", "openrouter/free"),
    CUSTOM("Custom OpenAI-compatible", CredentialId.CUSTOM_AI, "https://example.com/v1", "", keyRequired = false)
}

data class AiProviderConfig(
    val provider: AiProviderId,
    val baseUrl: String,
    val model: String,
    val hasKey: Boolean
)

data class CredentialDescriptor(
    val id: CredentialId,
    val title: String,
    val subtitle: String,
    val createUrl: String?,
    val permissionTip: String,
    val privacyNote: String = "Your secret is encrypted with Android Keystore and stored only on this device. NovaIDE sends it over HTTPS only to the provider you selected when you perform an authenticated action."
)

data class CredentialTestResult(
    val title: String,
    val details: String,
    val warnings: List<String> = emptyList()
)
