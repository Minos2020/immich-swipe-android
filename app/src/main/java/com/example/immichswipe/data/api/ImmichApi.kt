package com.example.immichswipe.data.api

import com.example.immichswipe.domain.model.Album
import com.example.immichswipe.domain.model.Asset
import com.example.immichswipe.domain.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Path

interface ImmichApi {

    @GET("api/users/me")
    suspend fun getCurrentUser(): User

    @GET("api/albums")
    suspend fun getAlbums(): List<Album>

    @GET("api/albums/{id}")
    suspend fun getAlbumInfo(@Path("id") albumId: String): AlbumWithAssets

    @GET("api/assets/{id}")
    suspend fun getAssetDetail(@Path("id") assetId: String): Asset

    /**
     * Supprime une liste d'assets (les déplace vers la corbeille).
     * Retourne 204 No Content en cas de succès.
     */
    @HTTP(method = "DELETE", path = "api/assets", hasBody = true)
    suspend fun deleteAssets(@Body request: DeleteAssetsRequest)
}

/**
 * Corps de la requête pour supprimer des assets.
 */
data class DeleteAssetsRequest(
    val ids: List<String>,
    val force: Boolean = false
)

/**
 * Objet intermédiaire car l'API Immich renvoie les détails de l'album 
 * ET la liste des assets dans le même appel.
 */
data class AlbumWithAssets(
    val id: String,
    val albumName: String,
    val assets: List<Asset>
)

