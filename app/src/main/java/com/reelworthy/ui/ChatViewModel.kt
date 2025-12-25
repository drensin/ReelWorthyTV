package com.reelworthy.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reelworthy.data.ChatRepository
import com.reelworthy.data.VideoDao
import com.reelworthy.data.SettingsRepository
import com.reelworthy.data.VideoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents a single message in the chat conversation.
 * @property id Unique ID for the message.
 * @property text The text content of the message.
 * @property isUser True if sent by the user, false if by AI.
 * @property timestamp Time of creation.
 * @property recommendations List of videos recommended in this message (if any).
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val recommendations: List<RecommendedVideo> = emptyList() // New field
)

/**
 * Wrapper for a recommended video entity containing the AI's reasoning.
 * @property video The full [VideoEntity] from the database.
 * @property reason The explanation text.
 */
data class RecommendedVideo(
    val video: VideoEntity,
    val reason: String
)

/**
 * ViewModel for the [DashboardScreen].
 *
 * Manages the state of the chat conversation, including:
 * - Sending user queries to the [ChatRepository].
 * - Handling streaming responses (Thinking mode).
 * - Hydrating AI recommendations with local database objects.
 * - Exposing UI state ([messages], [isLoading], [currentStreamingText]).
 *
 * @property apiKey The Gemini API Key.
 * @property videoDao DAO for accessing video data.
 * @property settingsRepository Repository for user preferences.
 */
class ChatViewModel(
    private val apiKey: String,
    private val videoDao: VideoDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val repository = ChatRepository(apiKey, videoDao, settingsRepository)
    
    // The list of completed chat messages
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Loading state for the AI generation process
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Real-time text stream for the "Thinking" overlay
    private val _currentStreamingText = MutableStateFlow("")
    val currentStreamingText: StateFlow<String> = _currentStreamingText.asStateFlow()

    /**
     * Sends a user query to the AI and processes the streaming response.
     *
     * 1. Adds user message to the list immediately.
     * 2. Sets loading state and clears streaming text.
     * 3. Collects the flow from [ChatRepository.getRecommendations].
     * 4. Updates [currentStreamingText] in real-time.
     * 5. Upon completion, hydrates video IDs to full entities and triggers the final AI message.
     *
     * @param text The user's query string.
     */
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(text = text, isUser = true)
        _messages.value = _messages.value + userMsg
        
        viewModelScope.launch {
            _isLoading.value = true
            _currentStreamingText.value = "Thinking..."
            Log.d("ChatViewModel", "Sending query... (Streaming Mode)")
            
            var finalAccumulatedText = ""
            var finalRecs = listOf<RecommendedVideo>()
            
            try {
                // Collect streaming updates
                repository.getRecommendations(text).collect { update ->
                    val accumulatedText = update.text
                    val isComplete = update.isComplete
                    
                    // Update streaming text independently of the main message list
                    _currentStreamingText.value = accumulatedText
                    finalAccumulatedText = accumulatedText
                    
                    if (isComplete && update.recommendations.isNotEmpty()) {
                        // Hydrate only on completion
                         val allVideos = videoDao.getAllVideosList()
                         val videoMap = allVideos.associateBy { it.id }
                         
                         val hydratedRecs = mutableListOf<RecommendedVideo>()
                         update.recommendations.forEach { rec ->
                            val entity = videoMap[rec.videoId]
                            if (entity != null) {
                                hydratedRecs.add(RecommendedVideo(entity, rec.reason))
                            }
                         }
                         finalRecs = hydratedRecs
                    }
                }
                
                // Stream complete. Now add the final message to the list.
                val aiMsg = ChatMessage(
                    text = finalAccumulatedText,
                    isUser = false,
                    recommendations = finalRecs
                )
                _messages.value = _messages.value + aiMsg
                
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error getting recs", e)
                 val errorMsg = ChatMessage(
                     text = "Error: ${e.message}",
                     isUser = false
                 )
                 _messages.value = _messages.value + errorMsg
            } finally {
                _isLoading.value = false
                _currentStreamingText.value = ""
            }
        }
    }
}
