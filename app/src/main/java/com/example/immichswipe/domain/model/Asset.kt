package com.example.immichswipe.domain.model

/**
 * Représente une photo ou vidéo (Asset) sur Immich.
 */
data class Asset(
    val id: String,
    val deviceAssetId: String,
    val ownerId: String,
    val fileCreatedAt: String,
    val type: String, // IMAGE ou VIDEO
    val duration: String? = null,
    val isFavorite: Boolean = false,
    val originalFileName: String? = null,
    val fileExtension: String? = null,
    val exifInfo: ExifInfo? = null
)

data class ExifInfo(
    val fileSizeInBytes: Long? = null,
    val projectionType: String? = null,
    val orientation: String? = null,
    val dateTimeOriginal: String? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null
)
