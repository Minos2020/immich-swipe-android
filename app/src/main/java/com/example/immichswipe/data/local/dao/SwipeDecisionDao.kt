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
     * Récupère une décision spécifique.
     */
    @Query("SELECT * FROM swipe_decisions WHERE assetId = :assetId")
    suspend fun getDecisionForAsset(assetId: String): SwipeDecisionEntity?

    /**
     * Supprime toutes les décisions d'un album (par exemple après une synchro réussie).
     */
    @Query("DELETE FROM swipe_decisions WHERE albumId = :albumId")
    suspend fun deleteDecisionsForAlbum(albumId: String)
    
    /**
     * Supprime une décision spécifique.
     */
    @Query("DELETE FROM swipe_decisions WHERE assetId = :assetId")
    suspend fun deleteDecision(assetId: String)
}
