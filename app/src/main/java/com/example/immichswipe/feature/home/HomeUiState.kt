package com.example.immichswipe.feature.home

import com.example.immichswipe.domain.model.Album
import com.example.immichswipe.domain.model.User
import com.example.immichswipe.core.PlaybackBehavior
import com.example.immichswipe.core.AppTheme

/**
 * Les différents onglets disponibles dans l'application.
 */
enum class HomeTab {
    HOME, SWIPE, SETTINGS
}

/**
 * État global de l'écran principal (après connexion).
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val user: User? = null,
    val albums: List<Album> = emptyList(),
    val currentTab: HomeTab = HomeTab.HOME,
    val selectedAlbum: Album? = null, // L'album que l'utilisateur a choisi de trier
    val error: String? = null,
    val playbackBehavior: PlaybackBehavior = PlaybackBehavior.PAUSE_OTHERS,
    val showProfilePopup: Boolean = false, // État de visibilité de la fenêtre profil
    val themeMode: AppTheme = AppTheme.SYSTEM,
    val previousTab: HomeTab = HomeTab.HOME
)
