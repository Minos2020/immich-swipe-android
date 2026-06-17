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

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}