# Data Layer (`com.reelworthy.data`)

This package contains the data management logic for the application, following the Recommended Android App Architecture.

## Repositories
Repositories are the source of truth for data.
*   **`VideoRepository`**: Manages video data. Handles fetching from YouTube API (Playlists & Subscriptions), ensuring parity with legacy web logic (e.g., Short filtering), and caching in `VideoDao`.
*   **`ChatRepository`**: Manages interactions with the Generative AI (Gemini) API.
*   **`SettingsRepository`**: Manages user preferences (API Keys, selected playlists, Subscription Feed toggle) using `DataStore`.
*   **`AuthRepository`**: Wraps Firebase Authentication and Google Sign-In logic.

## Local Database (Room)
*   **`AppDatabase`**: The main database holder.
*   **`VideoDao`**: Data Access Object for video operations.
*   **`VideoEntity`**: The table schema for storing videos.
*   **`PlaylistEntity`**: The table schema for storing playlists (metadata).

## Network (Retrofit)
*   **`RetrofitClient`**: Singleton that provides API service instances.
*   **`YouTubeApiService`**: Interface for YouTube Data API v3 endpoints.
*   **`GeminiApiService`**: Interface for Gemini/Generative Language API.

## Models
*   **`models/`**: Contains Data Transfer Objects (DTOs) for parsing JSON responses.
