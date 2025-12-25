package com.reelworthy.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Represents a specific video recommendation returned by the AI.
 * @property videoId The ID of the recommended video suitable for lookup.
 * @property reason The AI's explanation for why this video matches the user's query.
 */
data class AiRecommendation(
    val videoId: String,
    val reason: String
)

/**
 * Represents a real-time update from the AI chat stream.
 * @property text The accumulated text content (including "thinking" process).
 * @property isComplete True if the stream has finished.
 * @property recommendations The parsed list of video recommendations (populated only when complete).
 */
data class ChatStreamUpdate(
    val text: String,
    val isComplete: Boolean = false,
    val recommendations: List<AiRecommendation> = emptyList()
)

/**
 * Manages interactions with the Generative AI (Gemini).
 *
 * Handles:
 * 1. Fetching local video context from the database.
 * 2. Constructing the system prompt with context and user query.
 * 3. Streaming the response from Gemini.
 * 4. Parsing the unstructured (or semi-structured) response into Objects.
 *
 * @property apiKey The Gemini API Key.
 * @property videoDao Used to fetch the list of available videos to recommend from.
 * @property settingsRepository Used to fetch user preferences (Model, Deep Thinking).
 */
class ChatRepository(
    private val apiKey: String,
    private val videoDao: VideoDao,
    private val settingsRepository: SettingsRepository
) {
    
    /**
     * Generates recommendations for a user query.
     *
     * This method:
     * 1. Reads the current AI Settings.
     * 2. Dumps the entire local video database into a JSON context string.
     * 3. Streams the AI response, masking the raw JSON output from the user.
     * 4. Parses the final JSON block into [AiRecommendation] objects.
     *
     * @param userQuery The user's search query (e.g., "Show me something funny").
     * @return A Flow of [ChatStreamUpdate] chunks.
     */
    fun getRecommendations(userQuery: String): Flow<ChatStreamUpdate> = flow {
        // 0. Fetch Settings (need to be suspend-friendly, so we do this inside flow builder)
        val settings = settingsRepository.settingsFlow.first()
        val aiModelName = settings.aiModel
        val deepThinking = settings.isDeepThinkingEnabled
        
        Log.d("ChatRepository", "Using Model: $aiModelName, DeepThinking: $deepThinking")

        val generativeModel = GenerativeModel(
            modelName = aiModelName, 
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = if (deepThinking) 0.7f else 0.4f
                // responseMimeType = "application/json" // REMOVED to allow thinking text
            }
        )

        // 1. Fetch Context
        val allVideos = videoDao.getAllVideosList()
        Log.d("ChatRepository", "Database contains ${allVideos.size} videos.")
        
        if (allVideos.isEmpty()) {
            emit(ChatStreamUpdate("I don't have any videos in my database yet. Please go to Settings > Playlists and select a playlist to sync.", true))
            return@flow
        }

        // 2. Build Context
        val videoContext = allVideos.joinToString(separator = ",", prefix = "[", postfix = "]") { video ->
            """
            {
                "id": "${video.id}",
                "title": "${video.title.replace("\"", "\\\"")}",
                "description": "${video.description.take(200).replace("\"", "\\\"").replace("\n", " ")}...", 
                "channel": "${video.channelTitle.replace("\"", "\\\"")}"
            }
            """.trimIndent()
        }

        // 3. Build Prompt (Updated for Thinking)
        val systemPrompt = """
            You are a video recommendation assistant. 
            Using Model: $aiModelName. Deep Thinking Mode: $deepThinking.
            
            I will provide a 'Video List' JSON. 
            Your task is to recommend videos from this list that best match the 'User Query'.
            
            Video List (JSON):
            $videoContext

            User Query: $userQuery

            Instructions:
            1. First, THINK step-by-step about which videos match the query and why. Output this thinking process naturally.
            2. After analyzing, identify ALL videos that are relevant to the query. Do not arbitrarily limit the number of results. If 10 videos match, return 10.
            3. Prioritize relevance, but be generous in your selection to give the user plenty of options.
            4. FINALLY, output the result in a strict JSON block wrapped in ```json ... ```.
            
            The JSON structure MUST be:
            {
              "answer": "A friendly conversational response to the user summarizing the recommendations.",
              "suggestedVideos": [
                { "videoId": "THE_VIDEO_ID", "reason": "Why you chose this video" }
              ]
            }
            
            If NO videos match, return an empty array for suggestedVideos and explain why in the answer.
            Only suggest videos that actually exist in the provided list.
        """.trimIndent()

        val msg = content {
            text(systemPrompt)
        }

        try {
            Log.d("ChatRepository", "Sending prompt...")
            var accumulatedText = ""
            
            generativeModel.generateContentStream(msg).collect { chunk ->
                val chunkText = chunk.text ?: ""
                accumulatedText += chunkText
                
                // Mask JSON output
                if (accumulatedText.contains("```json")) {
                    val visibleText = accumulatedText.substringBefore("```json") + "\n\n[Receiving structured results... One moment.]"
                    emit(ChatStreamUpdate(visibleText, false))
                } else {
                    emit(ChatStreamUpdate(accumulatedText, false))
                }
            }
            
            // Post-process: Extract JSON
            val (finalAnswer, recs) = parseResponseWithThinking(accumulatedText)
            emit(ChatStreamUpdate(finalAnswer, true, recs))
            
        } catch (e: Exception) {
            Log.e("ChatRepository", "Gemini Error", e)
            emit(ChatStreamUpdate("Sorry, I encountered an error: ${e.message}", true))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Extracts the JSON block from the mixed text/JSON response.
     * Fallback to simple brace matching if code blocks are missing.
     */
    private fun parseResponseWithThinking(fullText: String): Pair<String, List<AiRecommendation>> {
        // Try to find JSON block
        val jsonStart = fullText.indexOf("```json")
        val jsonEnd = fullText.lastIndexOf("```")
        
        if (jsonStart != -1 && jsonEnd > jsonStart) {
            val jsonString = fullText.substring(jsonStart + 7, jsonEnd).trim()
            return parseJson(jsonString)
        } else {
            // Fallback: Try to find just the JSON brace structure if backticks missing
            val braceStart = fullText.indexOf("{")
            val braceEnd = fullText.lastIndexOf("}")
            if (braceStart != -1 && braceEnd > braceStart) {
                 val jsonString = fullText.substring(braceStart, braceEnd + 1)
                 // This is risky if thinking contains braces, but robust enough for now
                 return parseJson(jsonString)
            }
        }
        
        // If no JSON found, return the whole text as answer
        return fullText to emptyList()
    }

    private fun parseJson(jsonString: String): Pair<String, List<AiRecommendation>> {
         return try {
            val jsonObject = JSONObject(jsonString)
            val answer = jsonObject.optString("answer", "Here are some videos.")
            val suggestionsArray = jsonObject.optJSONArray("suggestedVideos") 
            
            val recommendations = mutableListOf<AiRecommendation>()
            if (suggestionsArray != null) {
                for (i in 0 until suggestionsArray.length()) {
                    val item = suggestionsArray.getJSONObject(i)
                    recommendations.add(
                        AiRecommendation(
                            videoId = item.getString("videoId"),
                            reason = item.optString("reason", "")
                        )
                    )
                }
            }
            answer to recommendations
        } catch (e: Exception) {
            Log.e("ChatRepository", "JSON Parse Error", e)
            "I found some videos but couldn't parse the details properly." to emptyList()
        }
    }
}
