package com.minos2020.immichswipe.data.repository

import com.minos2020.immichswipe.data.api.DeleteAssetsRequest
import com.minos2020.immichswipe.data.api.ImmichApi
import com.minos2020.immichswipe.data.api.SearchAssetsRequest
import com.minos2020.immichswipe.domain.model.Asset

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
        val request = SearchAssetsRequest(albumIds = listOf(albumId))
        return api.searchAssets(request).assets.items
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
