package io.github.arfaouiahmed1.pursivo.core.ai

import io.github.arfaouiahmed1.pursivo.core.model.ProviderConfig
import io.github.arfaouiahmed1.pursivo.core.model.ProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultProviderClientFactory @Inject constructor(
    private val client: OkHttpClient,
) : ProviderClientFactory {
    override fun create(config: ProviderConfig, apiKey: String): AiProvider = when (config.type) {
        ProviderType.OPENAI -> OpenAiResponsesProvider(client, config, apiKey)
        ProviderType.OPENAI_COMPATIBLE -> OpenAiProvider(client, config, apiKey, config.baseUrl.trimEnd('/'))
        ProviderType.ANTHROPIC -> AnthropicProvider(client, config, apiKey)
        ProviderType.GEMINI -> GeminiProvider(client, config, apiKey)
    }
}

private class OpenAiResponsesProvider(
    client: OkHttpClient,
    config: ProviderConfig,
    apiKey: String,
) : BaseProvider(client, config, apiKey) {
    override suspend fun testConnection(): ConnectionResult = runCatching {
        val request = Request.Builder()
            .url("https://api.openai.com/v1/models/${config.model}")
            .header("Authorization", "Bearer $apiKey")
            .build()
        execute(request)
        ConnectionResult(true, "Connection and model verified")
    }.getOrElse { ConnectionResult(false, it.message ?: "Connection failed") }

    override suspend fun generateJson(systemPrompt: String, userPrompt: String): String {
        val payload = buildJsonObject {
            put("model", config.model)
            put("instructions", systemPrompt)
            put("input", userPrompt)
            put("text", buildJsonObject {
                put("format", buildJsonObject { put("type", "json_object") })
            })
        }
        val request = Request.Builder()
            .url("https://api.openai.com/v1/responses")
            .header("Authorization", "Bearer $apiKey")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        val result = json.parseToJsonElement(execute(request)).jsonObject
        return result["output"]!!.jsonArray
            .flatMap { it.jsonObject["content"]?.jsonArray.orEmpty() }
            .first { it.jsonObject["type"]?.jsonPrimitive?.contentOrNull == "output_text" }
            .jsonObject["text"]!!.jsonPrimitive.content
    }
}

private val json = Json { ignoreUnknownKeys = true }
private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

private abstract class BaseProvider(
    protected val client: OkHttpClient,
    protected val config: ProviderConfig,
    protected val apiKey: String,
) : AiProvider {
    protected suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw AiProviderException(response.code, providerMessage(response.code))
            body
        }
    }

    private fun providerMessage(code: Int) = when (code) {
        401, 403 -> "The provider rejected this API key."
        404 -> "The selected model or endpoint was not found."
        429 -> "The provider rate limit or quota was reached."
        in 500..599 -> "The provider is temporarily unavailable."
        else -> "The provider request failed ($code)."
    }
}

class AiProviderException(val statusCode: Int, override val message: String) : Exception(message)

private class OpenAiProvider(
    client: OkHttpClient,
    config: ProviderConfig,
    apiKey: String,
    private val baseUrl: String,
) : BaseProvider(client, config, apiKey) {
    init { require(baseUrl.startsWith("https://")) { "Only HTTPS provider endpoints are supported" } }

    override suspend fun testConnection(): ConnectionResult = runCatching {
        val request = Request.Builder().url("$baseUrl/models").header("Authorization", "Bearer $apiKey").build()
        execute(request)
        ConnectionResult(true, "Connection verified")
    }.getOrElse { ConnectionResult(false, it.message ?: "Connection failed") }

    override suspend fun generateJson(systemPrompt: String, userPrompt: String): String {
        val payload = buildJsonObject {
            put("model", config.model)
            put("temperature", 0.2)
            put("response_format", buildJsonObject { put("type", "json_object") })
            put("messages", buildJsonArray {
                add(buildJsonObject { put("role", "system"); put("content", systemPrompt) })
                add(buildJsonObject { put("role", "user"); put("content", userPrompt) })
            })
        }
        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        val result = json.parseToJsonElement(execute(request)).jsonObject
        return result["choices"]!!.jsonArray[0].jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.content
    }
}

private class AnthropicProvider(
    client: OkHttpClient,
    config: ProviderConfig,
    apiKey: String,
) : BaseProvider(client, config, apiKey) {
    override suspend fun testConnection(): ConnectionResult = runCatching {
        generateJson("Reply using JSON.", "Return {\"status\":\"ok\"} only.")
        ConnectionResult(true, "Connection verified")
    }.getOrElse { ConnectionResult(false, it.message ?: "Connection failed") }

    override suspend fun generateJson(systemPrompt: String, userPrompt: String): String {
        val payload = buildJsonObject {
            put("model", config.model)
            put("max_tokens", 4096)
            put("temperature", 0.2)
            put("system", systemPrompt)
            put("messages", buildJsonArray {
                add(buildJsonObject { put("role", "user"); put("content", userPrompt) })
            })
        }
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        val result = json.parseToJsonElement(execute(request)).jsonObject
        return result["content"]!!.jsonArray.first { it.jsonObject["type"]?.jsonPrimitive?.contentOrNull == "text" }
            .jsonObject["text"]!!.jsonPrimitive.content
    }
}

private class GeminiProvider(
    client: OkHttpClient,
    config: ProviderConfig,
    apiKey: String,
) : BaseProvider(client, config, apiKey) {
    override suspend fun testConnection(): ConnectionResult = runCatching {
        val request = Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey").build()
        execute(request)
        ConnectionResult(true, "Connection verified")
    }.getOrElse { ConnectionResult(false, it.message ?: "Connection failed") }

    override suspend fun generateJson(systemPrompt: String, userPrompt: String): String {
        val payload = buildJsonObject {
            put("system_instruction", buildJsonObject {
                put("parts", buildJsonArray { add(buildJsonObject { put("text", systemPrompt) }) })
            })
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray { add(buildJsonObject { put("text", userPrompt) }) })
                })
            })
            put("generationConfig", buildJsonObject {
                put("temperature", 0.2)
                put("responseMimeType", "application/json")
            })
        }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${config.model}:generateContent?key=$apiKey"
        val request = Request.Builder().url(url).post(payload.toString().toRequestBody(jsonMediaType)).build()
        val result = json.parseToJsonElement(execute(request)).jsonObject
        return result["candidates"]!!.jsonArray[0].jsonObject["content"]!!.jsonObject["parts"]!!.jsonArray[0]
            .jsonObject["text"]!!.jsonPrimitive.content
    }
}
