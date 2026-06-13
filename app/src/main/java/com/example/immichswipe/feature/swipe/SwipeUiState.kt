package com.example.immichswipe.feature.swipe

import com.example.immichswipe.domain.model.Asset

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
    val albumName: String = "",
    val assets: List<Asset> = emptyList(),
    val currentIndex: Int = 0,
    val decisions: Map<String, SwipeDecision> = emptyMap(),
    val history: List<String> = emptyList(), // Liste des IDs swipés pour l'undo
    val error: String? = null
) {
    val currentAsset: Asset? get() = assets.getOrNull(currentIndex)
    val remainingCount: Int get() = assets.size - currentIndex
}
