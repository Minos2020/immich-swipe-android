package com.example.immichswipe.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.immichswipe.core.SessionManager
import com.example.immichswipe.feature.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState: HomeUiState by viewModel.uiState.collectAsState()

    // Charger l'utilisateur au premier affichage
    LaunchedEffect(Unit) {
        viewModel.loadUser()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Logo (Utilise FilterNone comme icône temporaire comme sur ton dessin)
                        Icon(
                            imageVector = Icons.Default.FilterNone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        // Texte stylisé : immichSwipe
                        Text(
                            text = buildAnnotatedString {
                                append("immich")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("Swipe")
                                }
                            },
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    // Indicateur de connexion discret
                    val isConnected = uiState.user != null
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) Color.Green else Color.Red)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isConnected) "Connected" else "Disconnected",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    // Photo de profil avec bordure violette comme sur ta capture
                    val baseUrl = SessionManager.getBaseUrl()
                    val profilePath = uiState.user?.profileImagePath
                    
                    val profileModifier = Modifier
                        .padding(end = 16.dp)
                        .size(36.dp)
                        .border(1.5.dp, Color(0xFF9C27B0), CircleShape) // Bordure violette
                        .padding(2.dp)
                        .clip(CircleShape)

                    if ((profilePath != null) && (baseUrl != null)) {
                        AsyncImage(
                            model = "$baseUrl/$profilePath",
                            contentDescription = "Profile Picture",
                            modifier = profileModifier,
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Default Profile",
                            modifier = profileModifier,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Default.Swipe, contentDescription = null) },
                    label = { Text("Swipe") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator()
                }

                uiState.error != null -> {
                    Text("Erreur : ${uiState.error}", color = MaterialTheme.colorScheme.error)
                }

                uiState.user != null -> {
                    Text("Bienvenue, ${uiState.user!!.name} !")
                }
                
                else -> {
                    Text("Veuillez vous connecter")
                }
            }
        }
    }
}