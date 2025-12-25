# Workers (`com.reelworthy.workers`)

This package contains background jobs managed by Android's **WorkManager** library.

## Components
*   **`SyncWorker`**: A `CoroutineWorker` responsible for synchronizing YouTube playlists in the background.
    *   **Triggers**: Runs periodically (every ~6 hours) and immediately upon app launch.
    *   **Task**: Fetches updated videos for all selected playlists, retrieves their full details (including duration) via a second API pass, and updates the local database.
    *   **Requirements**: Requires an active network connection.
