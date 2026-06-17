package com.example.immichswipe.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.immichswipe.data.repository.AlbumRepository
import com.example.immichswipe.data.repository.SessionRepository
import com.example.immichswipe.data.repository.SwipeDecisionRepository
import com.example.immichswipe.data.repository.AssetRepository

class HomeViewModelFactory(
    private val sessionRepository: SessionRepository,
    private val albumRepository: AlbumRepository,
    private val swipeDecisionRepository: SwipeDecisionRepository,
    private val assetRepository: AssetRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(sessionRepository, albumRepository, swipeDecisionRepository, assetRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
