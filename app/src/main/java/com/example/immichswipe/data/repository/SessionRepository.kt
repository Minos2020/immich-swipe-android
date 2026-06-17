package com.example.immichswipe.data.repository

import android.content.Context
import com.example.immichswipe.core.AppTheme
import com.example.immichswipe.core.IconPosition
import com.example.immichswipe.core.PlaybackBehavior
import com.example.immichswipe.core.SessionConfig
import com.example.immichswipe.data.datastore.SessionDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

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
     * Expose le comportement de lecture actuel.
     * Par défaut: PAUSE_OTHERS.
     */
    val playbackBehavior: Flow<PlaybackBehavior> = dataStore.getAudioFocusMode().map { modeString ->
        if (modeString == null) return@map PlaybackBehavior.PAUSE_OTHERS
        try {
            PlaybackBehavior.valueOf(modeString)
        } catch (e: Exception) {
            PlaybackBehavior.PAUSE_OTHERS
        }
    }

    /**
     * Expose le thème actuel.
     */
    val themeMode: Flow<AppTheme> = dataStore.getThemeMode().map {
        it?.let { try { AppTheme.valueOf(it) } catch(e: Exception) { AppTheme.SYSTEM } } ?: AppTheme.SYSTEM
    }

    /**
     * Expose l'inversion du swipe.
     */
    val swipeInverted: Flow<Boolean> = dataStore.isSwipeInverted()

    /**
     * Expose la position de l'icône plein écran.
     */
    val fullscreenButtonPosition: Flow<IconPosition> = dataStore.getFullscreenIconPosition().map {
        it?.let { try { IconPosition.valueOf(it) } catch(e: Exception) { IconPosition.TOP_RIGHT } } ?: IconPosition.TOP_RIGHT
    }

    /**
     * Expose la position de l'icône Immich.
     */
    val immichButtonPosition: Flow<IconPosition> = dataStore.getImmichIconPosition().map {
        it?.let { try { IconPosition.valueOf(it) } catch(e: Exception) { IconPosition.TOP_LEFT } } ?: IconPosition.TOP_LEFT
    }

    /**
     * Sauvegarde une nouvelle session. 
     * Grâce au Flow ci-dessus, tous les observateurs seront notifiés automatiquement.
     */
    suspend fun saveSession(baseUrl: String, token: String) {
        dataStore.saveSession(baseUrl, token)
    }

    /**
     * Sauvegarde la préférence de lecture.
     */
    suspend fun savePlaybackBehavior(behavior: PlaybackBehavior) {
        dataStore.saveAudioFocusMode(behavior.name)
    }

    /**
     * Sauvegarde le thème.
     */
    suspend fun saveThemeMode(theme: AppTheme) {
        dataStore.saveThemeMode(theme.name)
    }

    /**
     * Sauvegarde l'inversion du swipe.
     */
    suspend fun saveSwipeInverted(inverted: Boolean) {
        dataStore.saveSwipeInverted(inverted)
    }

    /**
     * Sauvegarde la position de l'icône plein écran.
     */
    suspend fun saveFullscreenButtonPosition(pos: IconPosition) {
        dataStore.saveFullscreenIconPosition(pos.name)
    }

    /**
     * Sauvegarde la position de l'icône Immich.
     */
    suspend fun saveImmichButtonPosition(pos: IconPosition) {
        dataStore.saveImmichIconPosition(pos.name)
    }

    /**
     * Supprime la session actuelle (Déconnexion).
     */
    suspend fun clearSession() {
        dataStore.clearSession()
    }
}
