package com.reelworthy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [SearchHistoryEntity].
 */
@Dao
interface SearchHistoryDao {

    /**
     * Inserts a new search query.
     * If the query already exists, it is replaced (effectively updating the timestamp).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistoryEntity)

    /**
     * Retrieves the top 100 most recent searches.
     */
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 100")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    /**
     * Deletes a specific search query.
     */
    @Query("DELETE FROM search_history WHERE `query` = :query")
    suspend fun deleteSearch(query: String)

    /**
     * Prunes the history to keep only the top 100 most recent items.
     */
    @Query("DELETE FROM search_history WHERE `query` NOT IN (SELECT `query` FROM search_history ORDER BY timestamp DESC LIMIT 100)")
    suspend fun pruneHistory()
}
