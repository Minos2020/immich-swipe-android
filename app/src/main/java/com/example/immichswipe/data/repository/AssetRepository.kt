package com.example.immichswipe.data.repository

import com.example.immichswipe.data.api.DeleteAssetsRequest
import com.example.immichswipe.data.api.ImmichApi
import com.example.immichswipe.domain.model.Asset

/**
 * Repository gérant les photos et vidéos (Assets).
 */
class AssetRepository(
    private val api: ImmichApi
) {
    /**
     * Récupère toutes les photos d'un album.
     */
    suspend fun getAssetsByAlbum(albumId: String): List<Asset> {
        return api.getAlbumInfo(albumId).assets
    }

    /**
     * Récupère les détails complets (EXIF, taille...) d'un asset spécifique.
     */
    suspend fun getAssetDetail(assetId: String): Asset {
        return api.getAssetDetail(assetId)
    }

    /**
     * Supprime plusieurs assets du serveur Immich.
     */
    suspend fun deleteAssets(assetIds: List<String>) {
        if (assetIds.isNotEmpty()) {
            api.deleteAssets(DeleteAssetsRequest(ids = assetIds, force = false))
        }
    }
}
