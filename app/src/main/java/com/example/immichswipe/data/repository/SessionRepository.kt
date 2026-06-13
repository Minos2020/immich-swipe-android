package com.example.immichswipe.data.repository

import android.content.Context
import com.example.immichswipe.core.SessionConfig
import com.example.immichswipe.data.datastore.SessionDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Repository gérant la persistence de la session utilisateur.
 * C'est la Source Unique de Vérité (SSOT) pour l'état de connexion.
 */
class SessionRepository(context: Context) {

    private val dataStore = SessionDataStore(context)

    /**
     * Expose la configuration de session actuelle sous forme de Flow.
     * Si l'un des deux éléments (URL ou Clé) est manquant, émet null.
     */
    val sessionConfig: Flow<SessionConfig?> = combine(
        dataStore.getBaseUrl(),
        dataStore.getApiKey()
    ) { url, key ->
        if (url != null && key != null) {
            SessionConfig(url, key)
        } else {
            null
        }
    }

    /**
     * Sauvegarde une nouvelle session. 
     * Grâce au Flow ci-dessus, tous les observateurs seront notifiés automatiquement.
     */
    suspend fun saveSession(baseUrl: String, token: String) {
        dataStore.saveSession(baseUrl, token)
    }

    /**
     * Supprime la session actuelle (Déconnexion).
     */
    suspend fun clearSession() {
        dataStore.clearSession()
    }
}
