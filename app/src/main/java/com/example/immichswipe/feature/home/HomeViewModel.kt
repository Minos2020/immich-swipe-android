package com.example.immichswipe.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.immichswipe.data.repository.UserRepository
import com.example.immichswipe.data.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.immichswipe.core.SessionManager
import com.example.immichswipe.core.PlaybackBehavior
import com.example.immichswipe.data.repository.SessionRepository
import com.example.immichswipe.domain.model.Album

/**
 * ViewModel de l'écran d'accueil.
 * Il orchestre la récupération des infos utilisateur et des albums.
 */
class HomeViewModel(
    private val sessionRepository: SessionRepository,
    private val albumRepository: AlbumRepository
) : ViewModel() {
    
    private val userRepository by lazy { 
        UserRepository(
            SessionManager.api ?: throw IllegalStateException("Session not initialized")
        )
    }
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Observe le comportement de lecture en temps réel
        viewModelScope.launch {
            sessionRepository.playbackBehavior.collect { behavior ->
                _uiState.value = _uiState.value.copy(playbackBehavior = behavior)
            }
        }
    }

    /**
     * Charge toutes les données nécessaires à l'écran d'accueil.
     * (l'utilisateur et la liste des albums)
     */
    fun loadUser() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // On récupère les deux informations
                val user = userRepository.getCurrentUser()
                val albums = albumRepository.getAlbums()
                
                _uiState.value = _uiState.value.copy(
                    user = user,
                    albums = albums,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Erreur lors du chargement des données",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Rafraîchit uniquement la liste des albums.
     * Utilisé pour le "Pull to Refresh".
     */
    fun refreshAlbums() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            try {
                // On ajoute un petit délai artificiel (ex: 800ms) pour que l'utilisateur
                // ait bien le temps de voir l'indicateur et sente que l'action est prise en compte
                val fetchJob = launch {
                    val albums = albumRepository.getAlbums()
                    _uiState.value = _uiState.value.copy(albums = albums)
                }
                
                // On s'assure que l'animation dure au moins 800ms même si le réseau est ultra rapide
                delay(800)
                fetchJob.join()
                
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "Erreur lors du rafraîchissement"
                )
            }
        }
    }

    /**
     * Change l'onglet actuel de la barre de navigation.
     */
    fun onTabSelected(tab: HomeTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    /**
     * Sélectionne un album pour commencer une session de tri.
     * Bascule automatiquement sur l'onglet SWIPE.
     */
    fun onAlbumSelected(album: Album) {
        _uiState.value = _uiState.value.copy(
            selectedAlbum = album,
            currentTab = HomeTab.SWIPE
        )
    }

    /**
     * Change la préférence de gestion du son.
     */
    fun setPlaybackBehavior(behavior: PlaybackBehavior) {
        viewModelScope.launch {
            sessionRepository.savePlaybackBehavior(behavior)
        }
    }

    fun getSessionRepository() = sessionRepository

    /**
     * Déconnecte l'utilisateur en vidant le stockage local.
     */
    fun logout() {
        viewModelScope.launch {
            // Réinitialise l'onglet sur HOME pour la prochaine connexion
            _uiState.value = _uiState.value.copy(currentTab = HomeTab.HOME)
            sessionRepository.clearSession()
        }
    }
}
