package com.example.immichswipe.core

data class SessionConfig(
    val baseUrl: String,
    val apiKey: String
)

/**
 * Définit comment l'application doit gérer l'Audio Focus (le son par rapport aux autres apps).
 */
enum class PlaybackBehavior {
    PAUSE_OTHERS, // Coupe les autres sons (Musique)
    IGNORE        // Joue par dessus sans rien changer
}

/**
 * Position des icônes d'action sur l'écran.
 */
enum class IconPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}
