package com.reelworthy.data

import android.util.Log
import com.reelworthy.data.models.YouTubeVideoItem
import kotlinx.coroutines.flow.Flow

/**
 * Manages video data retrieval and persistence.
 *
 * Acts as the single source of truth for video data, mediating between the [VideoDao] (local Room
 * database) and [RetrofitClient] (YouTube API).
 *
 * @property videoDao The Local Data Access Object.
 */
class VideoRepository(private val videoDao: VideoDao) {

    /**
     * Fetches a single video by ID and saves it to the local database. Use this when you only have
     * an ID (e.g., from a shared link) and need full details.
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
     * Fetches items for a playlist. Note: This method appears to be a stub or partial
     * implementation compared to [fetchPlaylistVideos]. Ideally, use [fetchPlaylistVideos] for full
     * synchronization.
     */
    suspend fun fetchAndSavePlaylistItems(playlistId: String, apiKey: String) {
        try {
            val response =
                    RetrofitClient.youtubeApi.getPlaylistItems(
                            playlistId = playlistId,
                            apiKey = apiKey
                    )
            // Note: Playground items response structure is slightly different (resourceId inside
            // snippet),
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
    suspend fun fetchUserPlaylists(accessToken: String) {
        try {
            val authHeader = "Bearer $accessToken"
            val response = RetrofitClient.youtubeApi.getMyPlaylists(authHeader = authHeader)
            if (response.items.isNotEmpty()) {
                val playlists = response.items.map { it.toEntity() }
                videoDao.insertPlaylists(playlists)
                Log.d("VideoRepository", "Inserted ${playlists.size} playlists")
            }
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(
                    "VideoRepository",
                    "HTTP Error fetching playlists: code=${e.code()}, body=$errorBody",
                    e
            )
            throw e
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
    suspend fun fetchPlaylistVideos(
            playlistId: String,
            accessToken: String?,
            apiKey: String
    ): List<String> {
        try {
            val authHeader = if (accessToken != null) "Bearer $accessToken" else null

            var nextPageToken: String? = null

            // Temporary list to hold entities before insertion, so we can enrich them
            val accumulatedVideos = mutableListOf<VideoEntity>()

            // Step 1: Paginate through PlaylistItems to get IDs and basic Snippets
            do {
                val response =
                        RetrofitClient.youtubeApi.getPlaylistItems(
                                authHeader = authHeader,
                                playlistId = playlistId,
                                apiKey = if (authHeader != null) null else apiKey,
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
                        val videoDetailsResponse =
                                RetrofitClient.youtubeApi.getVideos(
                                        authHeader = authHeader,
                                        id = videoIds,
                                        apiKey = if (authHeader != null) null else apiKey
                                )

                        // Map of ID -> Duration
                        val durationsMap =
                                videoDetailsResponse.items.associate {
                                    it.id to it.contentDetails?.duration
                                }

                        // Enrich entities
                        val enrichedChunk =
                                chunk.map { entity ->
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

                Log.d(
                        "VideoRepository",
                        "Inserted ${enrichedVideos.size} videos (with durations) from playlist $playlistId"
                )
            }

            return accumulatedVideos.map { it.id }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error fetching playlist items", e)
            throw e // Rethrow to let ViewModel generic handler (or specific UI handler) know
        }
    }

    /**
     * Deletes all local videos that are NOT in the provided list of IDs.
     *
     * This is a "Garbage Collection" method used after a successful sync to remove videos that have
     * been deselected by the user or removed from the source playlists.
     *
     * @param ids The list of "valid" video IDs that should reside in the database.
     */
    suspend fun deleteLocalVideosNotIn(ids: List<String>) {
        if (ids.isNotEmpty()) {
            videoDao.deleteVideosNotIn(ids)
            Log.d(
                    "VideoRepository",
                    "Garbage collection: Retained ${ids.size} videos. Others deleted."
            )
        } else {
            Log.w(
                    "VideoRepository",
                    "Garbage collection requested with EMPTY list. Skipping to avoid wiping database."
            )
        }
    }

    /**
     * Fetches recent videos from the user's subscriptions to maintain feature parity with the
     * legacy web app.
     *
     * This process involves several steps to respect API quotas while providing a relevant feed:
     * 1. **Fetch Subscriptions**: Retrieves the user's subscribed channels.
     * 2. **Resolve Uploads**: For each channel, identifies the "Uploads" playlist ID.
     * 3. **Fetch Candidates**: Retrieves the top 10 most recent videos from each channel's uploads.
     * 4. **Global Sort**: Aggregates all candidates and sorts them by publication date (newest
     * first).
     * 5. **Selection**: Selects the top 100 global candidates.
     * 6. **Enrichment**: Fetches full details (including duration) for these 100 videos.
     * 7. **Filtering**: Excludes "Shorts" (videos ≤ 60 seconds) to match the TV form factor
     * preference.
     * 8. **Persistence**: Saves the final list to the local database.
     *
     * @param accessToken The OAuth 2.0 Access Token for the authenticated user.
     * @param apiKey The Google Cloud Console API Key for quota-heavy public data fetches.
     * @return A list of video IDs that were successfully synced and persisted.
     */
    suspend fun fetchRecentSubscriptionVideos(accessToken: String, apiKey: String): List<String> {
        try {
            val authHeader = "Bearer $accessToken"

            // 1. Fetch Subscriptions
            val allSubs = mutableListOf<com.reelworthy.data.models.YouTubeSubscriptionItem>()
            var nextToken: String? = null
            do {
                val response =
                        RetrofitClient.youtubeApi.getSubscriptions(
                                authHeader = authHeader,
                                apiKey = if (authHeader != null) null else apiKey,
                                pageToken = nextToken
                        )
                allSubs.addAll(response.items)
                nextToken = response.nextPageToken
            } while (nextToken != null)

            // 2 & 3. Fetch Recent Uploads per Channel
            val allCandidateVideos = mutableListOf<VideoEntity>()

            // Optimization: Run these in parallel chunks if possible, but sequential for now to
            // avoid complexity/rate limits
            for (sub in allSubs) {
                try {
                    val resourceId = sub.snippet.resourceId
                    val channelId = resourceId.channelId ?: continue

                    // Get Uploads Content Details
                    val channelResponse =
                            RetrofitClient.youtubeApi.getChannels(
                                    authHeader = authHeader,
                                    id = channelId,
                                    apiKey = if (authHeader != null) null else apiKey
                            )
                    val uploadsPlaylistId =
                            channelResponse.items.firstOrNull()
                                    ?.contentDetails
                                    ?.relatedPlaylists
                                    ?.uploads
                                    ?: continue

                    // Fetch Top 10 from Uploads
                    val uploadsResponse =
                            RetrofitClient.youtubeApi.getPlaylistItems(
                                    authHeader = authHeader,
                                    apiKey = if (authHeader != null) null else apiKey,
                                    playlistId = uploadsPlaylistId,
                                    maxResults = 10
                            )

                    for (item in uploadsResponse.items) {
                        // Convert to basic entity (without duration yet)
                        val videoId = item.snippet.resourceId.videoId ?: continue
                        val entity =
                                VideoEntity(
                                        id = videoId,
                                        title = item.snippet.title,
                                        description = item.snippet.description,
                                        thumbnailUrl = item.snippet.thumbnails?.high?.url
                                                        ?: item.snippet.thumbnails?.medium?.url
                                                                ?: item.snippet
                                                                .thumbnails
                                                                ?.default
                                                                ?.url
                                                                ?: "",
                                        channelTitle = item.snippet.channelTitle,
                                        publishedAt =
                                                item.snippet.publishedAt, // This is published date
                                        duration = null, // To be filled later
                                        addedDate = System.currentTimeMillis() // Sort marker
                                )
                        allCandidateVideos.add(entity)
                    }
                } catch (e: Exception) {
                    // Ignore individual channel failures
                    Log.w(
                            "VideoRepository",
                            "Failed to fetch uploads for channel ${sub.snippet.title}",
                            e
                    )
                }
            }

            // 4 & 5. Sort & Top 100
            // Note: publishedAt format is standard ISO (e.g. 2023-10-27T10:00:00Z), so string
            // comparison works for sorting DESC
            val top100Candidates =
                    allCandidateVideos.sortedByDescending { it.publishedAt }.take(100)

            if (top100Candidates.isEmpty()) return emptyList()

            // 6. Enrich (Fetch Durations)
            // Batch requests max 50
            val enrichedVideos = mutableListOf<VideoEntity>()

            val chunks = top100Candidates.chunked(50)
            for (batch in chunks) {
                try {
                    val ids = batch.joinToString(",") { it.id }
                    val detailsResponse =
                            RetrofitClient.youtubeApi.getVideos(
                                    authHeader = authHeader,
                                    id = ids,
                                    apiKey = if (authHeader != null) null else apiKey
                            )

                    // Map details back to entities
                    for (candidate in batch) {
                        val details =
                                detailsResponse.items.find { detail -> detail.id == candidate.id }
                        val duration = details?.contentDetails?.duration

                        // 7. Filter Shorts (Duration > 60s)
                        val isShort = isDurationShort(duration)

                        if (!isShort) {
                            enrichedVideos.add(candidate.copy(duration = duration))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("VideoRepository", "Failed to enrich batch", e)
                }
            }

            // 8. Insert into DB
            videoDao.insertVideos(enrichedVideos)
            Log.d(
                    "VideoRepository",
                    "Synced ${enrichedVideos.size} recent videos from subscriptions."
            )

            return enrichedVideos.map { it.id }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error fetching subscription videos", e)
            return emptyList()
        }
    }

    /**
     * Determines if a video is considered a "Short" based on its ISO 8601 duration.
     *
     * Heuristic:
     * - Any duration with Hours (H) is Long.
     * - Any duration with Minutes (M) >= 2 is Long.
     * - Any duration with 1 Minute and > 0 Seconds is Long.
     * - "PT1M" (exactly 60s) and anything less is Short.
     *
     * @param isoDuration The ISO 8601 duration string (e.g., "PT1H", "PT59S").
     * @return True if the video is ≤ 60 seconds, False otherwise.
     */
    private fun isDurationShort(isoDuration: String?): Boolean {
        if (isoDuration == null) return true // Treat unknown as short/invalid
        if (isoDuration == "P0D") return true // Live streams sometimes show weird

        // Simple Heuristic matching Legacy JS:
        // if it has Hours (H), it's long.
        if (isoDuration.contains("H")) return false

        // If it has Minutes (M):
        // PT2M... -> Long
        // PT1M... -> Borderline (60s). Legacy said > 60s.
        // Let's check minutes value.
        // Regex: PT(\d+)M
        val match = java.util.regex.Pattern.compile("PT(\\d+)M").matcher(isoDuration)
        if (match.find()) {
            val minutes = match.group(1)?.toIntOrNull() ?: 0
            if (minutes >= 1) {
                // Check if it's exactly 1M and 0S? Legacy said > 60s.
                // 1 minute is 60s. So > 60s means 1m 1s.
                // If minutes >= 2, definitely long.
                if (minutes >= 2) return false
                // If 1 minute, check seconds.
                // Regex: PT1M(\d+)S
                val secMatch = java.util.regex.Pattern.compile("PT1M(\\d+)S").matcher(isoDuration)
                if (secMatch.find()) {
                    val seconds = secMatch.group(1)?.toIntOrNull() ?: 0
                    if (seconds > 0) return false // 1m 1s+
                }
                // If just "PT1M", that is exactly 60s. 60 is NOT > 60. So Short.
                return true
            }
        }

        // No Hours, No Minutes (or 0 minutes). Must be seconds only -> Short.
        return true
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
                thumbnailUrl = this.snippet.thumbnails?.high?.url
                                ?: this.snippet.thumbnails?.medium?.url ?: "",
                channelTitle = this.snippet.channelTitle,
                publishedAt = this.snippet.publishedAt,
                duration = this.contentDetails?.duration
        )
    }

    private fun com.reelworthy.data.models.PlaylistItemResult.toEntity(): VideoEntity {
        return VideoEntity(
                id = this.snippet.resourceId.videoId ?: "",
                title = this.snippet.title,
                description = this.snippet.description,
                thumbnailUrl = this.snippet.thumbnails?.high?.url
                                ?: this.snippet.thumbnails?.medium?.url ?: "",
                channelTitle = this.snippet.channelTitle,
                publishedAt = this.snippet.publishedAt
        )
    }

    private fun com.reelworthy.data.models.YouTubePlaylistItem.toEntity(): PlaylistEntity {
        return PlaylistEntity(
                id = this.id,
                title = this.snippet.title,
                description = this.snippet.description,
                thumbnailUrl = this.snippet.thumbnails?.high?.url
                                ?: this.snippet.thumbnails?.medium?.url ?: "",
                itemCount = this.contentDetails?.itemCount ?: 0
        )
    }
}
