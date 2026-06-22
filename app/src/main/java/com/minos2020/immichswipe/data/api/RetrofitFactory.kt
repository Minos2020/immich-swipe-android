package com.minos2020.immichswipe.data.api

import com.minos2020.immichswipe.core.ConnectionLevel
import com.minos2020.immichswipe.core.DiagStatus
import com.minos2020.immichswipe.core.SessionConfig
import com.minos2020.immichswipe.core.SessionManager
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
            level = HttpLoggingInterceptor.Level.BODY
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
                        SessionManager.updateStatus(ConnectionLevel.ONLINE, DiagStatus.CONNECTED)
                    }
                    401, 403 -> {
                        SessionManager.updateStatus(ConnectionLevel.ISSUES, DiagStatus.AUTH_ERROR)
                    }
                    502, 503, 504 -> {
                        SessionManager.updateStatus(ConnectionLevel.ISSUES, DiagStatus.UNAVAILABLE, response.code)
                    }
                    else -> {
                        SessionManager.updateStatus(ConnectionLevel.ISSUES, DiagStatus.UNEXPECTED, response.code)
                    }
                }
                response
            } catch (e: Exception) {
                val status = when (e) {
                    is UnknownHostException -> DiagStatus.DNS_ERROR
                    is SocketTimeoutException -> DiagStatus.TIMEOUT
                    is IOException -> DiagStatus.NO_INTERNET
                    else -> DiagStatus.CONNECTION_ERROR
                }
                SessionManager.updateStatus(ConnectionLevel.OFFLINE, status, rawMessage = e.localizedMessage)
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
