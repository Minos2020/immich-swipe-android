package com.minos2020.immichswipe.core

import com.minos2020.immichswipe.data.api.ImmichApi
import com.minos2020.immichswipe.data.api.RetrofitFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.ui.graphics.Color

/**
 * Représente les différents niveaux de santé de la connexion.
 */
enum class ConnectionLevel(val color: Color) {
    ONLINE(Color(0xFF4CAF50)),  // Vert
    ISSUES(Color(0xFFFF9800)),  // Orange
    OFFLINE(Color(0xFFF44336))  // Rouge
}

data class ConnectionStatus(
    val level: ConnectionLevel = ConnectionLevel.ONLINE,
    val message: String = "Connecté au serveur",
    val hint: String? = null,
    val lastUpdate: Long = System.currentTimeMillis()
)

object SessionManager {

    private var config: SessionConfig? = null

    var api: ImmichApi? = null
        private set

    // Flux global indiquant la santé de la connexion.
    private val _connectionStatus = MutableStateFlow(ConnectionStatus())
    val connectionStatus = _connectionStatus.asStateFlow()

    fun updateStatus(level: ConnectionLevel, message: String, hint: String? = null) {
        _connectionStatus.value = ConnectionStatus(level, message, hint)
    }

    fun initialize(config: SessionConfig) {
        this.config = config
        this.api = RetrofitFactory.create(config)
    }

    fun clear() {
        config = null
        api = null
        _connectionStatus.value = ConnectionStatus(ConnectionLevel.OFFLINE, "Déconnecté")
    }

    fun isLoggedIn(): Boolean = api != null
    fun getBaseUrl(): String? = config?.baseUrl
    fun getApiKey(): String? = config?.apiKey
}
