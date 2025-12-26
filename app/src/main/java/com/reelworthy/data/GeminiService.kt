package com.reelworthy.data

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Retrofit service interface for interacting with the Google Gemini API.
 *
 * Uses direct REST calls to bypass SDK compatibility issues on Android TV.
 */
interface GeminiService {

    /**
     * Streams content generation from the Gemini model.
     *
     * @param model The model name (e.g., "gemini-2.0-flash-exp").
     * @param apiKey The Google Cloud API key.
     * @param requestBody The JSON request payload wrapped in a [RequestBody] to avoid type issues.
     * @param alt Helper parameter to request Server-Sent Events (SSE). Defaults to "sse".
     * @return A [Call] yielding a [ResponseBody] which must be read as a stream.
     */
    @Streaming
    @POST("v1beta/models/{model}:streamGenerateContent")
    fun streamGenerateContent(
            @Path("model") model: String,
            @Header("x-goog-api-key") apiKey: String,
            @Body requestBody: RequestBody,
            @Query("alt") alt: String = "sse" // Server-Sent Events for streaming
    ): Call<ResponseBody>
}
