package com.reelworthy.data.models

/**
 * Response object for the 'videos' endpoint.
 * @property items List of [YouTubeVideoItem].
 */
data class YouTubeVideoListResponse(
    val items: List<YouTubeVideoItem>
)

/**
 * Represents a single YouTube video resource.
 * @property id The video ID.
 * @property snippet Basic details (title, description, thumbnails).
 * @property contentDetails content details (duration).
 */
data class YouTubeVideoItem(
    val id: String?, 
    val snippet: Snippet,
    val contentDetails: ContentDetails?
)

/**
 * Basic details about a video or playlist item.
 * @property title The title of the resource.
 * @property description The description.
 * @property thumbnails Map of available thumbnails.
 */
data class Snippet(
    val title: String,
    val description: String,
    val thumbnails: Thumbnails?,
    val channelTitle: String,
    val publishedAt: String
)

/**
 * Container for thumbnails of different resolutions.
 */
data class Thumbnails(
    val default: Thumbnail?,
    val medium: Thumbnail?,
    val high: Thumbnail?,
    val standard: Thumbnail?,
    val maxres: Thumbnail?
)

data class Thumbnail(
    val url: String,
    val width: Int?,
    val height: Int?
)

/**
 * Contains content details like duration.
 * @property duration ISO 8601 duration string.
 */
data class ContentDetails(
    val duration: String?
)

/**
 * Response object for 'playlists' endpoint.
 */
data class YouTubePlaylistListResponse(
    val items: List<YouTubePlaylistItem>
)

data class YouTubePlaylistItem(
    val id: String,
    val snippet: PlaylistSnippet,
    val contentDetails: PlaylistContentDetails?
)

data class PlaylistSnippet(
    val title: String,
    val description: String,
    val thumbnails: Thumbnails?
)

data class PlaylistContentDetails(
    val itemCount: Int
)

/**
 * Response for Gemini API 'models' endpoint.
 */
data class GeminiModelListResponse(
    val models: List<GeminiModel>
)

data class GeminiModel(
    val name: String,
    val displayName: String,
    val description: String,
    val inputTokenLimit: Int,
    val outputTokenLimit: Int,
    val supportedGenerationMethods: List<String>
)

/**
 * Response for 'playlistItems' endpoint.
 * @property items List of [PlaylistItemResult].
 * @property nextPageToken Token for next page of results.
 */
data class YouTubePlaylistItemListResponse(
    val items: List<PlaylistItemResult>,
    val nextPageToken: String?
)

data class PlaylistItemResult(
    val id: String,
    val snippet: PlaylistItemSnippet,
    val contentDetails: PlaylistItemContentDetails?
)

data class PlaylistItemSnippet(
    val title: String,
    val description: String,
    val thumbnails: Thumbnails?,
    val channelTitle: String,
    val publishedAt: String,
    val resourceId: ResourceId
)

/**
 * Identifies the resource being played (the video).
 * @property videoId The ID of the video.
 */
data class ResourceId(
    val videoId: String
)

data class PlaylistItemContentDetails(
    val videoPublishedAt: String?
)
