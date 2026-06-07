package com.example.immichswipe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.immichswipe.feature.home.HomeScreen
import com.example.immichswipe.ui.theme.ImmichSwipeTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.immichswipe.data.repository.SessionRepository
import com.example.immichswipe.feature.auth.AuthScreen
import com.example.immichswipe.feature.auth.AuthViewModel
import com.example.immichswipe.feature.auth.AuthViewModelFactory
import com.example.immichswipe.feature.common.LoadingScreen
import com.example.immichswipe.feature.home.HomeViewModel
import com.example.immichswipe.feature.home.HomeViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionRepository = SessionRepository(applicationContext)

        setContent {
            ImmichSwipeTheme {

                val appViewModel: AppViewModel = viewModel(
                    factory = AppViewModelFactory(sessionRepository)
                )

                val state by appViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    appViewModel.checkSession()
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AnimatedContent(
                        targetState = state,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                        },
                        label = "ScreenTransition",
                        modifier = Modifier.padding(innerPadding)
                    ) { targetState ->
                        when {
                            targetState.isLoading -> {
                                LoadingScreen()
                            }

                            targetState.isLoggedIn -> {
                                HomeScreen(
                                    viewModel = viewModel(
                                        factory = HomeViewModelFactory(sessionRepository)
                                    )
                                )
                            }

                            else -> {
                                AuthScreen(
                                    viewModel = viewModel(
                                        factory = AuthViewModelFactory(sessionRepository)
                                    ),
                                    onLoginSuccess = { appViewModel.checkSession() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}