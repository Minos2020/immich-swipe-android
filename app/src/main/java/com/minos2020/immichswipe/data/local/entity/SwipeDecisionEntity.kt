package com.minos2020.immichswipe.data.local.entity

import androidx.room.Entity

/**
 * Représente une décision de tri (Swipe) pour un asset donné dans un album donné.
 *
 * La clé primaire est composée du couple (assetId, albumId) car une même photo
 * peut être présente dans plusieurs albums Immich.
 *
 * @property assetId L'identifiant unique de la photo/vidéo sur le serveur Immich.
 * @property albumId L'identifiant de l'album auquel appartient cet asset dans cette session.
 * @property decision La décision prise : "KEEP" (Garder), "DELETE" (Supprimer), "SKIP" (Passer).
 * @property createdAt Le moment où la décision a été prise (pour l'historique).
 * @property isSynced Indique si cette décision a été synchronisée avec le serveur Immich.
 */
@Entity(
    tableName = "swipe_decisions",
    primaryKeys = ["assetId", "albumId", "userId"]
)
data class SwipeDecisionEntity(
    val assetId: String,
    val albumId: String,
    val userId: String,
    val decision: String,
    val fileSize: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
