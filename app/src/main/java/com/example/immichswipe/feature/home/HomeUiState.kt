package com.example.immichswipe.feature.home

import com.example.immichswipe.domain.model.Album
import com.example.immichswipe.domain.model.User
import com.example.immichswipe.core.PlaybackBehavior
import com.example.immichswipe.core.AppTheme
import com.example.immichswipe.core.ConnectionStatus

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
    val previousTab: HomeTab = HomeTab.HOME,
    // Map pour stocker le nombre de photos triées par albumId
    val albumTreatedCounts: Map<String, Int> = emptyMap(),
    // Map pour stocker le nombre de suppressions en attente par albumId
    val albumPendingDeletes: Map<String, Int> = emptyMap(),
    val isGridView: Boolean = false, // Toggle entre liste et grille
    val searchQuery: String = "", // Texte de recherche pour filtrer les albums
    val connectionStatus: ConnectionStatus = ConnectionStatus()
) {
    /**
     * Retourne la liste des albums filtrée par le texte de recherche.
     */
    val filteredAlbums: List<Album>
        get() = if (searchQuery.isBlank()) {
            albums
        } else {
            albums.filter { it.albumName.contains(searchQuery, ignoreCase = true) }
        }

    /**
     * Groupe les albums filtrés par état d'avancement.
     */
    val groupedAlbums: Map<AlbumStatus, List<Album>>
        get() {
            val filtered = filteredAlbums
            return filtered.groupBy { album ->
                val treated = albumTreatedCounts[album.id] ?: 0
                when {
                    treated == 0 -> AlbumStatus.NOT_STARTED
                    treated >= album.assetCount -> AlbumStatus.COMPLETED
                    else -> AlbumStatus.IN_PROGRESS
                }
            }
        }
}

/**
 * Représente l'état d'avancement d'un album pour le tri.
 */
enum class AlbumStatus(val label: String) {
    IN_PROGRESS("En cours"),
    NOT_STARTED("Pas commencé"),
    COMPLETED("Terminés")
}
