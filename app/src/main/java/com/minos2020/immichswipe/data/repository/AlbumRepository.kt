package com.minos2020.immichswipe.data.repository

import com.minos2020.immichswipe.data.api.ImmichApi
import com.minos2020.immichswipe.domain.model.Album

/**
 * Repository gérant la récupération des albums depuis le serveur Immich.
 */
class AlbumRepository(
    private val api: ImmichApi
) {
    /**
     * Rafraîchit la liste des albums depuis le serveur.
     */
    suspend fun refreshAlbums(): List<Album> {
        return api.getAlbums()
    }
}
