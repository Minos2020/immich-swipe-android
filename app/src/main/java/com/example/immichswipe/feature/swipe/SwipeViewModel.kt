package com.example.immichswipe.feature.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.immichswipe.data.repository.AssetRepository
import com.example.immichswipe.core.PlaybackBehavior
import com.example.immichswipe.core.IconPosition
import com.example.immichswipe.data.repository.SessionRepository
import com.example.immichswipe.domain.model.Album
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SwipeViewModel(
    private val assetRepository: AssetRepository,
    private val sessionRepository: SessionRepository,
    private val album: Album
) : ViewModel() {

    private val _uiState = MutableStateFlow(SwipeUiState(albumName = album.albumName))
    val uiState: StateFlow<SwipeUiState> = _uiState.asStateFlow()

    init {
        loadAssets()
        observePlaybackBehavior()
        observeSwipeInversion()
        observeFullscreenButtonPosition()
        observeImmichButtonPosition()
    }

    private fun observePlaybackBehavior() {
        viewModelScope.launch {
            sessionRepository.playbackBehavior.collect { behavior ->
                _uiState.value = _uiState.value.copy(playbackBehavior = behavior)
            }
        }
    }

    private fun observeSwipeInversion() {
        viewModelScope.launch {
            sessionRepository.swipeInverted.collect { inverted ->
                _uiState.value = _uiState.value.copy(isSwipeInverted = inverted)
            }
        }
    }

    private fun observeFullscreenButtonPosition() {
        viewModelScope.launch {
            sessionRepository.fullscreenButtonPosition.collect { pos ->
                _uiState.value = _uiState.value.copy(fullscreenButtonPosition = pos)
            }
        }
    }

    private fun observeImmichButtonPosition() {
        viewModelScope.launch {
            sessionRepository.immichButtonPosition.collect { pos ->
                _uiState.value = _uiState.value.copy(immichButtonPosition = pos)
            }
        }
    }

    private fun loadAssets() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val assets = assetRepository.getAssetsByAlbum(album.id)
                _uiState.value = _uiState.value.copy(
                    assets = assets,
                    isLoading = false
                )
                // On charge les détails du premier asset
                if (assets.isNotEmpty()) {
                    loadAssetDetail(assets[0].id, 0)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erreur lors du chargement des photos"
                )
            }
        }
    }

    private fun loadAssetDetail(assetId: String, index: Int) {
        viewModelScope.launch {
            try {
                val detail = assetRepository.getAssetDetail(assetId)
                val currentAssets = _uiState.value.assets.toMutableList()
                if (index < currentAssets.size) {
                    currentAssets[index] = detail
                    _uiState.value = _uiState.value.copy(assets = currentAssets)
                }
            } catch (e: Exception) {
                // Erreur silencieuse pour les détails
                android.util.Log.e("SWIPE_VM", "Erreur details asset: ${e.message}")
            }
        }
    }

    fun onSwipe(decision: SwipeDecision) {
        val currentState = _uiState.value
        val currentAsset = currentState.currentAsset ?: return

        val newDecisions = currentState.decisions.toMutableMap()
        newDecisions[currentAsset.id] = decision

        val newHistory = currentState.history.toMutableList()
        newHistory.add(currentAsset.id)

        // Trouver le prochain asset non traité
        val assets = currentState.assets
        var nextIndex = -1
        
        // 1. Chercher d'abord après l'index actuel
        for (i in (currentState.currentIndex + 1) until assets.size) {
            if (!newDecisions.containsKey(assets[i].id)) {
                nextIndex = i
                break
            }
        }
        
        // 2. Si non trouvé, repartir du début
        if (nextIndex == -1) {
            for (i in 0 until currentState.currentIndex) {
                if (!newDecisions.containsKey(assets[i].id)) {
                    nextIndex = i
                    break
                }
            }
        }
        
        // 3. Si toujours rien, on a vraiment fini l'album
        if (nextIndex == -1) {
            nextIndex = assets.size // Marqueur de fin
        }

        _uiState.value = currentState.copy(
            currentIndex = nextIndex,
            decisions = newDecisions,
            history = newHistory
        )

        // On charge les détails du prochain asset pour anticiper
        if (nextIndex < assets.size) {
            loadAssetDetail(assets[nextIndex].id, nextIndex)
        }
    }

    fun undo() {
        val currentState = _uiState.value
        if (currentState.currentIndex > 0) {
            val previousIndex = currentState.currentIndex - 1
            val previousAssetId = currentState.assets[previousIndex].id
            
            val newDecisions = currentState.decisions.toMutableMap()
            newDecisions.remove(previousAssetId)

            val newHistory = currentState.history.toMutableList()
            if (newHistory.isNotEmpty()) newHistory.removeAt(newHistory.size - 1)

            _uiState.value = currentState.copy(
                currentIndex = previousIndex,
                decisions = newDecisions,
                history = newHistory
            )
            // On recharge les détails au cas où ils auraient été perdus
            loadAssetDetail(previousAssetId, previousIndex)
        }
    }

    /**
     * Permet de sauter directement à un asset précis (via la timeline).
     */
    fun onMoveToAsset(index: Int) {
        if (index in 0 until _uiState.value.assets.size) {
            _uiState.value = _uiState.value.copy(currentIndex = index)
            loadAssetDetail(_uiState.value.assets[index].id, index)
        }
    }
}
