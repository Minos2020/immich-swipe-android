package com.example.immichswipe.feature.home

import com.example.immichswipe.domain.model.User

data class HomeUiState (
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null
)