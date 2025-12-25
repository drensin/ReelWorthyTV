# ReelWorthy TV - Source Root

This is the root package for the ReelWorthy Android TV application.

## Sub-packages

*   **`ui/`**: Contains all User Interface components, including Jetpack Compose screens (`DashboardScreen`, `SettingsScreen`), `ViewModels`, and `Activities`.
*   **`data/`**: The Data Layer. Contains Room Database entities, Retrofit API clients, and Repositories that mediate data access.
*   **`workers/`**: Contains Android `WorkManager` jobs for background processing (e.g., `SyncWorker`).
*   **`util/`**: General utility classes (e.g., `TimeUtils`).

## Key Components
*   **`ReelWorthyApp`**: (Conceptual) Note that this app follows a standard Android architecture using MVVM (Model-View-ViewModel) and Repository patterns.
