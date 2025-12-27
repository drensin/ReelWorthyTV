package com.reelworthy.ui

/** Constants used across the Settings UI and Logic. */
object SettingsConstants {
    /**
     * Fallback list of available Gemini models if the API fetch fails. Guaranteed to persist unless
     * the API completely deprecates these IDs.
     */
    val AVAILABLE_MODELS =
            listOf(
                    "gemini-3-flash-preview",
                    "gemini-2.0-pro-exp",
                    "gemini-1.5-flash",
                    "gemini-1.5-pro"
            )
}
