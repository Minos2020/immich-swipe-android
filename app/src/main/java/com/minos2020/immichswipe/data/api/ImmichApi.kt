package com.minos2020.immichswipe.data.api

import com.minos2020.immichswipe.domain.model.Album
import com.minos2020.immichswipe.domain.model.Asset
import com.minos2020.immichswipe.domain.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Path
import retrofit2.http.POST

interface ImmichApi {

    @GET("api/users/me")
    suspend fun getCurrentUser(): User

    @GET("api/albums")
    suspend fun getAlbums(): List<Album>

//    @GET("api/albums/{id}")
//    suspend fun getAlbumInfo(@Path("id") albumId: String): AlbumWithAssets

    // Nouveau Endpoint à utiliser à partir de la version v3 du serveur Immich
    @POST("api/search/metadata")
    suspend fun searchAssets(@Body request: SearchAssetsRequest): SearchResponse

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
 * Corps de la requête pour récupérer des assets.
 * Pourra contenir beaucoup d'autres paramètres si besoin !
 */
data class SearchAssetsRequest(
    val albumIds: List<String>? = null,
    val size: Int = 1000, //pour éviter les histoires de pagination pour l'instant
)

data class SearchResponse(
    val assets: SearchAssetResult
)

/**
 * Détail des assets trouvés.
 */
data class SearchAssetResult(
    val items: List<Asset>,
    val total: Int
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

