package com.example.immichswipe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Représente une décision de tri (Swipe) pour un asset donné.
 *
 * @property assetId L'identifiant unique de la photo/vidéo sur le serveur Immich.
 *                   C'est notre clé primaire car on ne veut qu'une seule décision par asset.
 * @property albumId L'identifiant de l'album auquel appartient cet asset.
 *                   Utile pour filtrer les décisions par album.
 * @property decision La décision prise : "KEEP" (Garder), "DELETE" (Supprimer), "SKIP" (Passer).
 * @property createdAt Le moment où la décision a été prise (pour l'historique).
 * @property isSynced Indique si cette décision a été synchronisée avec le serveur Immich.
 */
@Entity(tableName = "swipe_decisions")
data class SwipeDecisionEntity(
    @PrimaryKey val assetId: String,
    val albumId: String,
    val decision: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
