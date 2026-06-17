package com.example.immichswipe.feature.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.immichswipe.data.repository.AssetRepository
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
    }

    private fun observePlaybackBehavior() {
        viewModelScope.launch {
            sessionRepository.playbackBehavior.collect { behavior ->
                _uiState.value = _uiState.value.copy(playbackBehavior = behavior)
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

        val nextIndex = currentState.currentIndex + 1
        _uiState.value = currentState.copy(
            currentIndex = nextIndex,
            decisions = newDecisions,
            history = newHistory
        )

        // On charge les détails du prochain asset pour anticiper
        if (nextIndex < currentState.assets.size) {
            loadAssetDetail(currentState.assets[nextIndex].id, nextIndex)
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
