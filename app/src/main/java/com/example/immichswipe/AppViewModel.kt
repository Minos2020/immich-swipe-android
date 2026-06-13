package com.example.immichswipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.immichswipe.core.SessionManager
import com.example.immichswipe.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel principal de l'application. 
 * Son rôle est d'observer la session et de décider quel écran afficher.
 */
class AppViewModel(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        // Au démarrage, on lance l'observation réactive de la session
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            // On s'abonne au tuyau de la session
            sessionRepository.sessionConfig.collect { config ->
                if (config != null) {
                    // Si on reçoit une config, on initialise le manager de session
                    SessionManager.initialize(config)
                    
                    // On met à jour l'UI : on est connecté !
                    _uiState.value = AppUiState(
                        isLoading = false,
                        isLoggedIn = true
                    )
                } else {
                    // Si on reçoit null, on nettoie tout
                    SessionManager.clear()
                    
                    // On met à jour l'UI : on est déconnecté
                    _uiState.value = AppUiState(
                        isLoading = false,
                        isLoggedIn = false
                    )
                }
            }
        }
    }
}
