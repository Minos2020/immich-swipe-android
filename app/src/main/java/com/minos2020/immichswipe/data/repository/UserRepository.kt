package com.minos2020.immichswipe.data.repository

import com.minos2020.immichswipe.data.api.ImmichApi
import com.minos2020.immichswipe.domain.model.User

class UserRepository(
    private val api: ImmichApi
) {
    suspend fun getCurrentUser() = api.getCurrentUser()
}