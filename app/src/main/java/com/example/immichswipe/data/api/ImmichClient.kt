//package com.example.immichswipe.data.api
//
//import com.example.immichswipe.core.ApiConfig
//import okhttp3.Interceptor
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//
///**
// * Singleton gérant la configuration et l'instance Retrofit pour communiquer avec l'API Immich.
// */
//object ImmichClient {
//
//    // Intercepteur pour logger les requêtes et réponses HTTP (très utile pour le débogage)
//    private val logging = HttpLoggingInterceptor().apply {
//        level = HttpLoggingInterceptor.Level.BODY
//    }
//    /**
//     * Intercepteur pour ajouter automatiquement la clé API dans les headers de chaque requête.
//     */
//    private val apiKeyInterceptor = Interceptor { chain ->
//        val request = chain.request()
//            .newBuilder()
//            .addHeader("x-api-key", ApiConfig.API_KEY)
//            .build()
//        chain.proceed(request)
//    }
//
//    // Client HTTP OkHttp qui gère l'envoi des requêtes réseau
//    private val httpClient = OkHttpClient.Builder()
//        .addInterceptor(apiKeyInterceptor)
//        .addInterceptor(logging)
//        .build()
//
//    /**
//     * Instance de l'interface ImmichApi initialisée de manière "lazy" (uniquement lors du premier accès).
//     *
//     */
//    val api: ImmichApi by lazy {
//        Retrofit.Builder()
//            .baseUrl(ApiConfig.BASE_URL)
//            .client(httpClient)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(ImmichApi::class.java)
//    }
//}
