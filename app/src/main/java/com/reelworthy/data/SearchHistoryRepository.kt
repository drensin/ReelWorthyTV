package com.reelworthy.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing search history data.
 *
 * @property searchHistoryDao The data access object for search history.
 */
class SearchHistoryRepository(private val searchHistoryDao: SearchHistoryDao) {

    /**
     * A flow of the top 100 recent searches, ordered by newest first.
     */
    val recentSearches: Flow<List<SearchHistoryEntity>> = searchHistoryDao.getRecentSearches()

    /**
     * Adds a search query to the history.
     * Automatically prunes old entries to keep the list size at 100.
     *
     * @param query The search text to add.
     */
    suspend fun addSearch(query: String) {
        val entry = SearchHistoryEntity(query = query)
        searchHistoryDao.insertSearch(entry)
        searchHistoryDao.pruneHistory()
    }

    /**
     * Deletes a specific search query from history.
     *
     * @param query The search text to delete.
     */
    suspend fun deleteSearch(query: String) {
        searchHistoryDao.deleteSearch(query)
    }
}
