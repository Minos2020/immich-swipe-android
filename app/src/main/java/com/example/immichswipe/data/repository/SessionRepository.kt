package com.example.immichswipe.data.repository

import android.content.Context
import com.example.immichswipe.data.datastore.SessionDataStore

class SessionRepository(context: Context) {

    private val dataStore = SessionDataStore(context)

    suspend fun saveSession(baseUrl: String, token: String) {
        dataStore.saveSession(baseUrl, token)
    }

    fun getToken() = dataStore.getApiKey()

    fun getBaseUrl() = dataStore.getBaseUrl()

    suspend fun clearSession() {
        dataStore.clearSession()
    }
}