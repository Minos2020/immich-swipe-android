package com.example.immichswipe.data.api

import com.example.immichswipe.domain.model.User
import retrofit2.http.GET

interface ImmichApi {

    @GET("api/users/me")
    suspend fun getCurrentUser(): User
}

