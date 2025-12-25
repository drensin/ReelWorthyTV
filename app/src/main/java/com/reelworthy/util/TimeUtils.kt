package com.reelworthy.util

import java.time.Duration
import java.util.Locale

/**
 * Utility object for handling time and duration formatting.
 */
object TimeUtils {

    /**
     * Parses an ISO 8601 duration string (e.g., "PT1H2M10S") into a readable string (e.g., "1:02:10").
     *
     * Handles various formats:
     * - Minutes and Seconds (MM:SS)
     * - Hours, Minutes, and Seconds (H:MM:SS)
     *
     * @param isoDuration The ISO 8601 duration string from YouTube API.
     * @return A formatted duration string, or null if parsing fails or input is null.
     */
    fun formatIsoDuration(isoDuration: String?): String? {
        if (isoDuration.isNullOrEmpty()) return null
        
        return try {
            val duration = Duration.parse(isoDuration)
            val totalSeconds = duration.seconds
            
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            
            if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%d:%02d", minutes, seconds)
            }
        } catch (e: Exception) {
            null
        }
    }
}
