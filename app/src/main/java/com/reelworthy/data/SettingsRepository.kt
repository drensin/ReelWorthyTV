package com.reelworthy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Represents the user's configurable settings.
 *
 * @property aiModel The ID of the selected AI model (e.g., "gemini-1.5-flash").
 * @property isDeepThinkingEnabled Whether "deep thinking" mode is active (unused currently).
 * @property selectedPlaylistIds A set of YouTube Playlist IDs to sync.
 */
data class UserSettings(
    val aiModel: String,
    val isDeepThinkingEnabled: Boolean,
    val selectedPlaylistIds: Set<String>,
    val includeSubscriptionFeed: Boolean = false
)

/**
 * Manages persistence of user preferences using [DataStore].
 *
 * Handles reading and writing of settings like selected AI model and playlists.
 *
 * @property context The application context, used to access DataStore.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val AI_MODEL = stringPreferencesKey("ai_model")
        val DEEP_THINKING = booleanPreferencesKey("deep_thinking")
        val SELECTED_PLAYLISTS = stringSetPreferencesKey("selected_playlists")
        val SUBSCRIPTION_FEED = booleanPreferencesKey("subscription_feed")
    }

    /**
     * A Flow emitting current [UserSettings]. Updates in real-time as preferences change.
     */
    val settingsFlow: Flow<UserSettings> = context.dataStore.data
        .map { preferences ->
            UserSettings(
                aiModel = preferences[Keys.AI_MODEL] ?: "gemini-3-flash-preview",
                isDeepThinkingEnabled = preferences[Keys.DEEP_THINKING] ?: false,
                selectedPlaylistIds = preferences[Keys.SELECTED_PLAYLISTS] ?: emptySet(),
                includeSubscriptionFeed = preferences[Keys.SUBSCRIPTION_FEED] ?: false
            )
        }

    /**
     * Updates the selected AI model.
     * @param model The model ID string.
     */
    suspend fun updateAiModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.AI_MODEL] = model
        }
    }

    suspend fun updateDeepThinking(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DEEP_THINKING] = enabled
        }
    }

    /**
     * Updates the set of selected playlist IDs.
     * @param playlistIds The new set of IDs to persist.
     */
    suspend fun updateSelectedPlaylists(playlistIds: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SELECTED_PLAYLISTS] = playlistIds
        }
    }

    suspend fun updateIncludeSubscriptionFeed(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SUBSCRIPTION_FEED] = enabled
        }
    }

    /**
     * Fetches available Gemini models from the API to populate the settings dropdown.
     *
     * @param apiKey The API Key for Gemini.
     * @return A list of model names (e.g., "gemini-1.5-flash").
     */
    suspend fun fetchAvailableModels(apiKey: String): List<String> {
        return try {
            val response = RetrofitClient.geminiApi.getModels(apiKey)
            response.models
                .map { it.name } // Format: "models/gemini-1.5-flash"
                .filter { modelName ->
                    val lower = modelName.lowercase()
                    lower.contains("gemini") && !lower.contains("vision") && !lower.contains("text-to-speech")
                }
                .map { it.removePrefix("models/") } 
                .sortedDescending()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepository", "Error fetching models", e)
            emptyList()
        }
    }
}
