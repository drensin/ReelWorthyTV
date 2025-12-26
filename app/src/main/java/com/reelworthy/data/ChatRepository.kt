package com.reelworthy.data

import android.util.Log
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Represents a specific video recommendation returned by the AI.
 * @property videoId The ID of the recommended video suitable for lookup.
 * @property reason The AI's explanation for why this video matches the user's query.
 */
data class AiRecommendation(val videoId: String, val reason: String)

/**
 * Represents a real-time update from the AI chat stream.
 * @property text The accumulated text content (including "thinking" process).
 * @property isComplete True if the stream has finished.
 * @property recommendations The parsed list of video recommendations (populated only when
 * complete).
 */
data class ChatStreamUpdate(
        val text: String,
        val isComplete: Boolean = false,
        val recommendations: List<AiRecommendation> = emptyList()
)

/**
 * Manages interactions with the Generative AI (Gemini) using Retrofit and OkHttp.
 *
 * This repository handles the complex logic of communicating with the Gemini API via direct REST
 * calls, including:
 * 1. **Context Assembly**: Fetching local video data from [VideoDao] and formatting it into a
 * system prompt.
 * 2. **Manual Payload Construction**: Generating the specific JSON structure required by the Gemini
 * API,
 * ```
 *     including the placement of `thinking_config` within `generationConfig` for reasoning models.
 * ```
 * 3. **Streaming & Parsing**: Managing the SSE (Server-Sent Events) stream, parsing raw JSON
 * chunks,
 * ```
 *     and extracting both "thinking" content and the final structured JSON response.
 *
 * @property apiKey
 * ```
 * The Gemini API Key.
 * @property videoDao Used to fetch the list of available videos to recommend from.
 * @property settingsRepository Used to fetch user preferences (Model, Deep Thinking).
 */
class ChatRepository(
        private val apiKey: String,
        private val videoDao: VideoDao,
        private val settingsRepository: SettingsRepository
) {

        // Initialize Retrofit Service
        private val geminiService: GeminiService by lazy {
                val retrofit =
                        Retrofit.Builder()
                                .baseUrl("https://generativelanguage.googleapis.com/")
                                .client(OkHttpClient.Builder().build())
                                .addConverterFactory(GsonConverterFactory.create())
                                .build()
                retrofit.create(GeminiService::class.java)
        }

        /**
         * Generates video recommendations based on a user query.
         *
         * This method performs the following steps:
         * 1. Retrieves current settings (AI Model, Deep Thinking enabled/disabled).
         * 2. Fetches all video metadata from the local database.
         * 3. Constructs a prompt containing the video list and the user's query.
         * 4. Builds the JSON request body manually using [JSONObject]/Maps to ensure correct
         * structure,
         * ```
         *     specifically handling the experimental `thinking_config` field.
         * ```
         * 5. Executes the streaming API call via [GeminiService].
         * 6. Parses the SSE stream line-by-line to emit real-time [ChatStreamUpdate] events.
         *
         * @param userQuery The user's natural language search query (e.g., "Show me something
         * funny").
         * @return A [Flow] of [ChatStreamUpdate] objects, representing chunks of the AI's response
         * (both thinking and text).
         */
        fun getRecommendations(userQuery: String): Flow<ChatStreamUpdate> =
                flow {
                                // 0. Fetch Settings
                                val settings = settingsRepository.settingsFlow.first()
                                val aiModelName = settings.aiModel
                                val deepThinking = settings.isDeepThinkingEnabled

                                Log.d(
                                        "ChatRepository",
                                        "Using Model: $aiModelName, DeepThinking: $deepThinking"
                                )

                                // 1. Fetch Context
                                val allVideos = videoDao.getAllVideosList()
                                Log.d(
                                        "ChatRepository",
                                        "Database contains ${allVideos.size} videos."
                                )

                                if (allVideos.isEmpty()) {
                                        emit(
                                                ChatStreamUpdate(
                                                        "I don't have any videos in my database yet. Please go to Settings > Playlists and select a playlist to sync.",
                                                        true
                                                )
                                        )
                                        return@flow
                                }

                                // 2. Build Context
                                val videoContext =
                                        allVideos.joinToString(
                                                separator = ",",
                                                prefix = "[",
                                                postfix = "]"
                                        ) { video ->
                                                """
            {
                "id": "${video.id}",
                "title": "${video.title.replace("\"", "\\\"")}",
                "description": "${video.description.take(2048).replace("\"", "\\\"").replace("\n", " ")}...", 
                "channel": "${video.channelTitle.replace("\"", "\\\"")}",
                "duration": "${video.duration ?: "Unknown"}"
            }
            """.trimIndent()
                                        }

                                // 3. Build Prompt (Simplified for Thinking Model, but kept JSON
                                // requirement)
                                val systemPrompt =
                                        """
            You are a video recommendation assistant. 
            
            I will provide a 'Video List' JSON. 
            Your task is to recommend videos from this list that best match the 'User Query'.
            
            Video List (JSON):
            $videoContext

            User Query: $userQuery

            Instructions:
            1. Analyze the query and the video list.
            2. Identify videos that are relevant to the query.
            3. FINALLY, output the result in a strict JSON block wrapped in ```json ... ```.
            
            The JSON structure MUST be:
            {
              "answer": "A friendly conversational response to the user summarizing the recommendations.",
              "suggestedVideos": [
                { "videoId": "THE_VIDEO_ID", "reason": "Why you chose this video" }
              ]
            }
            
            If NO videos match, return an empty array for suggestedVideos.
        """.trimIndent()

                                // 4. Manual JSON Payload Construction
                                val generationConfig =
                                        mutableMapOf<String, Any>(
                                                "responseMimeType" to "text/plain"
                                        )

                                if (deepThinking) {
                                        // experimental feature: likely goes inside generationConfig
                                        val thinkingConfig = mapOf("include_thoughts" to true)
                                        generationConfig["thinking_config"] = thinkingConfig
                                }

                                val requestBody =
                                        mutableMapOf<String, Any>(
                                                "contents" to
                                                        listOf(
                                                                mapOf(
                                                                        "role" to "user",
                                                                        "parts" to
                                                                                listOf(
                                                                                        mapOf(
                                                                                                "text" to
                                                                                                        systemPrompt
                                                                                        )
                                                                                )
                                                                )
                                                        ),
                                                "generationConfig" to generationConfig
                                        )

                                // 5. Call API
                                try {
                                        Log.d("ChatRepository", "Sending prompt via Retrofit...")
                                        var accumulatedText = ""
                                        var accumulatedThinking = ""

                                        // Convert map to JSON String and then to RequestBody
                                        val gson = Gson()
                                        val jsonPayload = gson.toJson(requestBody)

                                        val mediaType =
                                                OkHttpUtils.createMediaType(
                                                        "application/json; charset=utf-8"
                                                )
                                        val requestBodyObj =
                                                OkHttpUtils.createRequestBody(
                                                        mediaType,
                                                        jsonPayload
                                                )

                                        val response =
                                                geminiService
                                                        .streamGenerateContent(
                                                                aiModelName,
                                                                apiKey,
                                                                requestBodyObj
                                                        )
                                                        .execute()

                                        if (!response.isSuccessful) {
                                                val errorBody =
                                                        response.errorBody()?.string()
                                                                ?: "Unknown Error"
                                                Log.e(
                                                        "ChatRepository",
                                                        "API Error Body: $errorBody"
                                                )
                                                throw Exception(
                                                        "API Call Failed: ${response.code()} $errorBody"
                                                )
                                        }

                                        val responseBody =
                                                response.body()
                                                        ?: throw Exception("Empty Response Body")
                                        val reader =
                                                BufferedReader(
                                                        InputStreamReader(responseBody.byteStream())
                                                )
                                        var line: String?

                                        // SSE Parsing
                                        while (reader.readLine().also { line = it } != null) {
                                                val currentLine = line ?: continue
                                                if (!currentLine.startsWith("data: ")) continue

                                                val jsonPart =
                                                        currentLine.removePrefix("data: ").trim()
                                                if (jsonPart == "[DONE]")
                                                        break // End of stream? verify Google uses
                                                // strict SSE

                                                try {
                                                        val jsonObject = JSONObject(jsonPart)
                                                        val candidates =
                                                                jsonObject.optJSONArray(
                                                                        "candidates"
                                                                )
                                                        if (candidates != null &&
                                                                        candidates.length() > 0
                                                        ) {
                                                                val candidate =
                                                                        candidates.getJSONObject(0)
                                                                val content =
                                                                        candidate.optJSONObject(
                                                                                "content"
                                                                        )
                                                                val parts =
                                                                        content?.optJSONArray(
                                                                                "parts"
                                                                        )

                                                                if (parts != null) {
                                                                        for (i in
                                                                                0 until
                                                                                        parts.length()) {
                                                                                val part =
                                                                                        parts.getJSONObject(
                                                                                                i
                                                                                        )

                                                                                // Check for
                                                                                // "thought"
                                                                                // (experimental
                                                                                // field)
                                                                                // Note: REST API
                                                                                // might return
                                                                                // "text" with
                                                                                // "thought:
                                                                                // true" metadata?
                                                                                // Or explicit
                                                                                // "thought" field?
                                                                                // SDK used
                                                                                // `part.thought()`.
                                                                                // Let's check for
                                                                                // `thought` key.
                                                                                if (part.has(
                                                                                                "thought"
                                                                                        ) &&
                                                                                                part.getBoolean(
                                                                                                        "thought"
                                                                                                )
                                                                                ) {
                                                                                        val thoughtText =
                                                                                                part.optString(
                                                                                                        "text",
                                                                                                        ""
                                                                                                )
                                                                                        accumulatedThinking +=
                                                                                                thoughtText
                                                                                        val display =
                                                                                                "> Thinking:\n$accumulatedThinking\n\n$accumulatedText"
                                                                                        emit(
                                                                                                ChatStreamUpdate(
                                                                                                        display,
                                                                                                        false
                                                                                                )
                                                                                        )
                                                                                } else {
                                                                                        // Standard
                                                                                        // text
                                                                                        val text =
                                                                                                part.optString(
                                                                                                        "text",
                                                                                                        ""
                                                                                                )
                                                                                        accumulatedText +=
                                                                                                text

                                                                                        var display =
                                                                                                ""
                                                                                        if (accumulatedThinking
                                                                                                        .isNotEmpty()
                                                                                        ) {
                                                                                                display +=
                                                                                                        "> Thinking:\n$accumulatedThinking\n\n"
                                                                                        }

                                                                                        if (accumulatedText
                                                                                                        .contains(
                                                                                                                "```json"
                                                                                                        )
                                                                                        ) {
                                                                                                display +=
                                                                                                        accumulatedText
                                                                                                                .substringBefore(
                                                                                                                        "```json"
                                                                                                                ) +
                                                                                                                "\n\n[Receiving structured results...]"
                                                                                        } else {
                                                                                                display +=
                                                                                                        accumulatedText
                                                                                        }
                                                                                        emit(
                                                                                                ChatStreamUpdate(
                                                                                                        display,
                                                                                                        false
                                                                                                )
                                                                                        )
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                } catch (e: Exception) {
                                                        // Ignore parse errors for individual chunks
                                                        // (e.g. keep-alive)
                                                        Log.w(
                                                                "ChatRepository",
                                                                "Error parsing SSE chunk: ${e.message}"
                                                        )
                                                }
                                        }

                                        // Post-process: Extract JSON
                                        val (finalAnswer, recs) =
                                                parseResponseWithThinking(accumulatedText)

                                        // Final emit
                                        emit(ChatStreamUpdate(finalAnswer, true, recs))
                                } catch (e: Exception) {
                                        Log.e("ChatRepository", "GenAI Error", e)
                                        emit(
                                                ChatStreamUpdate(
                                                        "Sorry, I encountered an error: ${e.message}",
                                                        true
                                                )
                                        )
                                }
                        }
                        .flowOn(Dispatchers.IO)

        /**
         * Extracts the JSON block from the mixed text/JSON response.
         *
         * The Gemini API often wraps JSON output in markdown code blocks (e.g.
         * ```json
         * ```
         * ...
         * ```)
         * or sometimes returns raw JSON. This method attempts to locate and extract the valid JSON substring.
         *
         * @param fullText The complete accumulated text from the AI response.
         * @return A Pair containing the "Answer" text and the list of [AiRecommendation]s (or empty list if parsing fails).
         * ```
         */
        private fun parseResponseWithThinking(
                fullText: String
        ): Pair<String, List<AiRecommendation>> {
                val jsonStart = fullText.indexOf("```json")
                val jsonEnd = fullText.lastIndexOf("```")

                if (jsonStart != -1 && jsonEnd > jsonStart) {
                        val jsonString = fullText.substring(jsonStart + 7, jsonEnd).trim()
                        return parseJson(jsonString)
                } else {
                        val braceStart = fullText.indexOf("{")
                        val braceEnd = fullText.lastIndexOf("}")
                        if (braceStart != -1 && braceEnd > braceStart) {
                                val jsonString = fullText.substring(braceStart, braceEnd + 1)
                                return parseJson(jsonString)
                        }
                }
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
                        "I found some videos but couldn't parse the details properly." to
                                emptyList()
                }
        }
}
