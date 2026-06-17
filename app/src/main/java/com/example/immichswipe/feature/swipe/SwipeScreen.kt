package com.example.immichswipe.feature.swipe

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import com.example.immichswipe.data.repository.AssetRepository
import com.example.immichswipe.data.repository.SessionRepository
import com.example.immichswipe.domain.model.Album
import com.example.immichswipe.domain.model.Asset
import com.example.immichswipe.R
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector

private val MaterialGreen = Color(0xFF4CAF50)
private val MaterialRed = Color(0xFFE57373)

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

                // On affiche les 2 assets (courant et suivant) dans une seule boucle
                // On inverse l'ordre (reversed) pour que l'asset courant (index plus petit)
                // soit dessiné en dernier, donc au dessus dans la Box.
                val visibleIndices = (currentIndex until (currentIndex + 2).coerceAtMost(assets.size)).toList().reversed()

                visibleIndices.forEach { index ->
                    val asset = assets[index]
                    val isNextCard = index > currentIndex
                    key(asset.id) {
                        SwipeCard(
                            asset = asset,
                            onSwipe = { viewModel.onSwipe(it) },
                            isNext = isNextCard,
                            playbackBehavior = uiState.playbackBehavior
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
    playbackBehavior: PlaybackBehavior
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
                                    onSwipe(SwipeDecision.KEEP)
                                } else if (currentX < -250) {
                                    offsetX.animateTo(-1500f, tween(150))
                                    onSwipe(SwipeDecision.DELETE)
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
                    IconButton(
                        onClick = { isFullscreenOpen = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(Icons.Default.Fullscreen, null, tint = Color.White)
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

                    if (keepAlpha > 0f) {
                        IndicatorBadge("KEEP", MaterialGreen, Alignment.TopStart, keepAlpha * 0.9f)
                    } else if (deleteAlpha > 0f) {
                        IndicatorBadge("DELETE", MaterialRed, Alignment.TopEnd, deleteAlpha * 0.9f)
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
            color = Color.Transparent, contentColor = color, shape = RoundedCornerShape(8.dp),
            border = BorderStroke(2.dp, color.copy(alpha = 0.6f))
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
