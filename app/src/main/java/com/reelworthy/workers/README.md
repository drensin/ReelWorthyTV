# Workers (`com.reelworthy.workers`)

This package contains background jobs managed by Android's **WorkManager** library.

## Components
*   **`SyncWorker`**: A `CoroutineWorker` responsible for synchronizing YouTube playlists in the background.
    *   **Triggers**: Runs periodically (every ~6 hours) and immediately upon app launch.
    *   **Task**: Fetches updated videos for all selected playlists and (optionally) the top 100 recent videos from subscribed channels.
    *   **Cleanup**: Performs "Garbage Collection" after a successful sync to remove local videos that are no longer present in the source feeds.
    *   **Requirements**: Requires an active network connection.
