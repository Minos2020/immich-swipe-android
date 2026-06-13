package com.example.immichswipe.data.repository

import com.example.immichswipe.data.api.ImmichApi
import com.example.immichswipe.domain.model.Album

/**
 * Repository gérant la récupération des albums depuis le serveur Immich.
 */
class AlbumRepository(
    private val api: ImmichApi
) {
    /**
     * Récupère la liste de tous les albums.
     */
    suspend fun getAlbums(): List<Album> {
        return api.getAlbums()
    }
}
