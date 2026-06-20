package com.minos2020.immichswipe.feature.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minos2020.immichswipe.core.SessionConfig
import com.minos2020.immichswipe.core.SessionManager
import com.minos2020.immichswipe.data.repository.AuthRepository
import com.minos2020.immichswipe.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel gérant la logique de l'écran de connexion.
 * Il délègue les appels réseau au AuthRepository.
 */
class AuthViewModel(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // On observe la session : si elle devient null (déconnexion), 
        // on réinitialise l'état du formulaire de login.
        observeSessionReset()
    }

    private fun observeSessionReset() {
        viewModelScope.launch {
            sessionRepository.sessionConfig.collect { config ->
                if (config == null) {
                    resetState()
                }
            }
        }
    }

    fun onBaseUrlChange(value: String) {
        _uiState.value = _uiState.value.copy(baseUrl = value)
    }

    fun onApiKeyChange(value: String) {
        _uiState.value = _uiState.value.copy(apiKey = value)
    }

    /**
     * Remet l'état à zéro (utile après une déconnexion).
     */
    private fun resetState() {
        _uiState.value = AuthUiState()
    }

    /**
     * Tente de connecter l'utilisateur.
     */
    fun login() {
        viewModelScope.launch {
            // Log pour inspecter l'état actuel avant de lancer la connexion
            Log.d("AUTH_DEBUG", "Tentative de connexion avec l'état : ${_uiState.value}")

            // On affiche le chargement et on réinitialise les erreurs
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val baseUrl = _uiState.value.baseUrl
                val apiKey = _uiState.value.apiKey

                // 1. On demande au Repository de vérifier les identifiants
                // Si ça échoue, ça lève une exception et on part dans le bloc 'catch'
                authRepository.checkCredentials(baseUrl, apiKey)

                // 2. Si on arrive ici, c'est que la connexion a réussi !
                // On initialise la session globale (SessionManager)
                val config = SessionConfig(baseUrl = baseUrl, apiKey = apiKey)
                SessionManager.initialize(config)

                // 3. On sauvegarde la session dans le stockage local (DataStore)
                sessionRepository.saveSession(baseUrl = baseUrl, token = apiKey)

                // 4. On met à jour l'UI pour signaler le succès
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    success = true
                )

            } catch (e: Exception) {
                // En cas d'erreur (mauvais URL, mauvaise clé, pas d'internet...)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Une erreur inconnue est survenue"
                )
            }
        }
    }
}
