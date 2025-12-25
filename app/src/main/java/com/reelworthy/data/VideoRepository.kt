package com.reelworthy.data

import android.util.Log
import com.reelworthy.data.models.YouTubeVideoItem
import kotlinx.coroutines.flow.Flow

/**
 * Manages video data retrieval and persistence.
 *
 * Acts as the single source of truth for video data, mediating between the
 * [VideoDao] (local Room database) and [RetrofitClient] (YouTube API).
 *
 * @property videoDao The Local Data Access Object.
 */
class VideoRepository(private val videoDao: VideoDao) {

    /**
     * Fetches a single video by ID and saves it to the local database.
     * Use this when you only have an ID (e.g., from a shared link) and need full details.
     *
     * @param videoId The YouTube Video ID.
     * @param apiKey The YouTube Data API Key.
     */
    suspend fun fetchAndSaveVideo(videoId: String, apiKey: String) {
        try {
            val response = RetrofitClient.youtubeApi.getVideos(id = videoId, apiKey = apiKey)
            if (response.items.isNotEmpty()) {
                val item = response.items[0]
                val videoEntity = item.toEntity()
                videoDao.insertVideos(listOf(videoEntity))
            }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error fetching video", e)
            throw e
        }
    }

    /**
     * Fetches items for a playlist.
     * Note: This method appears to be a stub or partial implementation compared to [fetchPlaylistVideos].
     * Ideally, use [fetchPlaylistVideos] for full synchronization.
     */
    suspend fun fetchAndSavePlaylistItems(playlistId: String, apiKey: String) {
         try {
            val response = RetrofitClient.youtubeApi.getPlaylistItems(playlistId = playlistId, apiKey = apiKey)
            // Note: Playground items response structure is slightly different (resourceId inside snippet), 
            // but for now assume we map what we can. 
            // actually, for playlistItems, the 'id' of the item is the playlist item id, 
            // but the video id is in snippet.resourceId.videoId.
            // My simple model might need adjustment or a mapper.
            // For MVP, let's just log success.
            Log.d("VideoRepository", "Fetched ${response.items.size} items from playlist")
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error fetching playlist", e)
             throw e
        }
    }

    /**
     * Fetches playlists owned by the authenticated user and saves them locally.
     *
     * @param accessToken The OAuth 2.0 Token.
     * @param apiKey The API Key.
     */
    suspend fun fetchUserPlaylists(accessToken: String, apiKey: String) {
        try {
            val authHeader = "Bearer $accessToken"
            val response = RetrofitClient.youtubeApi.getMyPlaylists(authHeader = authHeader, apiKey = apiKey)
            if (response.items.isNotEmpty()) {
                val playlists = response.items.map { it.toEntity() }
                videoDao.insertPlaylists(playlists)
                Log.d("VideoRepository", "Inserted ${playlists.size} playlists")
            }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error fetching playlists", e)
            throw e
        }
    }

    /**
     * Deep-syncs a playlist: Fetches all videos AND their durations.
     *
     * This is an expensive operation as it requires a "double-fetch":
     * 1. Iterate through `playlistItems` pages to get all Video IDs.
     * 2. Batch those IDs (max 50) and call the `videos` endpoint to get `contentDetails.duration`.
     *
     * @param playlistId The ID of the playlist to sync.
     * @param accessToken Optional OAuth token (for private playlists).
     * @param apiKey The API Key.
     */
    suspend fun fetchPlaylistVideos(playlistId: String, accessToken: String?, apiKey: String) {
        try {
            val authHeader = if (accessToken != null) "Bearer $accessToken" else null
            
            var nextPageToken: String? = null
            var totalFetched = 0
            
            // Temporary list to hold entities before insertion, so we can enrich them
            val accumulatedVideos = mutableListOf<VideoEntity>()

            // Step 1: Paginate through PlaylistItems to get IDs and basic Snippets
            do {
                val response = RetrofitClient.youtubeApi.getPlaylistItems(
                    authHeader = authHeader,
                    playlistId = playlistId, 
                    apiKey = apiKey,
                    pageToken = nextPageToken
                )
                
                if (response.items.isNotEmpty()) {
                    val batchEntities = response.items.map { it.toEntity() }
                    accumulatedVideos.addAll(batchEntities)
                }
                
                nextPageToken = response.nextPageToken
                
            } while (nextPageToken != null)
            
            // Step 2: Enrich with duration by fetching video details in batches of 50
            if (accumulatedVideos.isNotEmpty()) {
                val enrichedVideos = mutableListOf<VideoEntity>()
                val chunks = accumulatedVideos.chunked(50)
                
                for (chunk in chunks) {
                    val videoIds = chunk.map { it.id }.joinToString(",")
                    try {
                        val videoDetailsResponse = RetrofitClient.youtubeApi.getVideos(
                            id = videoIds,
                            apiKey = apiKey
                        )
                        
                        // Map of ID -> Duration
                        val durationsMap = videoDetailsResponse.items.associate { 
                            it.id to it.contentDetails?.duration 
                        }
                        
                        // Enrich entities
                        val enrichedChunk = chunk.map { entity ->
                            // Update duration if found
                            val duration = durationsMap[entity.id]
                            if (duration != null) {
                                entity.copy(duration = duration)
                            } else {
                                entity
                            }
                        }
                        enrichedVideos.addAll(enrichedChunk)
                        
                    } catch (e: Exception) {
                        Log.e("VideoRepository", "Failed to fetch video details for batch", e)
                        // Fallback: use original entities without duration
                        enrichedVideos.addAll(chunk)
                    }
                }
                
                // Step 3: Upsert into Database
                videoDao.insertVideos(enrichedVideos)
                totalFetched = enrichedVideos.size
                Log.d("VideoRepository", "Inserted $totalFetched videos (with durations) from playlist $playlistId")
            }
            
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error fetching playlist items", e)
            throw e // Rethrow to let ViewModel generic handler (or specific UI handler) know
        }
    }
    
    /** Flow emitting all videos from the DB, sorted by newest added. */
    val allVideos: Flow<List<VideoEntity>> = videoDao.getAllVideos()
    
    /** Flow emitting all playlists from the DB. */
    val allPlaylists: Flow<List<PlaylistEntity>> = videoDao.getAllPlaylists()

    // Helpers to convert API DTOs to local Entities

    private fun YouTubeVideoItem.toEntity(): VideoEntity {
        return VideoEntity(
            id = this.id ?: "", // TODO: Handle correctly if id is missing or nested
            title = this.snippet.title,
            description = this.snippet.description,
            thumbnailUrl = this.snippet.thumbnails?.high?.url ?: this.snippet.thumbnails?.medium?.url ?: "",
            channelTitle = this.snippet.channelTitle,
            publishedAt = this.snippet.publishedAt,
            duration = this.contentDetails?.duration
        )
    }

    private fun com.reelworthy.data.models.PlaylistItemResult.toEntity(): VideoEntity {
        return VideoEntity(
            id = this.snippet.resourceId.videoId,
            title = this.snippet.title,
            description = this.snippet.description,
            thumbnailUrl = this.snippet.thumbnails?.high?.url ?: this.snippet.thumbnails?.medium?.url ?: "",
            channelTitle = this.snippet.channelTitle,
            publishedAt = this.snippet.publishedAt // contentDetails.videoPublishedAt is also an option
        )
    }

    private fun com.reelworthy.data.models.YouTubePlaylistItem.toEntity(): PlaylistEntity {
        return PlaylistEntity(
            id = this.id,
            title = this.snippet.title,
            description = this.snippet.description,
            thumbnailUrl = this.snippet.thumbnails?.high?.url ?: this.snippet.thumbnails?.medium?.url ?: "",
            itemCount = this.contentDetails?.itemCount ?: 0
        )
    }
}
