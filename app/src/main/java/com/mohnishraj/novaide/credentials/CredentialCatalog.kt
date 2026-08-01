package com.mohnishraj.novaide.credentials

object CredentialCatalog {
    val github = CredentialDescriptor(
        CredentialId.GITHUB,
        "GitHub",
        "Repositories, commits, branches, Actions and artifacts",
        "https://github.com/settings/personal-access-tokens/new",
        "Tip: select only the repositories NovaIDE should manage. Enable Metadata read, Contents read/write and Actions read. Add Workflows write only when you intend to edit workflow files; avoid unrelated account or admin permissions."
    )

    val gitlab = CredentialDescriptor(
        CredentialId.GITLAB,
        "GitLab",
        "GitLab.com or a self-managed GitLab instance",
        "https://gitlab.com/-/user_settings/personal_access_tokens",
        "Tip: use the api scope for complete repository read/write API access, or read_api when you only need inspection. The token can never exceed your own project role."
    )

    fun ai(provider: AiProviderId): CredentialDescriptor = when (provider) {
        AiProviderId.OPENAI -> CredentialDescriptor(
            CredentialId.OPENAI, "OpenAI API", "AI chat, code generation and project reasoning",
            "https://platform.openai.com/api-keys",
            "Create a project API key with access to the models you plan to use. API usage and billing are controlled by your OpenAI account."
        )
        AiProviderId.GEMINI -> CredentialDescriptor(
            CredentialId.GEMINI, "Google Gemini API", "Gemini models through Google AI Studio",
            "https://aistudio.google.com/app/apikey",
            "Create an API key in Google AI Studio. Free-tier quotas and model availability are controlled by Google."
        )
        AiProviderId.GROQ -> CredentialDescriptor(
            CredentialId.GROQ, "Groq API", "Fast OpenAI-compatible inference",
            "https://console.groq.com/keys",
            "Create a Groq API key and choose a model available to your Groq project."
        )
        AiProviderId.OPENROUTER -> CredentialDescriptor(
            CredentialId.OPENROUTER, "OpenRouter API", "One key for many hosted model providers",
            "https://openrouter.ai/settings/keys",
            "Create an OpenRouter key. You can choose free models or models funded by your OpenRouter balance."
        )
        AiProviderId.CUSTOM -> CredentialDescriptor(
            CredentialId.CUSTOM_AI, "Custom AI endpoint", "Any HTTPS OpenAI-compatible /v1 endpoint",
            null,
            "Enter an HTTPS base URL such as https://host.example/v1. Keys are optional for endpoints that do not require authentication."
        )
    }

    val aiProviders: List<AiProviderId> = AiProviderId.entries
}
