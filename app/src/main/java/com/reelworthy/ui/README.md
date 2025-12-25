# UI Layer (`com.reelworthy.ui`)

This package contains all components related to the User Interface, built primarily with **Jetpack Compose for TV**.

## Screens
*   **`DashboardScreen`**: The main application screen. Displays the "Concierge" interface, chat input, and recommended video carousel.
*   **`SettingsScreen`**: A dialog-based settings interface for configuring AI models and playlists.

## ViewModels
*   **`ChatViewModel`**: Manages the state of the chat conversation, streaming AI responses, and holding recommended videos.
*   **`SettingsViewModel`**: Manages configuration state (API keys, playlist selections) and exposes them to the UI.

## Components
*   **`FocusableComponents.kt`**: Contains reusable TV-specific UI elements like `FocusableIcon`, `FocusableChip`, and `FocusableScaleWrapper` (used for the "grow on focus" effect).

## Activities
*   **`MainActivity`**: The single entry point for the application. Sets up the navigation graph (or root composable) and initializes basic dependencies.
