package com.example.immichswipe.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// extension DataStore attachée au Context
val Context.dataStore by preferencesDataStore(name = "session")

class SessionDataStore(private val context: Context) {

    companion object {
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_AUDIO_FOCUS = stringPreferencesKey("audio_focus")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_SWIPE_INVERTED = androidx.datastore.preferences.core.booleanPreferencesKey("swipe_inverted")
        private val KEY_FULLSCREEN_ICON_POS = stringPreferencesKey("fullscreen_icon_pos")
        private val KEY_IMMICH_ICON_POS = stringPreferencesKey("immich_icon_pos")
        private val KEY_DEFAULT_LAYOUT_GRID = androidx.datastore.preferences.core.booleanPreferencesKey("default_layout_grid")
    }

    suspend fun saveSession(baseUrl: String, apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = baseUrl
            prefs[KEY_API_KEY] = apiKey
        }
    }

    fun getBaseUrl(): Flow<String?> {
        return context.dataStore.data.map { it[KEY_BASE_URL] }
    }

    fun getApiKey(): Flow<String?> {
        return context.dataStore.data.map { it[KEY_API_KEY] }
    }

    fun getAudioFocusMode(): Flow<String?> {
        return context.dataStore.data.map { it[KEY_AUDIO_FOCUS] }
    }

    suspend fun saveAudioFocusMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUDIO_FOCUS] = mode
        }
    }

    fun getThemeMode(): Flow<String?> = context.dataStore.data.map { it[KEY_THEME_MODE] }
    
    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    fun isSwipeInverted(): Flow<Boolean> = context.dataStore.data.map { it[KEY_SWIPE_INVERTED] ?: false }
    
    suspend fun saveSwipeInverted(inverted: Boolean) {
        context.dataStore.edit { it[KEY_SWIPE_INVERTED] = inverted }
    }

    fun getFullscreenIconPosition(): Flow<String?> = context.dataStore.data.map { it[KEY_FULLSCREEN_ICON_POS] }
    
    suspend fun saveFullscreenIconPosition(pos: String) {
        context.dataStore.edit { it[KEY_FULLSCREEN_ICON_POS] = pos }
    }

    fun getImmichIconPosition(): Flow<String?> = context.dataStore.data.map { it[KEY_IMMICH_ICON_POS] }

    suspend fun saveImmichIconPosition(pos: String) {
        context.dataStore.edit { it[KEY_IMMICH_ICON_POS] = pos }
    }

    fun isDefaultLayoutGrid(): Flow<Boolean> = context.dataStore.data.map { it[KEY_DEFAULT_LAYOUT_GRID] ?: false }

    suspend fun saveDefaultLayoutGrid(isGrid: Boolean) {
        context.dataStore.edit { it[KEY_DEFAULT_LAYOUT_GRID] = isGrid }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}