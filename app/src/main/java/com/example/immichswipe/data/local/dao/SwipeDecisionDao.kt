package com.example.immichswipe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.immichswipe.data.local.entity.SwipeDecisionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface pour accéder aux données des décisions de swipe en base.
 * DAO = Data Access Object
 */
@Dao
interface SwipeDecisionDao {

    /**
     * Insère ou met à jour une décision.
     * OnConflictStrategy.REPLACE permet d'écraser une ancienne décision si on swipe à nouveau la même photo.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(decision: SwipeDecisionEntity)

    /**
     * Récupère toutes les décisions pour un album spécifique.
     * On utilise un Flow pour être notifié automatiquement dès que la base change.
     */
    @Query("SELECT * FROM swipe_decisions WHERE albumId = :albumId")
    fun getDecisionsForAlbum(albumId: String): Flow<List<SwipeDecisionEntity>>

    /**
     * Récupère une décision spécifique pour un asset dans un album donné.
     */
    @Query("SELECT * FROM swipe_decisions WHERE assetId = :assetId AND albumId = :albumId")
    suspend fun getDecisionForAsset(assetId: String, albumId: String): SwipeDecisionEntity?

    /**
     * Supprime toutes les décisions d'un album (par exemple après une synchro réussie).
     */
    @Query("DELETE FROM swipe_decisions WHERE albumId = :albumId")
    suspend fun deleteDecisionsForAlbum(albumId: String)
    
    /**
     * Supprime une décision spécifique pour un asset dans un album donné.
     */
    @Query("DELETE FROM swipe_decisions WHERE assetId = :assetId AND albumId = :albumId")
    suspend fun deleteDecision(assetId: String, albumId: String)

    /**
     * Supprime plusieurs décisions d'un coup pour un album donné.
     */
    @Query("DELETE FROM swipe_decisions WHERE assetId IN (:assetIds) AND albumId = :albumId")
    suspend fun deleteDecisions(assetIds: List<String>, albumId: String)

    /**
     * Supprime toutes les décisions liées à une liste d'assets spécifique, peu importe l'album.
     * Utile quand les assets sont définitivement supprimés du serveur Immich.
     */
    @Query("DELETE FROM swipe_decisions WHERE assetId IN (:assetIds)")
    suspend fun deleteDecisionsForAllAlbums(assetIds: List<String>)
    
    /**
     * Compte le nombre de décisions prises pour un album spécifique.
     */
    @Query("SELECT COUNT(*) FROM swipe_decisions WHERE albumId = :albumId")
    suspend fun getDecisionCountForAlbum(albumId: String): Int

    /**
     * Récupère les statistiques de décisions pour tous les albums sous forme de Flow.
     * On compte le total et spécifiquement les demandes de suppression.
     */
    @Query("""
        SELECT albumId, 
               COUNT(*) as totalCount, 
               SUM(CASE WHEN decision = 'DELETE' THEN 1 ELSE 0 END) as deleteCount 
        FROM swipe_decisions 
        GROUP BY albumId
    """)
    fun getAllAlbumDecisionCounts(): Flow<List<AlbumDecisionCount>>
}

/**
 * Objet pour transporter les statistiques par album.
 */
data class AlbumDecisionCount(
    val albumId: String,
    val totalCount: Int,
    val deleteCount: Int
)
