package com.minos2020.immichswipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minos2020.immichswipe.core.AppTheme
import com.minos2020.immichswipe.core.SessionManager
import com.minos2020.immichswipe.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel principal de l'application (niveau Activity).
 * Il s'occupe de l'initialisation et du thème.
 */
class AppViewModel(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        // Au démarrage, on lance l'observation réactive de la session
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            // On s'abonne au tuyau de la session
            sessionRepository.sessionConfig.collect { config ->
                if (config != null) {
                    // Si on a une config sauvegardée, on initialise le SessionManager
                    SessionManager.initialize(config)

                    // On met à jour l'UI : on est connecté !
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true
                    )
                } else {
                    // Si on reçoit null, on nettoie tout
                    SessionManager.clear()

                    // On met à jour l'UI : on est déconnecté
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = false
                    )
                }
            }
        }
    }

    private fun observeTheme() {
        viewModelScope.launch {
            sessionRepository.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
    }
}
