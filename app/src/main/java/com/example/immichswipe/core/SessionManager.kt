package com.example.immichswipe.core

import com.example.immichswipe.data.api.ImmichApi
import com.example.immichswipe.data.api.RetrofitFactory

object SessionManager {

    private var config: SessionConfig? = null

    var api: ImmichApi? = null
        private set

    fun initialize(config: SessionConfig) {
        this.config = config
        this.api = RetrofitFactory.create(config)
    }

    fun clear() {
        config = null
        api = null
    }

    fun isLoggedIn(): Boolean {
        return api != null
    }

    fun getBaseUrl(): String? = config?.baseUrl

    fun getApiKey(): String? = config?.apiKey
}