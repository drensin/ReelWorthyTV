package com.reelworthy.data

import com.reelworthy.data.models.YouTubeVideoListResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface definition for the YouTube Data API v3.
 *
 * Defines the endpoints used to fetch video and playlist information. Uses Retrofit annotations
 * mapping HTTP verbs (GET) to suspend functions.
 */
interface YouTubeApiService {
    /**
     * Fetches details for a specific list of videos. This is commonly used to "enrich" video items
     * with details not available in playlist feeds (e.g., duration).
     *
     * @param part The `part` parameter specifies a comma-separated list of one or more video
     * resource properties that the API response will include. Defaults to "snippet,contentDetails".
     * @param id A comma-separated list of the YouTube video IDs to retrieve.
     * @param apiKey The API key for authentication.
     * @return A [YouTubeVideoListResponse] containing the video details.
     */
    @GET("videos")
    suspend fun getVideos(
            @Query("part") part: String = "snippet,contentDetails",
            @Query("id") id: String,
            @Query("key") apiKey: String
    ): YouTubeVideoListResponse

    /**
     * Fetches items (videos) from a specific playlist.
     *
     * @param authHeader The OAuth 2.0 Access Token ("Bearer [token]"). Required for private
     * playlists.
     * @param part The `part` parameter. Defaults to "snippet,contentDetails".
     * @param playlistId The ID of the playlist to fetch items from.
     * @param maxResults The maximum number of items that should be returned (max 50).
     * @param pageToken The token to retrieve a specific page of results.
     * @param apiKey The API key for authentication.
     * @return A [YouTubePlaylistItemListResponse] containing the list of videos.
     */
    @GET("playlistItems")
    suspend fun getPlaylistItems(
            @retrofit2.http.Header("Authorization") authHeader: String? = null,
            @Query("part") part: String = "snippet,contentDetails",
            @Query("playlistId") playlistId: String,
            @Query("maxResults") maxResults: Int = 50,
            @Query("pageToken") pageToken: String? = null,
            @Query("key") apiKey: String? = null
    ): com.reelworthy.data.models.YouTubePlaylistItemListResponse

    /**
     * Fetches the playlists owned by the authenticated user.
     *
     * @param authHeader The OAuth 2.0 Access Token ("Bearer [token]").
     * @param part The `part` parameter.
     * @param mine Set to `true` to retrieve the authenticated user's playlists.
     * @param maxResults The maximum results per page.
     * @param apiKey The API key.
     * @return A [YouTubePlaylistListResponse] containing the user's playlists.
     */
    @GET("playlists")
    suspend fun getMyPlaylists(
            @retrofit2.http.Header("Authorization") authHeader: String,
            @Query("part") part: String = "snippet,contentDetails",
            @Query("mine") mine: Boolean = true,
            @Query("maxResults") maxResults: Int = 50,
            @Query("key") apiKey: String? = null
    ): com.reelworthy.data.models.YouTubePlaylistListResponse

    /**
     * Fetches the user's subscriptions.
     *
     * @param authHeader The OAuth 2.0 Access Token.
     * @param part The `part` parameter. Defaults to "snippet".
     * @param mine Set to `true` to fetch the authenticated user's subscriptions.
     * @param maxResults The maximum results per page.
     * @param pageToken Token for pagination.
     * @param apiKey The API key.
     * @return A [YouTubeSubscriptionListResponse] containing the list of subscriptions.
     */
    @GET("subscriptions")
    suspend fun getSubscriptions(
            @retrofit2.http.Header("Authorization") authHeader: String,
            @Query("part") part: String = "snippet",
            @Query("mine") mine: Boolean = true,
            @Query("maxResults") maxResults: Int = 50,
            @Query("pageToken") pageToken: String? = null,
            @Query("key") apiKey: String? = null
    ): com.reelworthy.data.models.YouTubeSubscriptionListResponse

    /**
     * Fetches details for a specific channel.
     *
     * Crucially used to resolve the "Uploads" playlist ID (contentDetails.relatedPlaylists.uploads)
     * for a given channel ID.
     *
     * @param part The `part` parameter. Defaults to "contentDetails".
     * @param id The Channel ID.
     * @param apiKey The API key.
     * @return A [YouTubeChannelListResponse] containing channel details.
     */
    @GET("channels")
    suspend fun getChannels(
            @Query("part") part: String = "contentDetails",
            @Query("id") id: String,
            @Query("key") apiKey: String
    ): com.reelworthy.data.models.YouTubeChannelListResponse
}
