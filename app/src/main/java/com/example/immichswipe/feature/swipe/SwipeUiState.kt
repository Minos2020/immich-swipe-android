package com.example.immichswipe.feature.swipe

import com.example.immichswipe.domain.model.Asset
import com.example.immichswipe.core.PlaybackBehavior
import com.example.immichswipe.core.IconPosition

/**
 * Les différentes décisions possibles pour un asset.
 */
enum class SwipeDecision {
    KEEP, DELETE, SKIP
}

/**
 * État de la session de tri (Swipe).
 */
data class SwipeUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val showSuccessAnimation: Boolean = false,
    val showSummary: Boolean = false,
    val albumName: String = "",
    val assets: List<Asset> = emptyList(),
    val currentIndex: Int = 0,
    val decisions: Map<String, SwipeDecision> = emptyMap(),
    val history: List<String> = emptyList(), // Liste des IDs swipés pour l'undo
    val error: String? = null,
    val playbackBehavior: PlaybackBehavior = PlaybackBehavior.PAUSE_OTHERS,
    val isSwipeInverted: Boolean = false,
    val fullscreenButtonPosition: IconPosition = IconPosition.TOP_RIGHT,
    val immichButtonPosition: IconPosition = IconPosition.TOP_LEFT
) {
    val currentAsset: Asset? get() = assets.getOrNull(currentIndex)
    
    // Statistiques de tri basées sur les décisions réelles
    val totalCount: Int get() = assets.size
    val processedCount: Int get() = decisions.size
    val keptCount: Int get() = decisions.values.count { it == SwipeDecision.KEEP }
    val deletedCount: Int get() = decisions.values.count { it == SwipeDecision.DELETE }
    val skippedCount: Int get() = decisions.values.count { it == SwipeDecision.SKIP }
    val remainingCount: Int get() = totalCount - processedCount
    
    // Progression (0.0f à 1.0f)
    val progress: Float get() = if (totalCount > 0) processedCount.toFloat() / totalCount else 0f
}
