package com.example.immichswipe.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.immichswipe.data.repository.AuthRepository
import com.example.immichswipe.data.repository.SessionRepository

class AuthViewModelFactory(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Vérifie si la classe demandée est bien AuthViewModel
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            // Crée l'instance avec les deux repositories requis
            return AuthViewModel(sessionRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}