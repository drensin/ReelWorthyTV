package com.reelworthy.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton holder for Retrofit API clients.
 *
 * This object manages the creation and storage of API service instances for
 * different backends (YouTube and Gemini). It uses Kotlin's `lazy` delegate
 * to ensure services are only initialized when first accessed.
 */
object RetrofitClient {
    private const val BASE_URL = "https://www.googleapis.com/youtube/v3/"

    /**
     * Lazily initialized instance of the [YouTubeApiService].
     * Configured with the base URL: `https://www.googleapis.com/youtube/v3/`.
     */
    val youtubeApi: YouTubeApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YouTubeApiService::class.java)
    }

    /**
     * Lazily initialized instance of the [GeminiApiService].
     * Configured with the base URL: `https://generativelanguage.googleapis.com/`.
     */
    val geminiApi: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }
}
