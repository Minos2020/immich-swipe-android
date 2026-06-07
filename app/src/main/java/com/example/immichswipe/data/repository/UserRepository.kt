package com.example.immichswipe.data.repository

import com.example.immichswipe.data.api.ImmichApi
import com.example.immichswipe.domain.model.User

class UserRepository(
    private val api: ImmichApi
) {
    suspend fun getCurrentUser() = api.getCurrentUser()
}