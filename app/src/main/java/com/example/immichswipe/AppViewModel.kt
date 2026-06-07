package com.example.immichswipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.immichswipe.core.SessionConfig
import com.example.immichswipe.core.SessionManager
import com.example.immichswipe.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AppViewModel(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState = _uiState.asStateFlow()

    fun checkSession() {
        viewModelScope.launch {

            _uiState.value = AppUiState(isLoading = true)

            val baseUrl = sessionRepository.getBaseUrl().firstOrNull()
            val apiKey = sessionRepository.getToken().firstOrNull()

            if (baseUrl != null && apiKey != null) {

                val config = SessionConfig(baseUrl, apiKey)

                SessionManager.initialize(config)

                _uiState.value = AppUiState(
                    isLoading = false,
                    isLoggedIn = true
                )

            } else {
                _uiState.value = AppUiState(
                    isLoading = false,
                    isLoggedIn = false
                )
            }
        }
    }
}