package com.minos2020.immichswipe.feature.swipe

import com.minos2020.immichswipe.domain.model.Asset
import com.minos2020.immichswipe.core.PlaybackBehavior
import com.minos2020.immichswipe.core.IconPosition

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
    val immichButtonPosition: IconPosition = IconPosition.TOP_LEFT,
    val skipLifespanDays: Long = 0L
) {
    val currentAsset: Asset? get() = assets.getOrNull(currentIndex)
    
    // Statistiques de tri basées sur les décisions réelles
    val totalCount: Int get() = assets.size
    val processedCount: Int get() = decisions.size
    val keptCount: Int get() = decisions.values.count { it == SwipeDecision.KEEP }
    val deletedCount: Int get() = decisions.values.count { it == SwipeDecision.DELETE }
    val skippedCount: Int get() = decisions.values.count { it == SwipeDecision.SKIP }
    val remainingCount: Int get() = totalCount - processedCount
    
    // Calcul des poids (en bytes)
    val keptSize: Long get() = assets.filter { decisions[it.id] == SwipeDecision.KEEP }.sumOf { it.exifInfo?.fileSizeInBytes ?: 0L }
    val deletedSize: Long get() = assets.filter { decisions[it.id] == SwipeDecision.DELETE }.sumOf { it.exifInfo?.fileSizeInBytes ?: 0L }
    val skippedSize: Long get() = assets.filter { decisions[it.id] == SwipeDecision.SKIP }.sumOf { it.exifInfo?.fileSizeInBytes ?: 0L }
    val remainingSize: Long get() = assets.filter { !decisions.containsKey(it.id) }.sumOf { it.exifInfo?.fileSizeInBytes ?: 0L }

    // Progression (0.0f à 1.0f)
    val progress: Float get() = if (totalCount > 0) processedCount.toFloat() / totalCount else 0f
}
