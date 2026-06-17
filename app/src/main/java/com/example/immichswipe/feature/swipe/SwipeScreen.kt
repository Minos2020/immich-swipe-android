package com.example.immichswipe.feature.swipe

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.immichswipe.core.SessionManager
import com.example.immichswipe.core.PlaybackBehavior
import com.example.immichswipe.core.IconPosition
import com.example.immichswipe.data.repository.AssetRepository
import com.example.immichswipe.data.repository.SessionRepository
import com.example.immichswipe.domain.model.Album
import com.example.immichswipe.domain.model.Asset
import com.example.immichswipe.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector

private val MaterialGreen = Color(0xFF2E7D32) // Un vert plus profond (Green 800)
private val MaterialRed = Color(0xFFC62828)   // Un rouge plus marqué (Red 800)

/**
 * Helper pour trouver l'Activity à partir du Context.
 */
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun SwipeScreen(
    album: Album,
    assetRepository: AssetRepository,
    sessionRepository: SessionRepository,
    modifier: Modifier = Modifier
) {
    val viewModel: SwipeViewModel = viewModel(
        key = album.id,
        factory = SwipeViewModelFactory(assetRepository, sessionRepository, album)
    )
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 0. En-tête avec nom de l'album et statistiques
        SwipeHeader(uiState = uiState)

        // 1. Timeline (Barre du haut avec vignettes)
        AssetTimeline(
            assets = uiState.assets,
            decisions = uiState.decisions,
            currentIndex = uiState.currentIndex,
            onAssetClick = { viewModel.onMoveToAsset(it) }
        )

        // 2. Zone centrale : La pile de cartes
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.error != null) {
                Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
            } else if (uiState.currentIndex < uiState.assets.size) {
                val currentIndex = uiState.currentIndex
                val assets = uiState.assets

                // On affiche les 2 assets (courant et prochain non traité)
                // On inverse l'ordre (reversed) pour que l'asset courant soit au dessus.
                val nextUnprocessedIndex = viewModel.getNextUnprocessedIndex()
                val visibleIndices = listOfNotNull(
                    currentIndex,
                    nextUnprocessedIndex.takeIf { it != -1 }
                ).distinct().reversed()

                visibleIndices.forEach { index ->
                    val asset = assets[index]
                    val isNextCard = index > currentIndex
                    key(asset.id) {
                        SwipeCard(
                            asset = asset,
                            onSwipe = { viewModel.onSwipe(it) },
                            isNext = isNextCard,
                            playbackBehavior = uiState.playbackBehavior,
                            isSwipeInverted = uiState.isSwipeInverted,
                            fullscreenButtonPosition = uiState.fullscreenButtonPosition,
                            immichButtonPosition = uiState.immichButtonPosition
                        )
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Celebration,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(text = "Félicitations ! Album trié.", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. Barre d'actions en bas
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = { viewModel.onSwipe(SwipeDecision.DELETE) },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer")
            }

            IconButton(
                onClick = { viewModel.undo() },
                enabled = uiState.currentIndex > 0
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Annuler")
            }

            IconButton(onClick = { viewModel.onSwipe(SwipeDecision.SKIP) }) {
                Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = "Passer")
            }

            FloatingActionButton(
                onClick = { viewModel.onSwipe(SwipeDecision.KEEP) },
                containerColor = MaterialGreen,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Check, contentDescription = "Garder")
            }
        }
    }
}

@Composable
fun SwipeHeader(uiState: SwipeUiState) {
    // Animation fluide de la progression
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "ProgressBarAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Barre de progression avec nom de l'album
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.CenterStart
        ) {
            // Remplissage de la progression (avec valeur animée)
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                            )
                        )
                    )
                    // On ajoute une légère couche sombre pour faire ressortir le texte blanc
                    .background(Color.Black.copy(alpha = 0.1f))
            )
            
            // Texte de l'album (en blanc ou noir selon le contraste)
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = uiState.albumName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "${(uiState.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Rangée de statistiques
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatBadge(label = "Gardés", count = uiState.keptCount, color = MaterialGreen)
            StatBadge(label = "Suppr.", count = uiState.deletedCount, color = MaterialRed)
            StatBadge(label = "Passés", count = uiState.skippedCount, color = Color.Gray)
            StatBadge(label = "Restant", count = uiState.remainingCount, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun StatBadge(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun AssetTimeline(
    assets: List<Asset>,
    decisions: Map<String, SwipeDecision>,
    currentIndex: Int,
    onAssetClick: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        if (assets.isNotEmpty()) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(assets) { index, asset ->
            val decision = decisions[asset.id]
            val isCurrent = index == currentIndex

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = if (isCurrent) 2.dp else 0.dp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onAssetClick(index) }
            ) {
                val baseUrl = SessionManager.getBaseUrl()?.removeSuffix("/")
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=WEBP")
                        .addHeader("x-api-key", SessionManager.getApiKey() ?: "")
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(if (isCurrent) 1f else 0.6f)
                )

                // Icône "Play" pour les vidéos dans la timeline
                if (asset.type == "VIDEO") {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(2.dp)
                            .size(14.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    )
                }

                if (decision != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                when (decision) {
                                    SwipeDecision.KEEP -> MaterialGreen
                                    SwipeDecision.DELETE -> MaterialRed
                                    SwipeDecision.SKIP -> Color.Gray
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (decision) {
                                SwipeDecision.KEEP -> Icons.Default.Check
                                SwipeDecision.DELETE -> Icons.Default.Delete
                                SwipeDecision.SKIP -> Icons.AutoMirrored.Filled.Forward
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun SwipeCard(
    asset: Asset,
    onSwipe: (SwipeDecision) -> Unit,
    isNext: Boolean,
    playbackBehavior: PlaybackBehavior,
    isSwipeInverted: Boolean,
    fullscreenButtonPosition: IconPosition,
    immichButtonPosition: IconPosition
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val baseUrl = SessionManager.getBaseUrl()?.removeSuffix("/")
    val apiKey = SessionManager.getApiKey() ?: ""
    val lifecycleOwner = LocalLifecycleOwner.current

    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    var isFullscreenOpen by rememberSaveable { mutableStateOf(false) }
    var isVideoReady by remember(asset.id) { mutableStateOf(false) }
    var showLoadingIndicator by remember(asset.id) { mutableStateOf(false) }

    // On affiche l'indicateur de chargement seulement après un court délai (ex: 400ms)
    // pour éviter les clignotements si la vidéo est déjà prête ou charge instantanément.
    LaunchedEffect(asset.id, isVideoReady) {
        if (!isVideoReady) {
            delay(500)
            showLoadingIndicator = true
        } else {
            showLoadingIndicator = false
        }
    }

    // Hauteur du panneau de métadonnées
    val metadataHeight = 300.dp
    val metadataHeightPx = with(density) { metadataHeight.toPx() }

    // On crée un Player unique pour cette carte si c'est une vidéo
    // On ajoute isNext dans les clés du remember pour que le player soit créé
    // au moment où la carte passe du second plan au premier plan.
    val exoPlayer = remember(asset.id, isNext) {
        if (asset.type == "VIDEO" && !isNext) {
            ExoPlayer.Builder(context).build().apply {
                // Configuration précise de l'Audio Focus
                if (playbackBehavior != PlaybackBehavior.IGNORE) {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build()
                    
                    // On active la gestion automatique du focus (coupe les autres sons)
                    setAudioAttributes(audioAttributes, true)
                }

                repeatMode = Player.REPEAT_MODE_ONE
                val videoUrl = "$baseUrl/api/assets/${asset.id}/video/playback"
                val dataSourceFactory = DefaultHttpDataSource.Factory()
                    .setDefaultRequestProperties(mapOf("x-api-key" to apiKey))
                val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(videoUrl))
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
            }
        } else null
    }

    // Gestion du cycle de vie pour mettre en pause la vidéo quand on quitte l'app
    DisposableEffect(exoPlayer, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isVideoReady = state == Player.STATE_READY
            }
        }
        
        // Initialiser l'état immédiatement si le player est déjà prêt
        if (exoPlayer?.playbackState == Player.STATE_READY) {
            isVideoReady = true
        }

        exoPlayer?.addListener(listener)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer?.playWhenReady = false
                }
                Lifecycle.Event.ON_RESUME -> {
                    // On ne relance que si on n'est pas en plein écran (pour éviter les conflits)
                    if (!isFullscreenOpen) {
                        exoPlayer?.playWhenReady = true
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer?.removeListener(listener)
            exoPlayer?.stop()
            exoPlayer?.release()
        }
    }

    // Animation de grossissement de la carte suivante (vitesse moyenne 500ms)
    val animatedScale by animateFloatAsState(
        targetValue = if (isNext) 0.85f else 1f,
        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
        label = "ScaleAnimation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isNext) 0.6f else 1f)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    if (!isNext) {
                        translationX = offsetX.value
                        // Photo reste fixe verticalement (offsetY utilisé pour les métadonnées)
                        rotationZ = offsetX.value / 40f
                    }
                }
                .pointerInput(isNext) {
                    if (isNext) return@pointerInput
                    detectDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val currentX = offsetX.value
                                val currentY = offsetY.value

                                if (currentX > 250) {
                                    offsetX.animateTo(1500f, tween(150))
                                    onSwipe(if (isSwipeInverted) SwipeDecision.DELETE else SwipeDecision.KEEP)
                                } else if (currentX < -250) {
                                    offsetX.animateTo(-1500f, tween(150))
                                    onSwipe(if (isSwipeInverted) SwipeDecision.KEEP else SwipeDecision.DELETE)
                                } else {
                                    launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }

                                    // Aimantage métadonnées :
                                    // Si on est en train d'ouvrir (Y < 0) et qu'on dépasse 25% de la hauteur -> on finit l'ouverture
                                    // Gestion du "cran" pour les métadonnées (Y)
                                    val wasOpen = offsetY.targetValue < -metadataHeightPx / 2
                                    if (wasOpen) {
                                        // Si c'était ouvert, on ferme au moindre geste vers le bas (seuil 90% de hauteur)
                                        if (currentY < -metadataHeightPx * 0.95f) {
                                            offsetY.animateTo(-metadataHeightPx, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                        } else {
                                            offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                        }
                                    } else {
                                        // Si c'était fermé, on ouvre si on dépasse 10% de la hauteur vers le haut
                                        if (currentY < -metadataHeightPx * 0.05f) {
                                            offsetY.animateTo(-metadataHeightPx, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                        } else {
                                            offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                        }
                                    }
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                                offsetY.snapTo((offsetY.value + dragAmount.y).coerceIn(-metadataHeightPx, 0f))
                            }
                        }
                    )
                },
            elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 0.dp else 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) {
                if (asset.type == "VIDEO" && !isNext && exoPlayer != null) {
                    // On ne garde qu'un seul PlayerView actif à la fois pour le même player
                    // Si le plein écran est ouvert, on cache celui de la carte pour qu'il puisse
                    // être ré-attaché proprement au retour.
                    if (!isFullscreenOpen) {
                        SharedVideoPlayer(
                            player = exoPlayer,
                            isFullscreen = false,
                            onDoubleTap = { isFullscreenOpen = true }
                        )

                        // Indicateur de chargement si la vidéo n'est pas prête
                        if (showLoadingIndicator) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    } else {
                        // Image de remplacement pendant que le player est utilisé en plein écran
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=JPEG&size=preview")
                                .addHeader("x-api-key", apiKey)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=JPEG&size=preview")
                            .addHeader("x-api-key", apiKey)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (!isNext) {
                                    Modifier.pointerInput(Unit) {
                                        detectTapGestures(onDoubleTap = { isFullscreenOpen = true })
                                    }
                                } else Modifier
                            )
                    )
                }

                // Bouton Plein Écran
                if (!isNext) {
                    // Zone pour les boutons d'action (Plein écran et Immich)
                    // Si les deux sont au même endroit, on les met dans une Column
                    val samePosition = fullscreenButtonPosition == immichButtonPosition
                    
                    if (samePosition) {
                        val stackAlignment = when (fullscreenButtonPosition) {
                            IconPosition.TOP_LEFT -> Alignment.TopStart
                            IconPosition.TOP_RIGHT -> Alignment.TopEnd
                            IconPosition.BOTTOM_LEFT -> Alignment.BottomStart
                            IconPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                        }
                        
                        Column(
                            modifier = Modifier
                                .align(stackAlignment)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = when (fullscreenButtonPosition) {
                                IconPosition.TOP_LEFT, IconPosition.BOTTOM_LEFT -> Alignment.Start
                                else -> Alignment.End
                            }
                        ) {
                            IconButton(
                                onClick = { isFullscreenOpen = true },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(Icons.Default.Fullscreen, null, tint = Color.White)
                            }
                            
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, "$baseUrl/photos/${asset.id}".toUri())
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Ouvrir dans Immich",
                                    tint = Color.White
                                )
                            }
                        }
                    } else {
                        // Positions différentes, comportement standard
                        val fullscreenAlignment = when (fullscreenButtonPosition) {
                            IconPosition.TOP_LEFT -> Alignment.TopStart
                            IconPosition.TOP_RIGHT -> Alignment.TopEnd
                            IconPosition.BOTTOM_LEFT -> Alignment.BottomStart
                            IconPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                        }
                        
                        IconButton(
                            onClick = { isFullscreenOpen = true },
                            modifier = Modifier
                                .align(fullscreenAlignment)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(Icons.Default.Fullscreen, null, tint = Color.White)
                        }

                        val immichButtonAlignment = when (immichButtonPosition) {
                            IconPosition.TOP_LEFT -> Alignment.TopStart
                            IconPosition.TOP_RIGHT -> Alignment.TopEnd
                            IconPosition.BOTTOM_LEFT -> Alignment.BottomStart
                            IconPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                        }

                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, "$baseUrl/photos/${asset.id}".toUri())
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .align(immichButtonAlignment)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Ouvrir dans Immich",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Dégradé pour le texte des métadonnées
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))))
                )

                // Panneau Métadonnées Interactif (Intégré dans la carte)
                if (!isNext) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(metadataHeight)
                            .graphicsLayer { translationY = metadataHeight.toPx() + offsetY.value }
                    ) {
                        MetadataPanel(asset = asset, onClose = { scope.launch { offsetY.animateTo(0f) } })
                    }
                }

                if (!isNext) {
                    val keepAlpha = (offsetX.value / 200f).coerceIn(0f, 1f)
                    val deleteAlpha = (-offsetX.value / 200f).coerceIn(0f, 1f)

                    if (isSwipeInverted) {
                        // Inversion des badges
                        if (keepAlpha > 0f) {
                            IndicatorBadge("DELETE", MaterialRed, Alignment.TopStart, keepAlpha * 0.9f)
                        } else if (deleteAlpha > 0f) {
                            IndicatorBadge("KEEP", MaterialGreen, Alignment.TopEnd, deleteAlpha * 0.9f)
                        }
                    } else {
                        // Comportement normal
                        if (keepAlpha > 0f) {
                            IndicatorBadge("KEEP", MaterialGreen, Alignment.TopStart, keepAlpha * 0.9f)
                        } else if (deleteAlpha > 0f) {
                            IndicatorBadge("DELETE", MaterialRed, Alignment.TopEnd, deleteAlpha * 0.9f)
                        }
                    }
                }
            }
        }
    }

    if (isFullscreenOpen) {
        FullscreenViewer(
            asset = asset,
            player = exoPlayer,
            onClose = { isFullscreenOpen = false }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun SharedVideoPlayer(
    player: Player,
    isFullscreen: Boolean,
    onDoubleTap: (() -> Unit)? = null
) {
    AndroidView(
        factory = { context ->
            // On utilise un layout XML pour pouvoir spécifier le surface_type="texture_view"
            // car le setter setSurfaceType n'est pas public dans Media3 PlayerView.
            val view = LayoutInflater.from(context).inflate(R.layout.view_player_texture, null) as PlayerView
            view
        },
        update = { view ->
            // Re-attache le player si nécessaire
            if (view.player != player) view.player = player
            view.useController = isFullscreen

            // TextureView est indispensable pour une intégration parfaite avec les couches Compose
            // Cela évite les écrans noirs au retour du plein écran.
            view.resizeMode = if (isFullscreen) {
                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            } else {
                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }

            // S'assure que la lecture reprend bien
            if (player.playbackState == Player.STATE_READY && !player.isPlaying) {
                player.play()
            }
        },
        onRelease = { view ->
            // Détache proprement le player de la vue avant destruction
            view.player = null
        },
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (onDoubleTap != null) {
                    Modifier.pointerInput(onDoubleTap) {
                        detectTapGestures(onDoubleTap = { onDoubleTap() })
                    }
                } else Modifier
            )
    )
}

@Composable
fun FullscreenViewer(
    asset: Asset,
    player: Player?,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val swipeY = remember { Animatable(0f) }

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (1f - (swipeY.value / 1000f)).coerceIn(0f, 1f)))
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { onClose() })
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (swipeY.value > 120) onClose()
                            else scope.launch { swipeY.animateTo(0f) }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch { swipeY.snapTo((swipeY.value + dragAmount.y).coerceAtLeast(0f)) }
                        }
                    )
                }
                .offset { IntOffset(0, swipeY.value.roundToInt()) },
            contentAlignment = Alignment.Center
        ) {
            if (asset.type == "VIDEO" && player != null) {
                SharedVideoPlayer(player = player, isFullscreen = true)
            } else {
                val baseUrlClean = SessionManager.getBaseUrl()?.removeSuffix("/")
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("$baseUrlClean/api/assets/${asset.id}/thumbnail?format=JPEG&size=preview")
                        .addHeader("x-api-key", SessionManager.getApiKey() ?: "")
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(vertical = 50.dp, horizontal = 20.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
        }
    }
}

@Composable
fun IndicatorBadge(text: String, color: Color, align: Alignment, alpha: Float) {
    Box(
        modifier = Modifier
            .padding(40.dp)
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha },
        contentAlignment = align
    ) {
        Surface(
            color = color.copy(alpha = 0.05f), // Fond très léger pour le contraste
            contentColor = color, 
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(2.dp, color.copy(alpha = 0.9f))
        ) {
            Text(
                text = text, fontSize = 32.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    .graphicsLayer { rotationZ = if (align == Alignment.TopStart) -15f else 15f }
            )
        }
    }
}

@Composable
fun MetadataPanel(asset: Asset, onClose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Box(modifier = Modifier.size(40.dp, 4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Détails de l'image", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp)) }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            MetadataRow(Icons.Default.Description, "Fichier", asset.originalFileName ?: "Inconnu")
            MetadataRow(Icons.Default.CalendarToday, "Date", asset.fileCreatedAt.substringBefore("T"))
            val formatLabel = if (asset.fileExtension != null) "${asset.type} (.${asset.fileExtension.lowercase()})" else asset.type
            MetadataRow(Icons.Default.Info, "Format", formatLabel)
            asset.exifInfo?.let { exif ->
                val sizeMb = exif.fileSizeInBytes?.let { String.format(Locale.getDefault(), "%.2f Mo", it / 1024.0 / 1024.0) } ?: "N/A"
                MetadataRow(Icons.Default.SdStorage, "Taille", sizeMb)
                MetadataRow(Icons.Default.AspectRatio, "Résolution", "${exif.imageWidth ?: "?"} x ${exif.imageHeight ?: "?"}")
            } ?: run {
                MetadataRow(Icons.Default.SdStorage, "Taille", "Chargement...")
                MetadataRow(Icons.Default.AspectRatio, "Résolution", "Chargement...")
            }
        }
    }
}

@Composable
fun MetadataRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.width(80.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
