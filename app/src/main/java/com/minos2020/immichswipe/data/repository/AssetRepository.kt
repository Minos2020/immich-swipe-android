package com.minos2020.immichswipe.data.repository

import com.minos2020.immichswipe.data.api.DeleteAssetsRequest
import com.minos2020.immichswipe.data.api.ImmichApi
import com.minos2020.immichswipe.data.api.SearchAssetsRequest
import com.minos2020.immichswipe.data.api.UpdateAssetsRequest
import com.minos2020.immichswipe.data.local.dao.SwipeDecisionDao
import com.minos2020.immichswipe.domain.model.Album
import com.minos2020.immichswipe.domain.model.Asset
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/**
 * Repository gérant les photos et vidéos (Assets).
 */
class AssetRepository(
    private val api: ImmichApi,
    private val swipeDecisionDao: SwipeDecisionDao? = null
) {
    /**
     * Récupère toutes les photos d'un album.
     */
    suspend fun getAssetsByAlbum(albumId: String, includeArchived: Boolean = false, userId: String? = null): List<Asset> {
        if (albumId == Album.VIRTUAL_SKIPPED_ID && swipeDecisionDao != null && userId != null) {
            // Album virtuel : On récupère les IDs depuis la base locale pour cet utilisateur
            val skippedDecisions = swipeDecisionDao.getSyncedSkipDecisions(userId).first()
            val assetIds = skippedDecisions.map { it.assetId }
            
            if (assetIds.isEmpty()) return emptyList()

            // On utilise l'API de recherche avec la liste d'IDs pour être plus efficace
            return fetchAllAssets(SearchAssetsRequest(ids = assetIds))
        }

        return if (includeArchived) {
            coroutineScope {
                val timelineDeferred = async { fetchAllAssets(SearchAssetsRequest(albumIds = listOf(albumId), visibility = "timeline")) }
                val archiveDeferred = async { fetchAllAssets(SearchAssetsRequest(albumIds = listOf(albumId), visibility = "archive")) }
                
                val timeline = try { timelineDeferred.await() } catch (_: Exception) { emptyList() }
                val archive = try { archiveDeferred.await() } catch (_: Exception) { emptyList() }
                
                (timeline + archive).sortedByDescending { it.fileCreatedAt }
            }
        } else {
            try {
                fetchAllAssets(SearchAssetsRequest(albumIds = listOf(albumId), visibility = "timeline"))
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Récupère TOUS les assets correspondant à une requête en gérant la pagination.
     */
    private suspend fun fetchAllAssets(baseRequest: SearchAssetsRequest): List<Asset> {
        val allItems = mutableListOf<Asset>()
        var nextToFetch: String? = "1" // On commence à la page 1

        while (nextToFetch != null) {
            val response = api.searchAssets(
                baseRequest.copy(
                    size = 1000,
                    page = nextToFetch.toIntOrNull() ?: 1
                )
            )
            allItems.addAll(response.assets.items)
            
            // Le serveur nous dit quelle est la prochaine page à charger
            nextToFetch = response.assets.nextPage

            // Sécurité : évite les boucles infinies
            if (allItems.size > 500000) break
        }
        return allItems
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

    /**
     * Met à jour plusieurs assets sur le serveur Immich.
     */
    suspend fun updateAssets(
        assetIds: List<String>,
        isFavorite: Boolean? = null,
        visibility: String? = null
    ) {
        if (assetIds.isNotEmpty()) {
            api.updateAssets(
                UpdateAssetsRequest(
                    ids = assetIds,
                    isFavorite = isFavorite,
                    visibility = visibility
                )
            )
        }
    }
}
