package com.example.immichswipe.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.immichswipe.R
import com.example.immichswipe.core.SessionManager
import com.example.immichswipe.data.repository.AssetRepository
import com.example.immichswipe.domain.model.Album
import com.example.immichswipe.feature.swipe.SwipeScreen
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    assetRepository: AssetRepository,
    modifier: Modifier = Modifier,
) {
    val uiState: HomeUiState by viewModel.uiState.collectAsState()

    // Charger l'utilisateur et les albums au premier affichage
    LaunchedEffect(Unit) {
        viewModel.loadUser()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = R.drawable.logo_immichswipe_couleurs),
                        contentDescription = "Logo Immich Swipe",
                        modifier = Modifier
                            .height(35.dp)
                            .padding(vertical = 4.dp),
                        contentScale = ContentScale.Fit
                    )
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

                    val baseUrl = SessionManager.getBaseUrl()
                    val userId = uiState.user?.id
                    
                    val profileModifier = Modifier
                        .padding(end = 16.dp)
                        .size(36.dp)
                        .border(1.dp, Color(0xFF9C27B0), CircleShape)
                        .padding(2.dp)
                        .clip(CircleShape)

                    if ((userId != null) && (baseUrl != null)) {
                        val cleanBaseUrl = baseUrl.removeSuffix("/")
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("$cleanBaseUrl/api/users/$userId/profile-image")
                                .addHeader("x-api-key", SessionManager.getApiKey() ?: "")
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile Picture",
                            placeholder = rememberVectorPainter(Icons.Default.AccountCircle),
                            error = rememberVectorPainter(Icons.Default.AccountCircle),
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
                    selected = uiState.currentTab == HomeTab.HOME,
                    onClick = { viewModel.onTabSelected(HomeTab.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = uiState.currentTab == HomeTab.SWIPE,
                    onClick = { viewModel.onTabSelected(HomeTab.SWIPE) },
                    icon = { Icon(Icons.Default.Swipe, contentDescription = null) },
                    label = { Text("Swipe") }
                )
                NavigationBarItem(
                    selected = uiState.currentTab == HomeTab.SETTINGS,
                    onClick = { viewModel.onTabSelected(HomeTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                HomeTab.HOME -> {
                    if (uiState.isLoading && uiState.albums.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (uiState.error != null) {
                        ErrorView(error = uiState.error!!, onRetry = { viewModel.loadUser() })
                    } else {
                        AlbumList(
                            albums = uiState.albums,
                            isRefreshing = uiState.isRefreshing,
                            onRefresh = { viewModel.refreshAlbums() },
                            onAlbumClick = { viewModel.onAlbumSelected(it) }
                        )
                    }
                }
                HomeTab.SWIPE -> {
                    if (uiState.selectedAlbum != null) {
                        SwipeScreen(
                            album = uiState.selectedAlbum!!,
                            assetRepository = assetRepository
                        )
                    } else {
                        SwipePlaceholder(selectedAlbum = null)
                    }
                }
                HomeTab.SETTINGS -> {
                    SettingsView(
                        userName = uiState.user?.name ?: "Utilisateur",
                        onLogout = { viewModel.logout() }
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumList(
    albums: List<Album>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onAlbumClick: (Album) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Mes Albums",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(albums) { album ->
                AlbumItem(album = album, onClick = { onAlbumClick(album) })
            }
        }
    }
}

@Composable
fun AlbumItem(album: Album, onClick: () -> Unit) {
    val baseUrl = SessionManager.getBaseUrl()?.removeSuffix("/")
    val apiKey = SessionManager.getApiKey() ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (album.albumThumbnailAssetId != null && baseUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("$baseUrl/api/assets/${album.albumThumbnailAssetId}/thumbnail?format=WEBP")
                            .addHeader("x-api-key", apiKey)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        placeholder = rememberVectorPainter(Icons.Default.PhotoLibrary),
                        error = rememberVectorPainter(Icons.Default.PhotoLibrary)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = album.albumName, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                if (!album.description.isNullOrBlank()) {
                    Text(text = album.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline, maxLines = 2)
                }
                Text(text = "${album.assetCount} photos", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
fun SwipePlaceholder(selectedAlbum: Album?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Swipe, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            if (selectedAlbum != null) {
                Text("Session de tri : ${selectedAlbum.albumName}", fontWeight = FontWeight.Bold)
                Text("${selectedAlbum.assetCount} photos à découvrir", fontSize = 14.sp)
            } else {
                Text("Sélectionnez un album pour commencer !")
            }
        }
    }
}

@Composable
fun SettingsView(userName: String, onLogout: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Paramètres", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Text("Connecté en tant que : $userName", fontSize = 14.sp)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Se déconnecter")
        }
    }
}

@Composable
fun ErrorView(error: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Oups ! Une erreur est survenue", color = MaterialTheme.colorScheme.error)
            Text(error, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Réessayer") }
        }
    }
}
