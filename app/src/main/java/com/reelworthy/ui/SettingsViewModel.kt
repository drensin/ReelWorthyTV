package com.reelworthy.ui

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reelworthy.data.AuthRepository
import com.reelworthy.data.PlaylistEntity
import com.reelworthy.data.SettingsRepository
import com.reelworthy.data.UserSettings
import com.reelworthy.data.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI State container for the [SettingsScreen].
 * @property settings The current persisted user settings.
 * @property allPlaylists List of all imported playlists from the DB.
 * @property isSyncing True if a playlist synchronization is currently active.
 * @property syncMessage Status message for the current sync operation.
 */
data class SettingsUiState(
        val settings: UserSettings =
                UserSettings("gemini-3-flash-preview", false, emptySet(), false),
        val allPlaylists: List<PlaylistEntity> = emptyList(),
        val isSyncing: Boolean = false,
        val syncMessage: String? = null,
        val authError: Boolean = false
)

/**
 * ViewModel for the [SettingsScreen].
 *
 * Responsibilities:
 * - Aggregating state from [SettingsRepository] and [VideoRepository] into a single
 * [SettingsUiState].
 * - Handling playlist imports (fetching from YouTube and saving to DB).
 * - Triggering deep-sync logic for selected playlists (fetching video details + duration).
 * - Managing AI model selection.
 */
class SettingsViewModel(
        application: android.app.Application,
        private val settingsRepository: SettingsRepository,
        private val videoRepository: VideoRepository,
        private val authRepository: AuthRepository,
        private val apiKey: String
) : AndroidViewModel(application) {

    // Internal mutable state for transient UI properties
    private val _isSyncing = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)
    private val _authError = MutableStateFlow(false)

    /**
     * Combined UI State flow. Merges UserSettings, Playlists, and Transient Sync State into one
     * observable object.
     */
    val uiState: StateFlow<SettingsUiState> =
            combine(
                            settingsRepository.settingsFlow,
                            videoRepository.allPlaylists,
                            _isSyncing,
                            _syncMessage,
                            _authError
                    ) { settings, playlists, isSyncing, syncMessage, authError ->
                        SettingsUiState(settings, playlists, isSyncing, syncMessage, authError)
                    }
                    .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = SettingsUiState()
                    )

    // Dynamic models list
    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    init {
        fetchModels()
    }

    /** Fetches available Gemini models from the API. Updates [availableModels] state. */
    private fun fetchModels() {
        viewModelScope.launch {
            val models = settingsRepository.fetchAvailableModels(apiKey)
            if (models.isNotEmpty()) {
                _availableModels.value = models
            } else {
                // Fallback if fetch fails
                _availableModels.value = SettingsConstants.AVAILABLE_MODELS
            }
        }
    }

    /**
     * Updates the preferred AI model by delegating to the repository.
     *
     * @param modelId The ID of the selected model (e.g., "gemini-1.5-flash").
     */
    fun onModelSelected(modelId: String) {
        viewModelScope.launch { settingsRepository.updateAiModel(modelId) }
    }

    /**
     * Enables or disables "Deep Thinking" mode.
     *
     * @param enabled True to enable.
     */
    fun onDeepThinkingChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateDeepThinking(enabled) }
    }

    /**
     * Toggles the inclusion of recent subscription videos in the AI context.
     *
     * @param enabled True to include the subscription feed.
     */
    fun onSubscriptionFeedChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateIncludeSubscriptionFeed(enabled) }
    }

    /**
     * Toggles a playlist's selection status.
     *
     * If selected, immediately triggers a "deep sync" to fetch all videos in that playlist. This is
     * crucial for populating the vector context.
     *
     * @param playlistId The ID of the playlist to toggle.
     * @param isSelected The new selection state.
     */
    fun onPlaylistSelectionChanged(playlistId: String, isSelected: Boolean) {
        viewModelScope.launch {
            val currentSet = uiState.value.settings.selectedPlaylistIds.toMutableSet()
            if (isSelected) {
                currentSet.add(playlistId)
                // Trigger deep sync
                _isSyncing.value = true
                _syncMessage.value = "Syncing videos..."
                try {
                    val token = authRepository.getAccessToken(getApplication())
                    videoRepository.fetchPlaylistVideos(playlistId, token, apiKey)
                    _isSyncing.value = false
                    _syncMessage.value = "Sync complete!"
                } catch (e: Exception) {
                    android.util.Log.e("SettingsViewModel", "Sync error", e)
                    _isSyncing.value = false
                    _syncMessage.value = "Sync failed: ${e.message}"
                    e.printStackTrace()
                }
            } else {
                currentSet.remove(playlistId)
            }
            settingsRepository.updateSelectedPlaylists(currentSet)
        }
    }

    /** Dismisses the current sync status message. */
    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    /**
     * Fetches top-level playlists for the user from YouTube. Requires a valid Google Sign-In access
     * token.
     */
    /**
     * Fetches top-level playlists for the user from YouTube. Requires a valid Google Sign-In access
     * token.
     */
    fun fetchPlaylists() {
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                android.util.Log.d("SettingsViewModel", "Fetching playlists... getting token.")
                val token = authRepository.getAccessToken(getApplication())
                if (token != null) {
                    android.util.Log.d("SettingsViewModel", "Token retrieved: ${token.take(10)}...")
                    videoRepository.fetchUserPlaylists(token)
                    _authError.value = false
                } else {
                    android.util.Log.e(
                            "SettingsViewModel",
                            "No access token available. User might not be signed in or GoogleSignIn account is null."
                    )
                    _authError.value = true
                    _syncMessage.value = "Auth failed. Please sign in again."
                }
                _isSyncing.value = false
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error fetching playlists", e)
                e.printStackTrace()
                _isSyncing.value = false
            }
        }
    }

    /**
     * Re-syncs everything: Models, Playlist definitions, and Video Content (Playlists +
     * Subscriptions).
     */
    fun syncEverything() {
        viewModelScope.launch {
            if (_isSyncing.value) return@launch

            _isSyncing.value = true
            try {
                // 1. Fetch Models
                _syncMessage.value = "Updating AI Models..."
                fetchModels()

                // 2. Refresh Playlist Definitions (List of playlists)
                // Note: This only updates available playlists in DB; it preserves UserSettings
                // selections via Repository logic.
                _syncMessage.value = "Updating Playlists..."
                val token = authRepository.getAccessToken(getApplication())
                if (token == null) {
                    _authError.value = true
                    _syncMessage.value = "Auth failed. Sign in required."
                    _isSyncing.value = false
                    return@launch
                }
                _authError.value = false
                videoRepository.fetchUserPlaylists(token)

                // 3. Sync Content for Selected Playlists
                val selectedIds = uiState.value.settings.selectedPlaylistIds
                if (selectedIds.isNotEmpty()) {
                    _syncMessage.value = "Syncing ${selectedIds.size} playlists..."
                    selectedIds.forEachIndexed { index, playlistId ->
                        _syncMessage.value = "Syncing playlist ${index + 1}/${selectedIds.size}..."
                        videoRepository.fetchPlaylistVideos(playlistId, token, apiKey)
                    }
                }

                // 4. Sync Subscriptions (if enabled)
                if (uiState.value.settings.includeSubscriptionFeed) {
                    _syncMessage.value = "Syncing subscriptions..."
                    videoRepository.fetchRecentSubscriptionVideos(token, apiKey)
                }

                _syncMessage.value = "Sync Complete!"
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Global Sync Error", e)
                _syncMessage.value = "Sync failed: ${e.message}"
            } finally {
                _isSyncing.value = false
                // Clear success message after delay
                kotlinx.coroutines.delay(3000)
                if (_syncMessage.value == "Sync Complete!") {
                    _syncMessage.value = null
                }
            }
        }
    }
}
