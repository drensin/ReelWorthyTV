package com.reelworthy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a saved search query in the history.
 *
 * @property query The search text. Serves as the primary key to prevent duplicates.
 * @property timestamp The time the search was performed (milliseconds). used for sorting.
 */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
