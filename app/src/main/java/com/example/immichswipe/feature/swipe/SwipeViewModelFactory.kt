package com.example.immichswipe.feature.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.immichswipe.data.repository.AssetRepository
import com.example.immichswipe.data.repository.SessionRepository
import com.example.immichswipe.data.repository.SwipeDecisionRepository
import com.example.immichswipe.domain.model.Album

class SwipeViewModelFactory(
    private val assetRepository: AssetRepository,
    private val sessionRepository: SessionRepository,
    private val swipeDecisionRepository: SwipeDecisionRepository,
    private val album: Album
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SwipeViewModel::class.java)) {
            return SwipeViewModel(assetRepository, sessionRepository, swipeDecisionRepository, album) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
