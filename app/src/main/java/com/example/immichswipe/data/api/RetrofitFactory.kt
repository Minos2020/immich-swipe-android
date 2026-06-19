package com.example.immichswipe.data.api

import com.example.immichswipe.core.ConnectionLevel
import com.example.immichswipe.core.SessionConfig
import com.example.immichswipe.core.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object RetrofitFactory {
    fun create(config: SessionConfig): ImmichApi {
        // Intercepteur pour logger les requêtes et réponses HTTP
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        // Intercepteur pour ajouter automatiquement la clé API dans les headers de chaque requête.
        val apiKeyInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("x-api-key", config.apiKey)
                .build()
            chain.proceed(request)
        }

        // SOLUTION : Intercepteur de Diagnostic intelligent sur TOUTES les requêtes réseau
        val connectivityInterceptor = Interceptor { chain ->
            try {
                val response = chain.proceed(chain.request())
                
                when (response.code) {
                    in 200..299 -> {
                        SessionManager.updateStatus(ConnectionLevel.ONLINE, "Connecté à Immich")
                    }
                    401, 403 -> {
                        SessionManager.updateStatus(
                            ConnectionLevel.ISSUES, 
                            "Problème d'authentification", 
                            "Vérifiez si votre clé API est toujours valide dans les réglages d'Immich."
                        )
                    }
                    502, 503, 504 -> {
                        SessionManager.updateStatus(
                            ConnectionLevel.ISSUES, 
                            "API Immich indisponible (${response.code})", 
                            "Votre reverse-proxy (Caddy/Nginx) répond, mais le container Immich semble arrêté ou en cours de redémarrage."
                        )
                    }
                    else -> {
                        SessionManager.updateStatus(ConnectionLevel.ISSUES, "Réponse inattendue (${response.code})")
                    }
                }
                response
            } catch (e: Exception) {
                val (msg, hint) = when (e) {
                    is UnknownHostException -> 
                        "Serveur introuvable (DNS)" to "L'URL est incorrecte ou votre domaine n'est pas encore propagé."
                    is SocketTimeoutException -> 
                        "Délai d'attente dépassé" to "Le serveur est trop lent à répondre. Vérifiez votre connexion montante (Upload) ou la charge du serveur."
                    is IOException -> 
                        "Pas de connexion internet" to "Vérifiez que votre téléphone capte le réseau ou que votre serveur est bien allumé et exposé sur internet."
                    else -> 
                        "Erreur de connexion" to e.localizedMessage
                }
                SessionManager.updateStatus(ConnectionLevel.OFFLINE, msg, hint)
                throw e
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(connectivityInterceptor)
            .addInterceptor(logging)
            .build()

        // Configure Retrofit avec l'URL de base, le client HTTP et le convertisseur JSON (Gson).
        val retrofit = Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ImmichApi::class.java)
    }

}