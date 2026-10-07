/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A chat turn stays in memory unless the user keeps it on screen. */
data class AiChatTurn(val role: String, val text: String)

enum class AiProvider(
    val id: String,
    val label: String,
    val defaultModel: String,
    val keyHint: String
) {
    OpenAI("openai", "OpenAI / GPT", "gpt-4.1-mini", "sk-…"),
    Anthropic("anthropic", "Anthropic / Claude", "claude-haiku-4-5", "sk-ant-…"),
    Gemini("gemini", "Google / Gemini", "gemini-2.5-flash", "AIza…");

    companion object {
        fun fromId(id: String): AiProvider = entries.firstOrNull { it.id == id } ?: OpenAI
    }
}

object AiProviderClient {
    suspend fun send(
        provider: AiProvider,
        model: String,
        messages: List<AiChatTurn>,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        val safeModel = model.trim().take(100).ifBlank { provider.defaultModel }
        val safeMessages = messages.takeLast(20).filter { it.role == "user" || it.role == "assistant" }
        if (safeMessages.isEmpty()) throw IllegalArgumentException("Escreva uma mensagem primeiro.")
        when (provider) {
            AiProvider.OpenAI -> openAi(safeModel, safeMessages, apiKey)
            AiProvider.Anthropic -> anthropic(safeModel, safeMessages, apiKey)
            AiProvider.Gemini -> gemini(safeModel, safeMessages, apiKey)
        }
    }

    private fun openAi(model: String, messages: List<AiChatTurn>, key: String): String {
        val payload = JSONObject().put("model", model).put("messages", JSONArray().apply {
            messages.forEach { turn -> put(JSONObject().put("role", turn.role).put("content", turn.text)) }
        })
        val response = postJson(
            url = "https://api.openai.com/v1/chat/completions",
            headers = mapOf("Authorization" to "Bearer $key"),
            payload = payload
        )
        return response.optJSONArray("choices")?.optJSONObject(0)
            ?.optJSONObject("message")?.optString("content")?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw IOException("O provedor não retornou texto.")
    }

    private fun anthropic(model: String, messages: List<AiChatTurn>, key: String): String {
        val payload = JSONObject()
            .put("model", model)
            .put("max_tokens", 900)
            .put("messages", JSONArray().apply {
                messages.forEach { turn ->
                    put(JSONObject().put("role", turn.role).put("content", turn.text))
                }
            })
        val response = postJson(
            url = "https://api.anthropic.com/v1/messages",
            headers = mapOf("x-api-key" to key, "anthropic-version" to "2023-06-01"),
            payload = payload
        )
        val content = response.optJSONArray("content") ?: JSONArray()
        return (0 until content.length()).mapNotNull { content.optJSONObject(it)?.optString("text") }
            .filter { it.isNotBlank() }.joinToString("\n")
            .ifBlank { throw IOException("O provedor não retornou texto.") }
    }

    private fun gemini(model: String, messages: List<AiChatTurn>, key: String): String {
        val contents = JSONArray().apply {
            messages.forEach { turn ->
                put(
                    JSONObject()
                        .put("role", if (turn.role == "assistant") "model" else "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", turn.text)))
                )
            }
        }
        val encodedModel = URLEncoder.encode(model, Charsets.UTF_8.name())
        val encodedKey = URLEncoder.encode(key, Charsets.UTF_8.name())
        val response = postJson(
            url = "https://generativelanguage.googleapis.com/v1beta/models/$encodedModel:generateContent?key=$encodedKey",
            headers = emptyMap(),
            payload = JSONObject().put("contents", contents)
        )
        val parts = response.optJSONArray("candidates")?.optJSONObject(0)
            ?.optJSONObject("content")?.optJSONArray("parts") ?: JSONArray()
        return (0 until parts.length()).mapNotNull { parts.optJSONObject(it)?.optString("text") }
            .filter { it.isNotBlank() }.joinToString("\n")
            .ifBlank { throw IOException("O provedor não retornou texto.") }
    }

    private fun postJson(url: String, headers: Map<String, String>, payload: JSONObject): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }
        try {
            connection.outputStream.use { stream -> stream.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IOException(errorMessage(status, body))
            return runCatching { JSONObject(body) }.getOrElse { throw IOException("Resposta inválida do provedor.") }
        } finally {
            connection.disconnect()
        }
    }

    private fun errorMessage(status: Int, body: String): String {
        val parsed = runCatching {
            val json = JSONObject(body)
            json.optJSONObject("error")?.optString("message")
                ?.takeIf { it.isNotBlank() }
                ?: json.optString("message").takeIf { it.isNotBlank() }
        }.getOrNull()
        val safeMessage = parsed?.take(240)
        return if (safeMessage == null) "Falha do provedor (HTTP $status). Confira a chave e o modelo configurados."
        else "Falha do provedor (HTTP $status): $safeMessage"
    }
}
