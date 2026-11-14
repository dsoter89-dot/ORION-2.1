package fr.orionai.app

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore("orion_settings")

data class AppSettings(
    val assistantName: String = "Orion",
    val endpoint: String = "https://api.openai.com/v1/chat/completions",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val systemPrompt: String = "Tu es Orion, un assistant polyvalent, fiable et prudent. Réponds en français. Demande confirmation avant toute action ayant un effet externe ou irréversible.",
    val wakeWord: String = "Bonjour Orion",
    val confirmImportantActions: Boolean = true
)

class SettingsStore(private val context: Context) {
    private val name = stringPreferencesKey("assistant_name")
    private val endpoint = stringPreferencesKey("endpoint")
    private val key = stringPreferencesKey("api_key")
    private val model = stringPreferencesKey("model")
    private val prompt = stringPreferencesKey("system_prompt")
    private val wake = stringPreferencesKey("wake_word")
    private val confirmActions = stringPreferencesKey("confirm_important_actions")

    suspend fun load(): AppSettings {
        val p = context.dataStore.data.first()
        return AppSettings(
            assistantName = p[name] ?: "Orion",
            endpoint = p[endpoint] ?: "https://api.openai.com/v1/chat/completions",
            apiKey = p[key] ?: "",
            model = p[model] ?: "gpt-4o-mini",
            systemPrompt = p[prompt] ?: AppSettings().systemPrompt,
            wakeWord = p[wake] ?: "Bonjour Orion",
            confirmImportantActions = p[confirmActions] != "false"
        )
    }

    suspend fun save(s: AppSettings) {
        context.dataStore.edit {
            it[name] = s.assistantName
            it[endpoint] = s.endpoint
            it[key] = s.apiKey
            it[model] = s.model
            it[prompt] = s.systemPrompt
            it[wake] = s.wakeWord
            it[confirmActions] = s.confirmImportantActions.toString()
        }
    }
}
