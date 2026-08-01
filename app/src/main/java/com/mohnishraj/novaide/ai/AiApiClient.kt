package com.mohnishraj.novaide.ai

import com.mohnishraj.novaide.credentials.AiProviderId
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class AiApiClient(private val runtime: AiRuntime) {
    companion object {
        private const val MAX_RESPONSE_BYTES = 8L * 1024L * 1024L
        private const val MAX_PROMPT_CHARS = 420_000
        private const val MAX_MODELS = 500
    }

    private data class HttpResult(val body: String, val requestId: String?)

    fun testConnection(): List<AiModelInfo> {
        val models = models()
        if (models.isEmpty() && runtime.config.model.isBlank()) {
            throw IOException("Provider connected but returned no usable models. Enter a model ID manually.")
        }
        return models
    }

    fun models(): List<AiModelInfo> = when (runtime.config.provider) {
        AiProviderId.GEMINI -> geminiModels()
        else -> openAiCompatibleModels()
    }

    fun complete(request: AiRequest): AiResponse {
        require(request.userPrompt.isNotBlank()) { "AI request cannot be blank" }
        val input = AiPromptBuilder.user(request)
        if (input.length > MAX_PROMPT_CHARS) throw IOException("AI context exceeds the mobile request limit")
        val model = runtime.config.model.trim().ifBlank { throw IOException("Choose an AI model first") }
        return when (runtime.config.provider) {
            AiProviderId.OPENAI -> openAiResponse(model, request.task, input)
            AiProviderId.GEMINI -> geminiGenerate(model, request.task, input)
            AiProviderId.GROQ, AiProviderId.OPENROUTER, AiProviderId.CUSTOM -> compatibleChat(model, request.task, input)
        }
    }

    private fun openAiCompatibleModels(): List<AiModelInfo> {
        val result = request("GET", endpoint("/models"), null)
        val root = parseObject(result.body)
        val data = root.optJSONArray("data") ?: JSONArray()
        return buildList {
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: continue
                val id = item.optString("id", "").trim()
                if (id.isNotBlank()) add(AiModelInfo(id))
                if (size >= MAX_MODELS) break
            }
        }.distinctBy { it.id }.sortedBy { it.id.lowercase() }
    }

    private fun geminiModels(): List<AiModelInfo> {
        val result = request("GET", endpoint("/models?pageSize=1000"), null)
        val root = parseObject(result.body)
        val data = root.optJSONArray("models") ?: JSONArray()
        return buildList {
            for (index in 0 until data.length()) {
                val item = data.optJSONObject(index) ?: continue
                val methods = item.optJSONArray("supportedGenerationMethods") ?: JSONArray()
                var supportsText = false
                for (m in 0 until methods.length()) if (methods.optString(m) == "generateContent") supportsText = true
                if (!supportsText) continue
                val id = item.optString("name", "").removePrefix("models/").trim()
                if (id.isNotBlank()) add(AiModelInfo(id, item.optString("displayName", id)))
                if (size >= MAX_MODELS) break
            }
        }.distinctBy { it.id }.sortedBy { it.label.lowercase() }
    }

    private fun openAiResponse(model: String, task: AiTask, input: String): AiResponse {
        val body = JSONObject().apply {
            put("model", model)
            put("instructions", AiPromptBuilder.system(task))
            put("input", input)
            put("max_output_tokens", 12_000)
        }
        val result = request("POST", endpoint("/responses"), body)
        val root = parseObject(result.body)
        val text = root.optString("output_text", "").ifBlank { parseOpenAiOutput(root) }
        if (text.isBlank()) throw IOException("OpenAI returned no text output")
        return AiResponse(text, runtime.config.provider.label, root.optString("model", model), result.requestId)
    }

    private fun compatibleChat(model: String, task: AiTask, input: String): AiResponse {
        val body = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", AiPromptBuilder.system(task)))
                put(JSONObject().put("role", "user").put("content", input))
            })
        }
        val result = request("POST", endpoint("/chat/completions"), body)
        val root = parseObject(result.body)
        val choices = root.optJSONArray("choices") ?: JSONArray()
        val first = choices.optJSONObject(0)
        val content = first?.optJSONObject("message")?.optString("content", "").orEmpty()
        if (content.isBlank()) throw IOException("${runtime.config.provider.label} returned no text output")
        return AiResponse(content, runtime.config.provider.label, root.optString("model", model), result.requestId)
    }

    private fun geminiGenerate(model: String, task: AiTask, input: String): AiResponse {
        val safeModel = URLEncoder.encode(model.removePrefix("models/"), "UTF-8").replace("+", "%20")
        val body = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", AiPromptBuilder.system(task) + "\n\n" + input)))
            }))
            put("generationConfig", JSONObject().put("maxOutputTokens", 12_000))
        }
        val result = request("POST", endpoint("/models/$safeModel:generateContent"), body)
        val root = parseObject(result.body)
        val candidates = root.optJSONArray("candidates") ?: JSONArray()
        val parts = candidates.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts") ?: JSONArray()
        val text = buildString {
            for (index in 0 until parts.length()) {
                val part = parts.optJSONObject(index) ?: continue
                part.optString("text", "").takeIf { it.isNotBlank() }?.let {
                    if (isNotEmpty()) append('\n')
                    append(it)
                }
            }
        }
        if (text.isBlank()) {
            val reason = candidates.optJSONObject(0)?.optString("finishReason", "").orEmpty()
            throw IOException("Gemini returned no text output${if (reason.isBlank()) "" else " ($reason)"}")
        }
        return AiResponse(text, runtime.config.provider.label, model, result.requestId)
    }

    private fun parseOpenAiOutput(root: JSONObject): String {
        val output = root.optJSONArray("output") ?: JSONArray()
        return buildString {
            for (i in 0 until output.length()) {
                val content = output.optJSONObject(i)?.optJSONArray("content") ?: continue
                for (j in 0 until content.length()) {
                    val item = content.optJSONObject(j) ?: continue
                    val text = item.optString("text", "")
                    if (text.isNotBlank()) {
                        if (isNotEmpty()) append('\n')
                        append(text)
                    }
                }
            }
        }
    }

    private fun request(method: String, url: URL, body: JSONObject?): HttpResult {
        validateCredential()
        val connection = try {
            open(url, method)
        } catch (error: Exception) {
            throw friendlyNetworkError(error, url.host)
        }
        try {
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.use { String(it.readBytesLimited(MAX_RESPONSE_BYTES), Charsets.UTF_8) }.orEmpty()
            if (status !in 200..299) throw providerError(status, text)
            return HttpResult(text, connection.getHeaderField("x-request-id") ?: connection.getHeaderField("request-id"))
        } catch (error: Exception) {
            if (error is IOException && error.message?.startsWith("AI provider") == true) throw error
            throw friendlyNetworkError(error, url.host)
        } finally {
            connection.disconnect()
        }
    }

    private fun providerError(status: Int, body: String): IOException {
        val root = runCatching { JSONObject(body) }.getOrNull()
        val nested = root?.optJSONObject("error")
        val message = nested?.optString("message", "")?.ifBlank { null }
            ?: root?.optString("message", "")?.ifBlank { null }
        val type = nested?.optString("type", "").orEmpty()
        val friendly = when (status) {
            400 -> message ?: "The provider rejected this request. Check model ID, endpoint and context size."
            401 -> "API key is invalid, expired or belongs to a different provider."
            403 -> "The provider denied this request. The key or project lacks permission for the selected model."
            404 -> "Model or API endpoint was not found. Fetch models again or verify the base URL."
            408 -> "The AI provider timed out while processing the request."
            413 -> "The AI context is too large for this provider or model."
            429 -> if (type.contains("quota", true) || message?.contains("quota", true) == true) {
                "Provider quota or account balance is exhausted. Choose a free model, add billing, or retry after the quota resets."
            } else "Provider rate limit reached. Wait briefly or choose another provider/model."
            else -> message ?: "AI provider request failed with HTTP $status"
        }
        return IOException("AI provider error ($status): $friendly")
    }

    private fun validateCredential() {
        if (runtime.config.provider.keyRequired && runtime.apiKey.isNullOrBlank()) {
            throw IOException("${runtime.config.provider.label} API key is not configured. Open Credentials Center.")
        }
    }

    private fun endpoint(path: String): URL {
        val base = runtime.config.baseUrl.trimEnd('/')
        return URI(base + if (path.startsWith('/')) path else "/$path").toURL()
    }

    private fun open(url: URL, method: String): HttpURLConnection =
        (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 25_000
            readTimeout = 120_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "NovaIDE-Android-M6")
            val baseHost = URI(runtime.config.baseUrl).host
            if (url.host.equals(baseHost, ignoreCase = true)) {
                runtime.apiKey?.takeIf { it.isNotBlank() }?.let { key ->
                    if (runtime.config.provider == AiProviderId.GEMINI) {
                        setRequestProperty("x-goog-api-key", key)
                    } else {
                        setRequestProperty("Authorization", "Bearer $key")
                    }
                }
                if (runtime.config.provider == AiProviderId.OPENROUTER) {
                    setRequestProperty("HTTP-Referer", "https://github.com/mohnish-ydv")
                    setRequestProperty("X-Title", "NovaIDE")
                }
            }
        }

    private fun parseObject(value: String): JSONObject = runCatching { JSONObject(value) }
        .getOrElse { throw IOException("AI provider returned invalid JSON") }

    private fun friendlyNetworkError(error: Exception, host: String): IOException = when (error) {
        is UnknownHostException -> IOException("Could not resolve $host. Check internet, Private DNS, VPN or endpoint settings.", error)
        is SocketTimeoutException -> IOException("AI provider connection timed out. The model may be busy; retry or choose a faster model.", error)
        is SSLException -> IOException("Secure TLS connection to $host failed. Check device date/time, VPN or server certificate.", error)
        is IOException -> error
        else -> IOException(error.message ?: "AI network request failed", error)
    }

    private fun java.io.InputStream.readBytesLimited(limit: Long): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(32 * 1024)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) throw IOException("AI response exceeded the mobile safety limit")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
