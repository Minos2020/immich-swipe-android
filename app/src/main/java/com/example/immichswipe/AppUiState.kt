package com.example.immichswipe

import com.example.immichswipe.core.AppTheme

data class AppUiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val themeMode: AppTheme = AppTheme.SYSTEM
)
