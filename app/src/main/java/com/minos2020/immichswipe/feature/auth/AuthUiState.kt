package com.minos2020.immichswipe.feature.auth

data class AuthUiState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)