package com.example.immichswipe.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.immichswipe.core.SessionConfig
import com.example.immichswipe.core.SessionManager
import com.example.immichswipe.data.api.RetrofitFactory
import com.example.immichswipe.data.repository.AuthRepository
import com.example.immichswipe.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onBaseUrlChange(value: String) {
        _uiState.value = _uiState.value.copy(baseUrl = value)
    }

    fun onApiKeyChange(value: String) {
        _uiState.value = _uiState.value.copy(apiKey = value)
    }

    fun login() {
        viewModelScope.launch {

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
//                // Appel à l'API
//                val api = RetrofitFactory.create(
//                    SessionConfig(
//                        baseUrl = _uiState.value.baseUrl,
//                        apiKey = _uiState.value.apiKey
//                    )
//                )
//
//                val authRepository = AuthRepository(api)
//
//                authRepository.testConnection()

                val config = SessionConfig(
                    baseUrl = _uiState.value.baseUrl,
                    apiKey = _uiState.value.apiKey
                )
                SessionManager.initialize(config)

                SessionManager.api?.getCurrentUser()

                // Sauvegarde de la session
                sessionRepository.saveSession(
                    baseUrl = _uiState.value.baseUrl,
                    token = _uiState.value.apiKey
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    success = true
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}