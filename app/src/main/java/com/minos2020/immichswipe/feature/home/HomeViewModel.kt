package com.minos2020.immichswipe.feature.home

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minos2020.immichswipe.data.repository.UserRepository
import com.minos2020.immichswipe.data.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.minos2020.immichswipe.core.SessionManager
import kotlinx.coroutines.flow.update
import com.minos2020.immichswipe.core.PlaybackBehavior
import com.minos2020.immichswipe.core.AppTheme
import com.minos2020.immichswipe.data.repository.SessionRepository
import com.minos2020.immichswipe.data.repository.SwipeDecisionRepository
import com.minos2020.immichswipe.data.repository.AssetRepository
import com.minos2020.immichswipe.domain.model.Album

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

        viewModelScope.launch {
            sessionRepository.includeArchived.collect { include ->
                _uiState.update { it.copy(includeArchived = include) }
                // On rafraîchit les albums si cette option change car les comptes vont changer
                refreshAlbums()
            }
        }

        // Applique le mode d'affichage par défaut au démarrage
        viewModelScope.launch {
            val isGrid = sessionRepository.defaultLayoutGrid.first()
            _uiState.update { it.copy(isGridView = isGrid) }
        }

        // SOLUTION : Observe l'état de santé global de la connexion.
        // Puisque SessionManager met à jour son Flow à chaque requête réseau,
        // la pastille réagira à tout (refresh, swipe, vidéo, etc.)
        viewModelScope.launch {
            SessionManager.connectionStatus.collect { status ->
                _uiState.update { it.copy(connectionStatus = status) }
            }
        }

        // Observe les décisions locales pour mettre à jour les barres de progression
        viewModelScope.launch {
            sessionRepository.sessionConfig.collect { config ->
                if (config != null) {
                    swipeDecisionRepository.getAllAlbumDecisionCounts(config.userId).collect { stats ->
                        val treatedMap = stats.associate { it.albumId to it.totalCount }
                        val unsyncedMap = stats.associate { it.albumId to it.unsyncedCount }
                        _uiState.update { 
                            it.copy(
                                albumTreatedCounts = treatedMap,
                                albumUnsyncedChanges = unsyncedMap
                            )
                        }
                    }
                }
            }
        }

        // Observe le nombre de SKIP synchronisés pour l'album virtuel
        viewModelScope.launch {
            sessionRepository.sessionConfig.collect { config ->
                if (config != null) {
                    combine(
                        swipeDecisionRepository.getSyncedSkipCount(config.userId),
                        sessionRepository.includeArchived
                    ) { count, _ ->
                        count // Imprecision accepted for now
                    }.collect { adjustedCount ->
                        _uiState.update { it.copy(syncedSkipCount = adjustedCount) }
                    }
                }
            }
        }
    }

    fun loadUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val user = userRepository.getCurrentUser()
                // SOLUTION : Migration des anciennes données (v3 -> v4) vers l'ID utilisateur réel
                swipeDecisionRepository.migrateLegacyDecisions(user.id)

                val albums = albumRepository.refreshAlbums(_uiState.value.includeArchived)
                _uiState.update { 
                    it.copy(
                        user = user, 
                        albums = albums, 
                        isLoading = false, 
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Erreur de chargement", 
                        isLoading = false
                    )
                }
            }
        }
    }

    fun refreshAlbums() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                // On mémorise l'heure de début
                val startTime = System.currentTimeMillis()
                
                // On lance la requête
                val albums = albumRepository.refreshAlbums(_uiState.value.includeArchived)
                
                // On calcule combien de temps a duré la requête
                val duration = System.currentTimeMillis() - startTime
                // On attend le complément pour atteindre au moins 800ms
                if (duration < 800) {
                    delay(800 - duration)
                }

                _uiState.update { 
                    it.copy(
                        albums = albums, 
                        isRefreshing = false, 
                        error = null
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isRefreshing = false
                    ) 
                }
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
        
        /*// SOLUTION : Rafraîchissement systématique si on revient sur HOME
        if (previous == HomeTab.HOME) {
            refreshAlbums()
        }*/
        
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
    
    fun updateVirtualNames(id: String, name: String, description: String? = null) {
        _uiState.update { 
            val newNames = it.virtualNames.toMutableMap()
            newNames[id] = name
            val newDescs = it.virtualDescriptions.toMutableMap()
            if (description != null) {
                newDescs[id] = description
            }
            it.copy(virtualNames = newNames, virtualDescriptions = newDescs)
        }
    }

    fun toggleLayoutMode() = _uiState.update { it.copy(isGridView = !it.isGridView) }
    fun getSessionRepository() = sessionRepository
}
