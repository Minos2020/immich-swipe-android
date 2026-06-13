package com.example.immichswipe.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.immichswipe.data.repository.AlbumRepository
import com.example.immichswipe.data.repository.SessionRepository

class HomeViewModelFactory(
    private val sessionRepository: SessionRepository,
    private val albumRepository: AlbumRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(sessionRepository, albumRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
