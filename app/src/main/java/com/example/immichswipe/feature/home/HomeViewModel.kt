package com.example.immichswipe.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.immichswipe.data.repository.UserRepository
import com.example.immichswipe.data.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.immichswipe.core.SessionManager
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
     * Déconnecte l'utilisateur en vidant le stockage local.
     */
    fun logout() {
        viewModelScope.launch {
            sessionRepository.clearSession()
        }
    }
}
