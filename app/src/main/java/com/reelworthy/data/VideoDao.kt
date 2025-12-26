package com.reelworthy.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for accessing video and playlist data in the local database.
 */
@Dao
interface VideoDao {
    /**
     * Retrieves all videos sorted by their added date (newest first) as a Flow.
     * Use this for observing real-time updates in the UI.
     *
     * @return A Flow emitting the list of [VideoEntity].
     */
    @Query("SELECT * FROM videos ORDER BY addedDate DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    /**
     * Retrieves all videos as a one-shot List.
     * Use this for synchronous processing (e.g., generating AI search context).
     *
     * @return A List of [VideoEntity].
     */
    @Query("SELECT * FROM videos ORDER BY addedDate DESC")
    suspend fun getAllVideosList(): List<VideoEntity>

    /**
     * Inserts or updates a list of videos.
     * If a video with the same ID exists, it will be replaced.
     *
     * @param videos The list of [VideoEntity] across which to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    /**
     * Retrieves all locally stored playlists as a Flow.
     *
     * @return A Flow emitting the list of [PlaylistEntity].
     */
    @Query("SELECT * FROM playlists")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>
    
    /**
     * Inserts or updates a list of playlists.
     *
     * @param playlists The list of [PlaylistEntity] to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<PlaylistEntity>)

    /**
     * Deletes all videos whose IDs are NOT in the provided list.
     * Use this for garbage collection after a full sync.
     *
     * @param ids The list of valid video IDs to KEEP.
     */
    @Query("DELETE FROM videos WHERE id NOT IN (:ids)")
    suspend fun deleteVideosNotIn(ids: List<String>)
}
