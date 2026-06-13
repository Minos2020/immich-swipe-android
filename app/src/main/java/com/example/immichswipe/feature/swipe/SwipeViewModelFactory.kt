package com.example.immichswipe.feature.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.immichswipe.data.repository.AssetRepository
import com.example.immichswipe.domain.model.Album

class SwipeViewModelFactory(
    private val assetRepository: AssetRepository,
    private val album: Album
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SwipeViewModel::class.java)) {
            return SwipeViewModel(assetRepository, album) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
