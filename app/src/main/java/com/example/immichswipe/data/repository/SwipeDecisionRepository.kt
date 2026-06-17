package com.example.immichswipe.data.repository

import com.example.immichswipe.data.local.dao.AlbumDecisionCount
import com.example.immichswipe.data.local.dao.SwipeDecisionDao
import com.example.immichswipe.data.local.entity.SwipeDecisionEntity
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
    suspend fun saveDecision(assetId: String, albumId: String, decision: String) {
        val entity = SwipeDecisionEntity(
            assetId = assetId,
            albumId = albumId,
            decision = decision,
            createdAt = System.currentTimeMillis(),
            isSynced = false
        )
        swipeDecisionDao.insertDecision(entity)
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
        assetIds.forEach { swipeDecisionDao.deleteDecision(it, albumId) }
    }

    /**
     * Nettoie les décisions d'un album.
     */
    suspend fun clearAlbumDecisions(albumId: String) {
        swipeDecisionDao.deleteDecisionsForAlbum(albumId)
    }
}
