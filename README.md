# ReelWorthy TV: The AI Concierge for Your Watch Later List

> **Welcome to the ReelWorthy TV codebase.**
> This document is designed to be a comprehensive "Manual of Operations" for developers joining this project. Whether you are an experienced Android engineer or writing your first Kotlin app, this guide explains the *Why*, *Data*, *Design*, and *Implementation* of ReelWorthy from first principles.

---

## 1. The Problem: "The Watch Later Graveyard"

We all do it. We scroll through YouTube on our phones, spot an interesting 45-minute video essay or a coding tutorial, and tap "Watch Later". We promise ourselves we'll watch it when we have time, when we are on the couch, "leaning back" on the big screen.

But when we actually sit down in front of the TV, we are tired. Opening the "Watch Later" playlist reveals a chaotic, uncurated list of 500 videos added over the last 3 years. We scroll for 2 minutes, get overwhelmed by decision paralysis, and turn off the TV.

**The "Watch Later" playlist is where good intentions go to die.**

### The Solution: An AI Concierge
ReelWorthy TV solves this by acting as an intelligent filter—a "concierge"—between you and your massive backlog.
It curates content from your **Watch Later** list and your **Recent Subscription Uploads** to find the perfect video.
Instead of showing you a list, it asks you a simple question:

> *"What would you like to watch today?"*

You reply naturally: *"I'm in the mood for something funny but short,"* or *"Show me that coding tutorial about architecture I saved last week."*

The app then:
1.  **Reads** your playlist metadata (titles, descriptions, channel names).
2.  **Thinks** about your request using a Large Language Model (Google Gemini).
3.  **Curates** a small, bespoke selection of 3-5 videos that match your exact mood.
4.  **Explains** *why* it picked them.

It turns a database query into a conversation.

---

## 2. Technology Stack (First Principles)

We chose specific technologies to prioritize **developer velocity**, **maintainability**, and **user experience** on TV hardware.

### A. Language: Kotlin
*   **What it is**: The modern standard language for Android development.
*   **Why we chose it**: It is concise, expressive, and "null-safe" (it prevents the most common crash in history: the `NullPointerException`). It runs on the Java Virtual Machine (JVM) but fixes many of Java's verbosity issues.

### B. UI Framework: Jetpack Compose for TV
*   **What it is**: A declarative UI toolkit.
*   **Old Way (XML)**: You would define a layout in an XML file and then write Java/Kotlin code to find those views (`findViewById`) and mutate them manually (e.g., `textView.setText("Hello")`). This was error-prone and verbose.
*   **New Way (Compose)**: You write Kotlin functions annotated with `@Composable`. You describe *what* the UI should look like for a given state, and Compose handles the rendering.
    *   *Example*: `if (isLoading) ShowSpinner() else ShowContent()`.
*   **For TV**: We use `androidx.tv.material3`, a specialized library that handles TV-specific interactions like **D-Pad Focus** (highlighting items when you navigate with a remote control) automatically.

### C. Architecture: MVVM (Model-View-ViewModel)
We don't dump all code into one file. We separate concerns:
1.  **Model (Data Layer)**: "How do we get data?" (e.g., Databases, Network APIs). It doesn't know about the UI.
2.  **View (UI Layer)**: "How do we show data?" (e.g., Composable functions). It observes the ViewModel.
3.  **ViewModel**: The brain in the middle. It fetches data from the Model, processes it, and exposes "State" to the View.

### D. Asynchrony: Coroutines & Flow
*   **The Challenge**: Network requests and database reads are "slow" (milliseconds to seconds). If you run them on the "Main Thread" (which draws the UI), the app freezes (ANR - Application Not Responding).
*   **The Solution**:
    *   **Coroutines**: Lightweight threads that allow us to write asynchronous code that *looks* synchronous.
    *   **Flow**: A stream of data that emits multiple values over time. (e.g., A stream of "Loading" -> "Success" -> "Data Update").

### E. Data Persistence: Room Database
*   **What it is**: An abstraction layer over SQLite.
*   **Why**: We cache video metadata locally. This makes the app work offline (to browse) and, critically, allows us to feed thousands of video titles to the AI without hitting the YouTube API quota every single time.

### F. Networking: Retrofit
*   **What it is**: A type-safe HTTP client for Android.
*   **Why**: It turns an HTTP API (like `GET /videos?id=123`) into a simple Kotlin interface function: `suspend fun getVideo(id: String): Video`.

### G. AI: Google Gemini API (via Retrofit)
*   **What it is**: A multimodal generative AI model.
*   **Integration**: We use a direct **Retrofit** integration (REST API) instead of the Java SDK. This ensures full compatibility with Android TV (avoiding Apache HttpClient conflicts) and allows for fine-grained control over streaming responses and "Thinking" model configurations.
*   **Context Window**: We use Gemini because it has a huge context window, allowing us to pass *your entire playlist* in a single prompt.

---

## 3. System Architecture & Design

### The "Concierge" Flow
The user experience is designed to be minimal and cinematic.

1.  **Ingestion (Sync)**:
    *   Background Workers (`SyncWorker`) periodically talk to the YouTube Data API.
    *   They fetch your selected playlists and (optionally) your **Subscription Feed** (top 100 recent videos).
    *   They perform **Garbage Collection** to remove videos you've blocked, watched, or deselected, keeping the database lean.
    *   They store `VideoEntity` objects in the local `AppDatabase`.
    
2.  **The Prompt**:
    *   User types or speaks: *"Something about space."*
    *   `ChatRepository` pulls *all* video metadata from `AppDatabase`.
    *   It constructs a massive prompt: *"Here is a list of 500 videos: [JSON Data]. The user wants 'Something about space'. Return a JSON list of all the relevant videos and explain why."*

3.  **Streaming Response**:
    *   LLMs can be slow to generate full answers.
    *   We use **Streaming**: As Gemini generates the text, we update the UI *token by token*.
    *   The user sees a "Thinking..." overlay that types out the AI's thoughts in real-time. This makes the wait feel shorter and cooler (like a sci-fi computer interface).

4.  **Hydration**:
    *   The AI returns a list of Video IDs (e.g., `["abc12345", "xyz98765"]`).
    *   We "hydrate" these IDs by looking them up in our `AppDatabase` to get the full thumbnails, descriptions, and titles to display in the UI.

### Navigation Logic (Hybrid Model)
ReelWorthy TV is designed for a hybrid future (TVs and Foldables).
*   **Hybrid Input**: We support both **5-way D-Pad** (TV Remotes) and **Touch** (Foldables/Tablets) seamlessly.
*   **Unified Focus**: Interactions are standardized. A "Click" on a remote and a "Tap" on a screen trigger the same `onClick`. A "Hold Center" on a remote and a "Long Press" on a screen trigger the same `onLongClick`.
*   **No Back Stack Complexity**: We use a simple overlay system. The `DashboardScreen` is the base. `Settings` is a dialog on top. `Thinking` is an overlay on top.

---

## 4. Implementation Walkthrough (The Map)

If you are new to the codebase, here is the tour of the packages in `app/src/main/java/com/reelworthy/`:

### A. `com.reelworthy.data` (The Foundation)
This package handles all data operations. It is the "Truth".

*   **`AppDatabase.kt`**: The database setup.
*   **`VideoDao.kt`**: "Data Access Object". Contains SQL queries like `SELECT * FROM videos WHERE id = :id`.
*   **`VideoEntity.kt`**: Defines the table structure (columns) for videos.
*   **`RetrofitClient.kt`**: Configures the connection to YouTube and Gemini.
*   **`ChatRepository.kt`**: The logic for talking to Gemini using a dedicated Retrofit service (`GeminiService`). It manages manual JSON payload construction, executes the streaming API call, and parses the SSE (Server-Sent Events) response to extract "thinking" and "text" chunks.
*   **`VideoRepository.kt`**: Hides the complexity of data sources. Handles logic parity with the web app (e.g. fetching subscriptions, sorting, filtering Shorts).
*   **`SearchHistoryRepository.kt`**: Manages the user's search history.
    *   **`SearchHistoryEntity.kt`**: Defines the table for storing queries (timestamped).
    *   **`SearchHistoryDao.kt`**: Handles FIFO logic (deleting oldest when > 50) and looking up recent queries.

### B. `com.reelworthy.ui` (The Paint)
This package handles what the user sees.

*   **`MainActivity.kt`**: The entry point. It sets up the window and initializes the main view.
*   **`DashboardScreen.kt`**: The main screen. Contains:
    *   The background gradient.
    *   The recommendation carousel (`LazyRow`).
    *   The "Thinking" modal overlay.
*   **`ChatViewModel.kt`**: Holds the state for the Dashboard.
    *   `messages`: A list of chat bubbles.
    *   `isLoading`: Are we waiting for AI?
    *   `currentStreamingText`: The raw text coming from Gemini right now.
*   **`SettingsScreen.kt`**: The configuration panel to select playlists, toggle "Recent from Subscriptions", or change AI models.
*   **`FocusableComponents.kt`**: **CRITICAL**. This file contains the "Hybrid Primitives" (e.g., `FocusableScaleWrapper`). These wrappers handle the logic to translate D-Pad events and Touch events into a unified interaction model, ensuring buttons work on both Remotes and Touchscreens.

### C. `com.reelworthy.workers` (The Backend)
*   **`SyncWorker.kt`**: A background task that runs every 6 hours (or on app launch). It silently updates the database so the user never sees a loading spinner for "Ingesting data".

### D. `com.reelworthy.util` (The Helpers)
*   **`TimeUtils.kt`**: formats "PT1H4M" (ISO 8601) into "1:04:00".

---

## 5. Getting Started Guide

### Prerequisites
1.  **Android Studio Ladybug** (or newer).
2.  **Video API Key**: A Google Cloud API Key with access to **YouTube Data API v3** and **Google Gemini API**.

### Setup
1.  Clone the repository.
2.  **Add API Key**: Create or open the `local.properties` file in the project root (this file is ignored by Git).
3.  Add the following line:
    ```properties
    YOUTUBE_API_KEY=your_api_key_here
    ```
4.  Sync Gradle project. The build system will automatically inject this key into `BuildConfig.YOUTUBE_API_KEY` for use in the app.

### Running the App
1.  Create an **Android TV Emulator** (e.g., 1080p Android 11+).
2.  Press **Run**.
3.  **Sign In**: The app requires a Google Account to read your private "Watch Later" plyalist.
    *   *Note: On emulators, you may need to sign into the emulator's Google Play Services dummy account first.*

### Debugging Tips
*   **Logcat**: Filter by "ReelWorthy" to see app logs.
*   **Streaming Logs**: Filter by "ChatViewModel" to see the raw text streaming from the AI before it appears on screen.
*   **Database Inspection**: Use Android Studio's **App Inspection** tab to view the live `AppDatabase` tables and run SQL queries to verify your data is caching correctly.

---

> "The best code is the code you don't have to write. The second best is code that is easy to read."
> — *ReelWorthy Philosophy*
