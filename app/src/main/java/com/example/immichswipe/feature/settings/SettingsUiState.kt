package com.example.immichswipe.feature.settings

import com.example.immichswipe.core.AppTheme
import com.example.immichswipe.core.IconPosition
import com.example.immichswipe.core.PlaybackBehavior

/**
 * État de l'écran des paramètres.
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val userName: String = "",
    val playbackBehavior: PlaybackBehavior = PlaybackBehavior.PAUSE_OTHERS,
    val themeMode: AppTheme = AppTheme.SYSTEM,
    val isSwipeInverted: Boolean = false,
    val fullscreenButtonPosition: IconPosition = IconPosition.TOP_RIGHT,
    val immichButtonPosition: IconPosition = IconPosition.TOP_LEFT,
    val isDefaultLayoutGrid: Boolean = false
)
