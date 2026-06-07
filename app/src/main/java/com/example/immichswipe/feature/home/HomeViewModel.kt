package com.example.immichswipe.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.immichswipe.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context
import com.example.immichswipe.core.SessionManager
import com.example.immichswipe.data.datastore.SessionDataStore
import com.example.immichswipe.data.repository.SessionRepository
import kotlinx.coroutines.flow.first

// On définit HomeViewModel à partir de la classe existante ViewModel
class HomeViewModel(
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val repository = UserRepository(
        SessionManager.api
            ?: throw IllegalStateException("Session not initialized")
    )
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun testSaveSession() {
        viewModelScope.launch {
            sessionRepository.saveSession(
                baseUrl = "https://immich.test.fr",
                token = "fake-token"
            )
        }
    }

    fun testReadSession() {
        viewModelScope.launch {
            val token = sessionRepository.getToken().first()
            val baseUrl = sessionRepository.getBaseUrl().first()

            android.util.Log.d("SESSION", "token = $token")
            android.util.Log.d("SESSION", "baseUrl = $baseUrl")
        }
    }

    fun loadUser() {
        // Permet de lancer la requête dans une coroutine pour ne pas freeze l'UI pendant son exécution
        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)

            try {
                val user = repository.getCurrentUser()

                _uiState.value = HomeUiState(user = user, isLoading = false)

            } catch (e: Exception) {

                _uiState.value = HomeUiState(error = e.message, isLoading = false)
            }
        }
    }


}