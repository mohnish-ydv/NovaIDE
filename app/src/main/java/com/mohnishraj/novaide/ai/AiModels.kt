package com.mohnishraj.novaide.ai

import com.mohnishraj.novaide.credentials.AiProviderConfig

enum class AiTask(val label: String) {
    CHAT("AI Chat"),
    EXPLAIN("Explain code"),
    FIX("Fix code"),
    REFACTOR("Refactor code"),
    GENERATE("Generate code"),
    PROJECT_QA("Ask project"),
    ERROR_TRACE("Trace error"),
    SECURITY("Security review"),
    PERFORMANCE("Performance review")
}

data class AiModelInfo(val id: String, val label: String = id)

data class AiProjectContext(
    val text: String,
    val includedFiles: List<String>,
    val omittedFiles: Int,
    val redactions: Int
)

data class AiRequest(
    val task: AiTask,
    val userPrompt: String,
    val context: AiProjectContext,
    val activeFile: String?,
    val selectionPresent: Boolean
)

data class AiResponse(
    val text: String,
    val provider: String,
    val model: String,
    val requestId: String? = null
)

data class AiRuntime(
    val config: AiProviderConfig,
    val apiKey: String?
)

data class NovaFilePatch(val path: String, val content: String)
