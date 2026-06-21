package com.minos2020.immichswipe.feature.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.BackHandler
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.minos2020.immichswipe.R
import com.minos2020.immichswipe.core.AppTheme
import com.minos2020.immichswipe.core.SessionManager
import com.minos2020.immichswipe.data.repository.AssetRepository
import com.minos2020.immichswipe.data.repository.SwipeDecisionRepository
import com.minos2020.immichswipe.domain.model.Album
import com.minos2020.immichswipe.feature.settings.SettingsScreen
import com.minos2020.immichswipe.feature.settings.SettingsViewModel
import com.minos2020.immichswipe.feature.settings.SettingsViewModelFactory
import com.minos2020.immichswipe.feature.swipe.SwipeScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    assetRepository: AssetRepository,
    swipeDecisionRepository: SwipeDecisionRepository,
    modifier: Modifier = Modifier,
) {
    val uiState: HomeUiState by viewModel.uiState.collectAsState()
    val isSettings = uiState.currentTab == HomeTab.SETTINGS
    val isHome = uiState.currentTab == HomeTab.HOME

    // Charger l'utilisateur et les albums au premier affichage
    LaunchedEffect(Unit) {
        viewModel.loadUser()
    }

    // Gestion du retour physique/gestuel du téléphone
    BackHandler(enabled = uiState.currentTab != HomeTab.HOME) {
        viewModel.goBack()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (isSettings) {
                // Barre de titre pour les paramètres
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.goBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                // Barre principale avec logo et profil
                Column {
                    TopAppBar(
                        title = {
                            val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                            val logoRes = if (isDark) R.drawable.immichswipe_logo_colors_dark else R.drawable.immichswipe_logo_colors_light
                            
                            Image(
                                painter = painterResource(id = logoRes),
                                contentDescription = stringResource(R.string.app_name),
                                modifier = Modifier
                                    .height(35.dp)
                                    .padding(vertical = 4.dp),
                                contentScale = ContentScale.Fit
                            )
                        },
                        actions = {
                            if (isHome) {
                                // Bouton pour basculer le layout
                                IconButton(onClick = { viewModel.toggleLayoutMode() }) {
                                    Icon(
                                        imageVector = if (uiState.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                        contentDescription = stringResource(R.string.settings_layout_label)
                                    )
                                }
                            }

                            val baseUrl = SessionManager.getBaseUrl()
                            val userId = uiState.user?.id
                            
                            val profileModifier = Modifier
                                .padding(end = 16.dp)
                                .size(36.dp)
                                .border(1.dp, Color(0xFF9C27B0), CircleShape)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.toggleProfilePopup(true) }

                            Box(contentAlignment = Alignment.BottomEnd) {
                                if ((userId != null) && (baseUrl != null)) {
                                    val cleanBaseUrl = baseUrl.removeSuffix("/")
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data("$cleanBaseUrl/api/users/$userId/profile-image")
                                            .addHeader("x-api-key", SessionManager.getApiKey() ?: "")
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = stringResource(R.string.settings_section_account),
                                        placeholder = rememberVectorPainter(Icons.Default.AccountCircle),
                                        error = rememberVectorPainter(Icons.Default.AccountCircle),
                                        modifier = profileModifier,
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = stringResource(R.string.settings_section_account),
                                        modifier = profileModifier,
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }

                                // Indicateur de connexion (Badge tricolore)
                                Surface(
                                    modifier = Modifier
                                        .padding(end = 16.dp, bottom = 2.dp)
                                        .size(10.dp)
                                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                    color = uiState.connectionStatus.level.color,
                                    shape = CircleShape
                                ) {}
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Barre de recherche (uniquement sur Home)
                    if (isHome) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { Text(stringResource(R.string.home_search_placeholder), fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.common_cancel), modifier = Modifier.size(20.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        },
        bottomBar = {
            // On n'affiche la barre du bas QUE si on n'est pas dans les paramètres
            if (!isSettings) {
                NavigationBar {
                    NavigationBarItem(
                        selected = uiState.currentTab == HomeTab.HOME,
                        onClick = { viewModel.onTabSelected(HomeTab.HOME) },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_home)) }
                    )
                    NavigationBarItem(
                        selected = uiState.currentTab == HomeTab.SWIPE,
                        onClick = { viewModel.onTabSelected(HomeTab.SWIPE) },
                        icon = { Icon(Icons.Default.Swipe, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_swipe)) }
                    )
                }
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
                    } else if (uiState.filteredAlbums.isEmpty() && uiState.searchQuery.isNotEmpty()) {
                        // Aucun résultat de recherche
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(Modifier.height(8.dp))
                                Text(stringResource(R.string.home_no_results), color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    } else {
                        Crossfade(
                            targetState = uiState.isGridView,
                            animationSpec = tween(durationMillis = 500),
                            label = "LayoutSwitch"
                        ) { isGrid ->
                            if (isGrid) {
                                AlbumGrid(
                                    groupedAlbums = uiState.groupedAlbums,
                                    treatedCounts = uiState.albumTreatedCounts,
                                    pendingDeletes = uiState.albumPendingDeletes,
                                    isRefreshing = uiState.isRefreshing,
                                    onRefresh = { viewModel.refreshAlbums() },
                                    onAlbumClick = { viewModel.onAlbumSelected(it) }
                                )
                            } else {
                                AlbumList(
                                    groupedAlbums = uiState.groupedAlbums,
                                    treatedCounts = uiState.albumTreatedCounts,
                                    pendingDeletes = uiState.albumPendingDeletes,
                                    isRefreshing = uiState.isRefreshing,
                                    onRefresh = { viewModel.refreshAlbums() },
                                    onAlbumClick = { viewModel.onAlbumSelected(it) }
                                )
                            }
                        }
                    }
                }
                HomeTab.SWIPE -> {
                    if (uiState.selectedAlbum != null) {
                        SwipeScreen(
                            album = uiState.selectedAlbum!!,
                            assetRepository = assetRepository,
                            swipeDecisionRepository = swipeDecisionRepository,
                            sessionRepository = viewModel.getSessionRepository()
                        )
                    } else {
                        SwipePlaceholder(selectedAlbum = null)
                    }
                }
                HomeTab.SETTINGS -> {
                    val settingsViewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModelFactory(
                            viewModel.getSessionRepository(),
                            swipeDecisionRepository
                        )
                    )
                    SettingsScreen(
                        viewModel = settingsViewModel
                    )
                }
            }
        }
    }

    // Affichage de la fenêtre popup de profil
    if (uiState.showProfilePopup) {
        ProfilePopup(
            user = uiState.user,
            connectionStatus = uiState.connectionStatus,
            onClose = { viewModel.toggleProfilePopup(false) },
            onSettingsClick = { viewModel.onTabSelected(HomeTab.SETTINGS) },
            onLogout = { viewModel.logout() }
        )
    }
}

@Composable
fun ProfilePopup(
    user: com.minos2020.immichswipe.domain.model.User?,
    connectionStatus: com.minos2020.immichswipe.core.ConnectionStatus,
    onClose: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val baseUrl = SessionManager.getBaseUrl()?.removeSuffix("/")
    val apiKey = SessionManager.getApiKey() ?: ""

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header avec logo et bouton fermer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close))
                    }
                    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                    val logoRes = if (isDark) R.drawable.immichswipe_logo_colors_dark else R.drawable.immichswipe_logo_colors_light
                    
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = null,
                        modifier = Modifier.height(24.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(48.dp))
                }

                Spacer(Modifier.height(24.dp))

                // Photo de profil grande
                val profileModifier = Modifier
                    .size(100.dp)
                    .border(3.dp, Color(0xFF9C27B0), CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)

                if (user != null && baseUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("$baseUrl/api/users/${user.id}/profile-image")
                            .addHeader("x-api-key", apiKey)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.settings_section_account),
                        placeholder = rememberVectorPainter(Icons.Default.AccountCircle),
                        error = rememberVectorPainter(Icons.Default.AccountCircle),
                        modifier = profileModifier,
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = stringResource(R.string.settings_section_account),
                        modifier = profileModifier,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = user?.name ?: stringResource(R.string.home_user_fallback),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(Modifier.height(24.dp))

                // Diagnostic de connexion
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = connectionStatus.level.color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, connectionStatus.level.color.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(connectionStatus.level.color, CircleShape)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when(connectionStatus.level) {
                                    com.minos2020.immichswipe.core.ConnectionLevel.ONLINE -> stringResource(R.string.diag_online)
                                    com.minos2020.immichswipe.core.ConnectionLevel.ISSUES -> stringResource(R.string.diag_issues)
                                    com.minos2020.immichswipe.core.ConnectionLevel.OFFLINE -> stringResource(R.string.diag_offline)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = connectionStatus.level.color
                            )
                            val message = when(connectionStatus.type) {
                                com.minos2020.immichswipe.core.DiagStatus.CONNECTED -> stringResource(R.string.diag_connected_msg)
                                com.minos2020.immichswipe.core.DiagStatus.AUTH_ERROR -> stringResource(R.string.diag_error_auth)
                                com.minos2020.immichswipe.core.DiagStatus.UNAVAILABLE -> stringResource(R.string.diag_error_unavailable, connectionStatus.statusCode ?: 0)
                                com.minos2020.immichswipe.core.DiagStatus.UNEXPECTED -> stringResource(R.string.diag_error_unexpected, connectionStatus.statusCode ?: 0)
                                com.minos2020.immichswipe.core.DiagStatus.DNS_ERROR -> stringResource(R.string.diag_error_dns)
                                com.minos2020.immichswipe.core.DiagStatus.TIMEOUT -> stringResource(R.string.diag_error_timeout)
                                com.minos2020.immichswipe.core.DiagStatus.NO_INTERNET -> stringResource(R.string.diag_error_no_internet)
                                com.minos2020.immichswipe.core.DiagStatus.CONNECTION_ERROR -> stringResource(R.string.diag_error_connection)
                                com.minos2020.immichswipe.core.DiagStatus.LOGGED_OUT -> stringResource(R.string.diag_logged_out)
                                else -> connectionStatus.rawMessage ?: stringResource(R.string.diag_unknown)
                            }
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val hint = when(connectionStatus.type) {
                                com.minos2020.immichswipe.core.DiagStatus.AUTH_ERROR -> stringResource(R.string.diag_error_auth_hint)
                                com.minos2020.immichswipe.core.DiagStatus.UNAVAILABLE -> stringResource(R.string.diag_error_unavailable_hint)
                                com.minos2020.immichswipe.core.DiagStatus.DNS_ERROR -> stringResource(R.string.diag_error_dns_hint)
                                com.minos2020.immichswipe.core.DiagStatus.TIMEOUT -> stringResource(R.string.diag_error_timeout_hint)
                                com.minos2020.immichswipe.core.DiagStatus.NO_INTERNET -> stringResource(R.string.diag_error_no_internet_hint)
                                com.minos2020.immichswipe.core.DiagStatus.CONNECTION_ERROR -> connectionStatus.rawMessage
                                else -> null
                            }
                            if (hint != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = hint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Actions
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column {
                        PopupActionItem(
                            icon = Icons.Default.Settings,
                            text = stringResource(R.string.settings_title),
                            onClick = onSettingsClick
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        PopupActionItem(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            text = stringResource(R.string.profile_logout_button),
                            onClick = onLogout,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Lien Code Source
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW,
                                "https://github.com/Minos2020/immich-swipe".toUri())
                            context.startActivity(intent)
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.logo_immich_swipe),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.profile_source_code),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PopupActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@Composable
fun AlbumList(
    groupedAlbums: Map<AlbumStatus, List<Album>>,
    treatedCounts: Map<String, Int>,
    pendingDeletes: Map<String, Int>,
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
            // On définit l'ordre d'affichage des catégories
            val statusOrder = listOf(AlbumStatus.IN_PROGRESS, AlbumStatus.NOT_STARTED, AlbumStatus.COMPLETED)

            statusOrder.forEach { status ->
                val albumsInStatus = groupedAlbums[status]
                if (!albumsInStatus.isNullOrEmpty()) {
                    item(key = "header_${status.name}") {
                        val statusLabel = when(status) {
                            AlbumStatus.IN_PROGRESS -> stringResource(R.string.home_status_in_progress)
                            AlbumStatus.NOT_STARTED -> stringResource(R.string.home_status_not_started)
                            AlbumStatus.COMPLETED -> stringResource(R.string.home_status_completed)
                        }
                        Text(
                            text = statusLabel,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(albumsInStatus, key = { it.id }) { album ->
                        AlbumItem(
                            album = album,
                            treatedCount = treatedCounts[album.id] ?: 0,
                            pendingDeleteCount = pendingDeletes[album.id] ?: 0,
                            onClick = { onAlbumClick(album) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumGrid(
    groupedAlbums: Map<AlbumStatus, List<Album>>,
    treatedCounts: Map<String, Int>,
    pendingDeletes: Map<String, Int>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onAlbumClick: (Album) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val statusOrder = listOf(AlbumStatus.IN_PROGRESS, AlbumStatus.NOT_STARTED, AlbumStatus.COMPLETED)

            statusOrder.forEach { status ->
                val albumsInStatus = groupedAlbums[status]
                if (!albumsInStatus.isNullOrEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        val statusLabel = when(status) {
                            AlbumStatus.IN_PROGRESS -> stringResource(R.string.home_status_in_progress)
                            AlbumStatus.NOT_STARTED -> stringResource(R.string.home_status_not_started)
                            AlbumStatus.COMPLETED -> stringResource(R.string.home_status_completed)
                        }
                        Text(
                            text = statusLabel,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    gridItems(albumsInStatus, key = { it.id }) { album ->
                        AlbumGridItem(
                            album = album,
                            treatedCount = treatedCounts[album.id] ?: 0,
                            pendingDeleteCount = pendingDeletes[album.id] ?: 0,
                            onClick = { onAlbumClick(album) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumGridItem(album: Album, treatedCount: Int, pendingDeleteCount: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    val baseUrl = remember { SessionManager.getBaseUrl()?.removeSuffix("/") }
    val apiKey = remember { SessionManager.getApiKey() ?: "" }
    val progress = if (album.assetCount > 0) treatedCount.toFloat() / album.assetCount else 0f
    val isCompleted = album.assetCount in 1..treatedCount
    val isNotStarted = treatedCount == 0
    val hasUnsyncedChanges = pendingDeleteCount > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Image de fond
            if (album.albumThumbnailAssetId != null && baseUrl != null) {
                // On pré-construit la requête Coil de manière stable pour optimiser la fluidité du scroll
                val imageRequest = remember(album.albumThumbnailAssetId, baseUrl, apiKey) {
                    ImageRequest.Builder(context)
                        .data("$baseUrl/api/assets/${album.albumThumbnailAssetId}/thumbnail?format=WEBP")
                        .addHeader("x-api-key", apiKey)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                }

                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isCompleted) Modifier.alpha(0.8f) else Modifier)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            // Overlay dégradé pour le texte
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            // Badges d'état
            if (isCompleted) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    color = Color(0xFF388E3C),
                    shape = CircleShape,
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(4.dp).size(16.dp)
                    )
                }
            } else if (isNotStarted) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = RoundedCornerShape(4.dp),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = stringResource(R.string.home_new_badge),
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Contenu texte
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                if (hasUnsyncedChanges) {
                    Text(
                        text = stringResource(R.string.home_unsynced_badge),
                        color = Color(0xFFD32F2F),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Text(
                    text = album.albumName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.home_sorted_count, treatedCount, album.assetCount),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }

            // Barre de progression en haut de l'album (discrète)
            if (progress > 0 && !isCompleted) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@Composable
fun AlbumItem(album: Album, treatedCount: Int, pendingDeleteCount: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    val baseUrl = remember { SessionManager.getBaseUrl()?.removeSuffix("/") }
    val apiKey = remember { SessionManager.getApiKey() ?: "" }
    val progress = if (album.assetCount > 0) treatedCount.toFloat() / album.assetCount else 0f
    val isCompleted = album.assetCount in 1..treatedCount
    val isNotStarted = treatedCount == 0
    val hasUnsyncedChanges = pendingDeleteCount > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(60.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        if (album.albumThumbnailAssetId != null && baseUrl != null) {
                            val imageRequest = remember(album.albumThumbnailAssetId, baseUrl, apiKey) {
                                ImageRequest.Builder(context)
                                    .data("$baseUrl/api/assets/${album.albumThumbnailAssetId}/thumbnail?format=WEBP")
                                    .addHeader("x-api-key", apiKey)
                                    .crossfade(true)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build()
                            }

                            AsyncImage(
                                model = imageRequest,
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
                    
                    // Petit badge sur la miniature
                    if (isCompleted) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp),
                            color = Color(0xFF388E3C),
                            shape = CircleShape,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp).padding(2.dp))
                        }
                    } else if (isNotStarted) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(14.dp)
                                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = album.albumName, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                        if (isCompleted) {
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.home_album_completed), fontSize = 10.sp, color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (hasUnsyncedChanges) {
                        Text(
                            text = stringResource(R.string.home_unsynced_changes, pendingDeleteCount),
                            fontSize = 11.sp,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!album.description.isNullOrBlank()) {
                        Text(text = album.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline, maxLines = 2)
                    }
                    Text(
                        text = stringResource(R.string.home_sorted_count, treatedCount, album.assetCount),
                        fontSize = 12.sp,
                        color = if (isCompleted) Color(0xFF388E3C) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
            }

            // Barre de progression
            if (progress > 0 && !isCompleted) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            }
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
                Text(stringResource(R.string.home_session_title, selectedAlbum.albumName), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.home_photos_to_discover, selectedAlbum.assetCount), fontSize = 14.sp)
            } else {
                Text(stringResource(R.string.home_select_album))
            }
        }
    }
}

@Composable
fun ErrorView(error: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.home_error_title), color = MaterialTheme.colorScheme.error)
            Text(error, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text(stringResource(R.string.home_retry_button)) }
        }
    }
}
