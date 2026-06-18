package com.example.immichswipe.feature.home

import kotlinx.coroutines.flow.first
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
import kotlinx.coroutines.flow.update
import com.example.immichswipe.core.PlaybackBehavior
import com.example.immichswipe.core.AppTheme
import com.example.immichswipe.data.repository.SessionRepository
import com.example.immichswipe.data.repository.SwipeDecisionRepository
import com.example.immichswipe.data.repository.AssetRepository
import com.example.immichswipe.domain.model.Album

/**
 * ViewModel de l'écran d'accueil.
 */
class HomeViewModel(
    private val sessionRepository: SessionRepository,
    private val albumRepository: AlbumRepository,
    private val swipeDecisionRepository: SwipeDecisionRepository,
    private val assetRepository: AssetRepository
) : ViewModel() {
    
    private val userRepository by lazy { 
        UserRepository(
            SessionManager.api ?: throw IllegalStateException("Session not initialized")
        )
    }
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Observe les préférences
        viewModelScope.launch {
            sessionRepository.playbackBehavior.collect { behavior ->
                _uiState.update { it.copy(playbackBehavior = behavior) }
            }
        }
        viewModelScope.launch {
            sessionRepository.themeMode.collect { theme ->
                _uiState.update { it.copy(themeMode = theme) }
            }
        }

        // Applique le mode d'affichage par défaut au démarrage
        viewModelScope.launch {
            val isGrid = sessionRepository.defaultLayoutGrid.first()
            _uiState.update { it.copy(isGridView = isGrid) }
        }

        // Observe les décisions locales pour mettre à jour les barres de progression
        viewModelScope.launch {
            swipeDecisionRepository.getAllAlbumDecisionCounts().collect { stats ->
                val treatedMap = stats.associate { it.albumId to it.totalCount }
                val deleteMap = stats.associate { it.albumId to it.deleteCount }
                _uiState.update { 
                    it.copy(
                        albumTreatedCounts = treatedMap,
                        albumPendingDeletes = deleteMap
                    )
                }
            }
        }
    }

    fun loadUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val user = userRepository.getCurrentUser()
                val albums = albumRepository.refreshAlbums()
                _uiState.update { 
                    it.copy(user = user, albums = albums, isLoading = false, error = null)
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Erreur de chargement", isLoading = false)
                }
            }
        }
    }

    fun refreshAlbums() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val albums = albumRepository.refreshAlbums()
                _uiState.update { it.copy(albums = albums, isRefreshing = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun onTabSelected(tab: HomeTab) {
        val current = _uiState.value.currentTab
        
        // SOLUTION : Rafraîchissement systématique si on revient sur HOME
        if (tab == HomeTab.HOME && current != HomeTab.HOME) {
            refreshAlbums()
        }

        val nextPrevious = if (tab == HomeTab.SETTINGS) current else _uiState.value.previousTab
        _uiState.update { 
            it.copy(currentTab = tab, previousTab = nextPrevious, showProfilePopup = false)
        }
    }

    fun goBack() {
        val previous = _uiState.value.previousTab
        
        // SOLUTION : Rafraîchissement systématique si on revient sur HOME
        if (previous == HomeTab.HOME) {
            refreshAlbums()
        }
        
        _uiState.update { it.copy(currentTab = previous) }
    }

    fun onAlbumSelected(album: Album) {
        _uiState.update { 
            it.copy(selectedAlbum = album, currentTab = HomeTab.SWIPE, previousTab = HomeTab.HOME)
        }
    }

    fun toggleProfilePopup(visible: Boolean) {
        _uiState.update { it.copy(showProfilePopup = visible) }
    }

    fun setPlaybackBehavior(behavior: PlaybackBehavior) = viewModelScope.launch {
        sessionRepository.savePlaybackBehavior(behavior)
    }

    fun setThemeMode(theme: AppTheme) = viewModelScope.launch {
        sessionRepository.saveThemeMode(theme)
    }

    fun logout() = viewModelScope.launch {
        _uiState.update { it.copy(currentTab = HomeTab.HOME) }
        sessionRepository.clearSession()
    }

    fun onSearchQueryChanged(query: String) = _uiState.update { it.copy(searchQuery = query) }
    fun toggleLayoutMode() = _uiState.update { it.copy(isGridView = !it.isGridView) }
    fun getSessionRepository() = sessionRepository
}
