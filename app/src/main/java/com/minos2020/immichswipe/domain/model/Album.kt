package com.minos2020.immichswipe.domain.model

/**
 * Représente un album Immich.
 */
data class Album(
    val id: String,
    val albumName: String,
    val description: String? = null,
    val assetCount: Int,
    val albumThumbnailAssetId: String?
)
