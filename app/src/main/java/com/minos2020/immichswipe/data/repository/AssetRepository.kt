package com.minos2020.immichswipe.data.repository

import com.minos2020.immichswipe.data.api.DeleteAssetsRequest
import com.minos2020.immichswipe.data.api.ImmichApi
import com.minos2020.immichswipe.data.api.SearchAssetsRequest
import com.minos2020.immichswipe.data.api.UpdateAssetsRequest
import com.minos2020.immichswipe.data.local.dao.SwipeDecisionDao
import com.minos2020.immichswipe.domain.model.Album
import com.minos2020.immichswipe.domain.model.Asset
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    suspend fun getAssetsByAlbum(albumId: String, includeArchived: Boolean = false): List<Asset> {
        if (albumId == Album.VIRTUAL_SKIPPED_ID && swipeDecisionDao != null) {
            // Album virtuel : On récupère les IDs depuis la base locale
            val skippedDecisions = swipeDecisionDao.getSyncedSkipDecisions().first()
            val assetIds = skippedDecisions.map { it.assetId }
            
            // On récupère les détails pour chaque asset en parallèle
            return coroutineScope {
                assetIds.map { id ->
                    async {
                        try {
                            api.getAssetDetail(id)
                        } catch (_: Exception) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }
        }

        return if (includeArchived) {
            coroutineScope {
                val timelineDeferred = async { 
                    api.searchAssets(SearchAssetsRequest(albumIds = listOf(albumId), visibility = "timeline")) 
                }
                val archiveDeferred = async { 
                    api.searchAssets(SearchAssetsRequest(albumIds = listOf(albumId), visibility = "archive")) 
                }
                
                val timeline = try { timelineDeferred.await().assets.items } catch (_: Exception) { emptyList() }
                val archive = try { archiveDeferred.await().assets.items } catch (_: Exception) { emptyList() }
                
                (timeline + archive).sortedByDescending { it.fileCreatedAt }
            }
        } else {
            val request = SearchAssetsRequest(albumIds = listOf(albumId), visibility = "timeline")
            api.searchAssets(request).assets.items
        }
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
