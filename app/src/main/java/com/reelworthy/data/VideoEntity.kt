package com.reelworthy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a video stored in the local database.
 *
 * This entity captures the essential metadata of a YouTube video required for display
 * and filtering within the application.
 *
 * @property id The unique YouTube Video ID (e.g., "dQw4w9WgXcQ").
 * @property title The title of the video.
 * @property description The description snippet of the video.
 * @property thumbnailUrl The URL of the video's thumbnail image (usually high quality).
 * @property channelTitle The name of the channel that published the video.
 * @property publishedAt The ISO 8601 date string of when the video was published.
 * @property duration The ISO 8601 duration string (e.g., "PT1H10M"). Null if not yet fetched.
 * @property isWatched A flag indicating if the user has watched this video (locally tracked).
 * @property addedDate The timestamp (milliseconds) when this video was added to the database.
 */
@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val channelTitle: String,
    val publishedAt: String,
    val duration: String? = null,
    val isWatched: Boolean = false,
    val addedDate: Long = System.currentTimeMillis()
)
