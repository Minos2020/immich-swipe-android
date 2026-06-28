package com.minos2020.immichswipe.data.repository

import com.minos2020.immichswipe.data.local.dao.AlbumDecisionCount
import com.minos2020.immichswipe.data.local.dao.SwipeDecisionDao
import com.minos2020.immichswipe.data.local.entity.SwipeDecisionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository qui gère les décisions de swipe.
 * Il fait le lien entre le ViewModel et le DAO (la base Room).
 */
class SwipeDecisionRepository(
    private val swipeDecisionDao: SwipeDecisionDao
) {
    /**
     * Observe le compte des décisions pour tous les albums.
     */
    fun getAllAlbumDecisionCounts(): Flow<List<AlbumDecisionCount>> {
        return swipeDecisionDao.getAllAlbumDecisionCounts()
    }

    /**
     * Enregistre un nouveau swipe en base locale.
     */
    suspend fun saveDecision(assetId: String, albumId: String, decision: String, fileSize: Long? = null, isSynced: Boolean = false) {
        val entity = SwipeDecisionEntity(
            assetId = assetId,
            albumId = albumId,
            decision = decision,
            fileSize = fileSize,
            createdAt = System.currentTimeMillis(),
            isSynced = isSynced
        )
        swipeDecisionDao.insertDecision(entity)
    }

    /**
     * Marque plusieurs assets comme synchronisés.
     */
    suspend fun markAsSynced(assetIds: List<String>) {
        if (assetIds.isNotEmpty()) {
            swipeDecisionDao.markAsSynced(assetIds)
        }
    }

    /**
     * Récupère toutes les décisions d'un album sous forme de Flow.
     */
    fun getDecisionsForAlbum(albumId: String): Flow<List<SwipeDecisionEntity>> {
        return swipeDecisionDao.getDecisionsForAlbum(albumId)
    }

    /**
     * Supprime une décision (si l'utilisateur veut annuler un swipe par exemple).
     */
    suspend fun removeDecision(assetId: String, albumId: String) {
        swipeDecisionDao.deleteDecision(assetId, albumId)
    }

    /**
     * Supprime plusieurs décisions d'un coup pour un album donné.
     */
    suspend fun removeDecisions(assetIds: List<String>, albumId: String) {
        swipeDecisionDao.deleteDecisions(assetIds, albumId)
    }

    /**
     * Supprime toutes les décisions pour une liste d'assets, sur TOUS les albums.
     * À utiliser après une synchronisation réussie (suppression réelle sur Immich).
     */
    suspend fun removeDecisionsFromAllAlbums(assetIds: List<String>) {
        swipeDecisionDao.deleteDecisionsForAllAlbums(assetIds)
    }

    /**
     * Nettoie les décisions d'un album.
     */
    suspend fun clearAlbumDecisions(albumId: String) {
        swipeDecisionDao.deleteDecisionsForAlbum(albumId)
    }

    /**
     * Supprime les SKIP expirés de la base de données.
     */
    suspend fun cleanExpiredSkips(lifespanDays: Long) {
        if (lifespanDays <= 0) return
        val threshold = System.currentTimeMillis() - (lifespanDays * 24 * 60 * 60 * 1000L)
        swipeDecisionDao.deleteExpiredSkips(threshold)
    }
}
