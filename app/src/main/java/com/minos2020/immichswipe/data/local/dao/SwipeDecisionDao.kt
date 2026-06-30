package com.minos2020.immichswipe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.minos2020.immichswipe.data.local.entity.SwipeDecisionEntity
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
     * Récupère toutes les décisions pour un album spécifique d'un utilisateur donné.
     * On utilise un Flow pour être notifié automatiquement dès que la base change.
     */
    @Query("SELECT * FROM swipe_decisions WHERE albumId = :albumId AND userId = :userId")
    fun getDecisionsForAlbum(albumId: String, userId: String): Flow<List<SwipeDecisionEntity>>

    /**
     * Récupère une décision spécifique pour un asset dans un album donné pour un utilisateur.
     */
    @Query("SELECT * FROM swipe_decisions WHERE assetId = :assetId AND albumId = :albumId AND userId = :userId")
    suspend fun getDecisionForAsset(assetId: String, albumId: String, userId: String): SwipeDecisionEntity?

    /**
     * Supprime toutes les décisions d'un album pour un utilisateur.
     */
    @Query("DELETE FROM swipe_decisions WHERE albumId = :albumId AND userId = :userId")
    suspend fun deleteDecisionsForAlbum(albumId: String, userId: String)
    
    /**
     * Supprime une décision spécifique pour un asset dans un album donné pour un utilisateur.
     */
    @Query("DELETE FROM swipe_decisions WHERE assetId = :assetId AND albumId = :albumId AND userId = :userId")
    suspend fun deleteDecision(assetId: String, albumId: String, userId: String)

    /**
     * Supprime plusieurs décisions d'un coup pour un album donné pour un utilisateur.
     */
    @Query("DELETE FROM swipe_decisions WHERE assetId IN (:assetIds) AND albumId = :albumId AND userId = :userId")
    suspend fun deleteDecisions(assetIds: List<String>, albumId: String, userId: String)

    /**
     * Supprime toutes les décisions liées à une liste d'assets spécifique pour un utilisateur.
     */
    @Query("DELETE FROM swipe_decisions WHERE assetId IN (:assetIds) AND userId = :userId")
    suspend fun deleteDecisionsForAllAlbums(assetIds: List<String>, userId: String)
    
    /**
     * Compte le nombre de décisions prises pour un album spécifique d'un utilisateur.
     */
    @Query("SELECT COUNT(*) FROM swipe_decisions WHERE albumId = :albumId AND userId = :userId")
    suspend fun getDecisionCountForAlbum(albumId: String, userId: String): Int

    /**
     * Marque des décisions comme synchronisées pour un utilisateur.
     */
    @Query("UPDATE swipe_decisions SET isSynced = 1 WHERE assetId IN (:assetIds) AND userId = :userId")
    suspend fun markAsSynced(assetIds: List<String>, userId: String)

    /**
     * Supprime les décisions 'SKIP' NON SYNCHRONISÉES plus vieilles qu'un certain timestamp pour tous les utilisateurs.
     * (Ou on pourrait filtrer par utilisateur, mais le nettoyage global est souvent plus simple).
     */
    @Query("DELETE FROM swipe_decisions WHERE decision = 'SKIP' AND isSynced = 0 AND createdAt < :threshold")
    suspend fun deleteExpiredSkips(threshold: Long)

    /**
     * Récupère toutes les décisions 'SKIP' déjà synchronisées pour un utilisateur.
     */
    @Query("SELECT * FROM swipe_decisions WHERE decision = 'SKIP' AND isSynced = 1 AND userId = :userId")
    fun getSyncedSkipDecisions(userId: String): Flow<List<SwipeDecisionEntity>>

    /**
     * Compte le nombre de décisions 'SKIP' synchronisées pour un utilisateur.
     */
    @Query("SELECT COUNT(*) FROM swipe_decisions WHERE decision = 'SKIP' AND isSynced = 1 AND userId = :userId")
    fun getSyncedSkipCount(userId: String): Flow<Int>

    /**
     * Migre les données d'une version précédente (sans userId) vers l'utilisateur actuel.
     */
    @Query("UPDATE swipe_decisions SET userId = :userId WHERE userId = 'legacy_user'")
    suspend fun migrateLegacyData(userId: String)

    /**
     * Récupère les statistiques de décisions pour tous les albums d'un utilisateur sous forme de Flow.
     */
    @Query("""
        SELECT albumId, 
               COUNT(*) as totalCount, 
               SUM(CASE WHEN isSynced = 0 THEN 1 ELSE 0 END) as unsyncedCount 
        FROM swipe_decisions 
        WHERE userId = :userId
        GROUP BY albumId
    """)
    fun getAllAlbumDecisionCounts(userId: String): Flow<List<AlbumDecisionCount>>
}

/**
 * Objet pour transporter les statistiques par album.
 */
data class AlbumDecisionCount(
    val albumId: String,
    val totalCount: Int,
    val unsyncedCount: Int
)
