package com.reelworthy.data

import com.reelworthy.data.models.GeminiModelListResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface definition for the Gemini (Generative Language) API.
 */
interface GeminiApiService {
    /**
     * Lists available AI models.
     *
     * @param apiKey The API key for authentication.
     * @return A [GeminiModelListResponse] containing a list of available models.
     */
    @GET("v1beta/models")
    suspend fun getModels(
        @Query("key") apiKey: String
    ): GeminiModelListResponse
}
