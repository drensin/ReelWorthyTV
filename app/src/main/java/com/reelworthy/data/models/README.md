# Data Models (`com.reelworthy.data.models`)

This package contains **Data Transfer Objects (DTOs)** used for parsing network responses from APIs.

These classes are typically used by Retrofit and Gson to map JSON responses to Kotlin data classes. They are distinct from the *Entities* in the parent package, which are optimized for local database storage.

## Key Files
*   **`YouTubeModels.kt`**: Contains classes representing the structure of YouTube Data API responses (e.g., `PlaylistItemListResponse`, `VideoListResponse`, `Snippet`, `ContentDetails`).
