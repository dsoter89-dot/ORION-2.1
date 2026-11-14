package fr.orionai.app

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ChatRepository {
    private val client = OkHttpClient.Builder()
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    fun send(settings: AppSettings, history: List<Pair<String, String>>): String {
        require(settings.endpoint.startsWith("https://")) { "L'URL doit utiliser HTTPS." }
        require(settings.apiKey.isNotBlank()) { "Ajoute ta clé API dans Réglages." }

        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", settings.systemPrompt))
        history.takeLast(24).forEach { (role, content) ->
            messages.put(JSONObject().put("role", role).put("content", content))
        }
        val body = JSONObject()
            .put("model", settings.model)
            .put("messages", messages)
            .put("temperature", 0.7)
        val request = Request.Builder()
            .url(settings.endpoint)
            .header("Authorization", "Bearer ${settings.apiKey}")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("Erreur fournisseur (${response.code}) : ${raw.take(400)}")
            val json = JSONObject(raw)
            return json.getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content")
        }
    }
}
