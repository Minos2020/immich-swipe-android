package com.example.immichswipe.data.repository

import com.example.immichswipe.data.api.ImmichApi

class AuthRepository(
    private val api: ImmichApi
) {
    suspend fun testConnection() = api.getCurrentUser()
}