package com.reelworthy.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.reelworthy.data.AppDatabase
import com.reelworthy.data.AuthRepository
import com.reelworthy.data.SettingsRepository
import com.reelworthy.data.VideoRepository
import kotlinx.coroutines.flow.first

/**
 * Background worker for periodic synchronization of YouTube data.
 *
 * Designed to run periodically (e.g., every 6 hours) or can be triggered once on app launch.
 * It performs a "Deep Sync" of all selected playlists, ensuring local database validity
 * even if the app hasn't been opened in a while.
 *
 * @param appContext Application context.
 * @param workerParams Worker parameters.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // Securely retrieve API Key from BuildConfig
    private val apiKey = com.reelworthy.BuildConfig.YOUTUBE_API_KEY

    /**
     * Performs the sync work.
     *
     * Steps:
     * 1. Check for a valid Access Token (requires user to remain signed in).
     * 2. Retrieve the list of playlists the user has selected in Settings.
     * 3. Iterate through each playlist and call [VideoRepository.fetchPlaylistVideos] to update the DB.
     *
     * @return [Result.success] if sync completes (even if partial), [Result.failure] if auth fails.
     */
    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Starting background sync...")

        return try {
            val context = applicationContext
            
            // Initialize dependencies
            val database = AppDatabase.getDatabase(context)
            val settingsRepo = SettingsRepository(context)
            val authRepo = AuthRepository()
            val videoRepo = VideoRepository(database.videoDao())
            
            // 1. Get Access Token
            // This might block and refresh tokens if needed
            val accessToken = authRepo.getAccessToken(context)
            if (accessToken == null) {
                Log.w("SyncWorker", "No Access Token available (user not signed in?). Aborting.")
                return Result.failure()
            }
            
            // 2. Get Selected Playlists
            val userSettings = settingsRepo.settingsFlow.first()
            val selectedPlaylistIds = userSettings.selectedPlaylistIds
            
            if (selectedPlaylistIds.isEmpty()) {
                Log.d("SyncWorker", "No playlists selected for sync.")
                return Result.success()
            }

            // 3. Sync each playlist
            var successCount = 0
            selectedPlaylistIds.forEach { playlistId ->
                try {
                    Log.d("SyncWorker", "Syncing playlist: $playlistId")
                    videoRepo.fetchPlaylistVideos(
                        playlistId = playlistId,
                        accessToken = accessToken,
                        apiKey = apiKey
                    )
                    successCount++
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Failed to sync playlist $playlistId", e)
                }
            }

            Log.d("SyncWorker", "Sync complete. Successfully synced $successCount/${selectedPlaylistIds.size} playlists.")
            Result.success()
            
        } catch (e: Exception) {
            Log.e("SyncWorker", "Fatal error during sync", e)
            Result.retry()
        }
    }
}
