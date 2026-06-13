package com.example.immichswipe.feature.swipe

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.Alignment
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.immichswipe.core.SessionManager
import com.example.immichswipe.data.repository.AssetRepository
import com.example.immichswipe.domain.model.Album
import com.example.immichswipe.domain.model.Asset
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.foundation.BorderStroke

import androidx.compose.runtime.saveable.rememberSaveable

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
    modifier: Modifier = Modifier
) {
    val viewModel: SwipeViewModel = viewModel(
        key = album.id,
        factory = SwipeViewModelFactory(assetRepository, album)
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
                
                // On affiche l'asset suivant en arrière-plan pour l'effet de pile
                val nextAsset = uiState.assets.getOrNull(uiState.currentIndex + 1)
                if (nextAsset != null) {
                    key(nextAsset.id) {
                        SwipeCard(
                            asset = nextAsset,
                            onSwipe = {},
                            isNext = true
                        )
                    }
                }

                // On affiche l'asset courant
                val currentAsset = uiState.assets[uiState.currentIndex]
                key(currentAsset.id) {
                    SwipeCard(
                        asset = currentAsset,
                        onSwipe = { viewModel.onSwipe(it) },
                        isNext = false
                    )
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

@Composable
fun SwipeCard(
    asset: Asset,
    onSwipe: (SwipeDecision) -> Unit,
    isNext: Boolean
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val baseUrl = SessionManager.getBaseUrl()?.removeSuffix("/")
    val apiKey = SessionManager.getApiKey() ?: ""

    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    var isFullscreenOpen by rememberSaveable { mutableStateOf(false) }

    // Hauteur du panneau de métadonnées
    val metadataHeight = 300.dp
    val metadataHeightPx = with(density) { metadataHeight.toPx() }

    // Animation de grossissement de la carte suivante (plus lente et fluide)
    val animatedScale by animateFloatAsState(
        targetValue = if (isNext) 0.85f else 1f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
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
                        // La photo reste fixe verticalement (offsetY utilisé pour les métadonnées)
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
                                
                                if (currentX > 400) {
                                    // Swipe rapide vers la droite (KEEP)
                                    offsetX.animateTo(1500f, animationSpec = tween(250))
                                    onSwipe(SwipeDecision.KEEP)
                                } else if (currentX < -400) {
                                    // Swipe rapide vers la gauche (DELETE)
                                    offsetX.animateTo(-1500f, animationSpec = tween(250))
                                    onSwipe(SwipeDecision.DELETE)
                                } else {
                                    // Si on n'a pas swipé assez loin horizontalement, on revient TOUJOURS au centre X
                                    launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
                                    
                                    // Gestion du "cran" pour les métadonnées (Y)
                                    if (currentY < -metadataHeightPx / 4) {
                                        // On aimante le panneau en position ouverte
                                        offsetY.animateTo(-metadataHeightPx, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    } else {
                                        // On referme le panneau
                                        offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    }
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch { 
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                                // On permet de monter (valeurs négatives) mais pas de descendre en dessous de 0
                                // On bride aussi la montée à la hauteur exacte du panneau pour qu'il reste ancré
                                val newY = (offsetY.value + dragAmount.y).coerceIn(-metadataHeightPx, 0f)
                                offsetY.snapTo(newY)
                            }
                        }
                    )
                },
            elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 0.dp else 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) {
                if (asset.type == "VIDEO" && !isNext) {
                    VideoPlayer(assetId = asset.id, baseUrl = baseUrl, apiKey = apiKey)
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=JPEG&size=preview")
                            .addHeader("x-api-key", apiKey)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
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
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                            )
                        )
                )

                // Panneau Métadonnées Interactif (Intégré dans la carte)
                if (!isNext) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(metadataHeight)
                            .graphicsLayer {
                                // Le panneau émerge du bas de la carte
                                // translationY = hauteur du panneau (caché) + mouvement vertical
                                translationY = metadataHeight.toPx() + offsetY.value
                            }
                    ) {
                        MetadataPanel(asset = asset, onClose = { 
                            scope.launch { offsetY.animateTo(0f) }
                        })
                    }
                }

                // Indicateurs Visuels Swipe
                if (offsetX.value > 150) {
                    IndicatorBadge(text = "KEEP", color = MaterialGreen, align = Alignment.TopStart)
                } else if (offsetX.value < -150) {
                    IndicatorBadge(text = "DELETE", color = MaterialRed, align = Alignment.TopEnd)
                }
            }
        }
    }

    if (isFullscreenOpen) {
        FullscreenViewer(
            asset = asset,
            baseUrl = baseUrl,
            apiKey = apiKey,
            onClose = { isFullscreenOpen = false }
        )
    }
}

@Composable
fun FullscreenViewer(
    asset: Asset,
    baseUrl: String?,
    apiKey: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val swipeY = remember { Animatable(0f) }
    
    // On déverrouille l'orientation quand on entre en plein écran
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        onDispose {
            // On revient en portrait quand on ferme le plein écran
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false // Pour vraiment utiliser tout l'écran
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (1f - (swipeY.value / 1000f)).coerceIn(0f, 1f)))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (swipeY.value > 300) {
                                onClose()
                            } else {
                                scope.launch { swipeY.animateTo(0f) }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // On ne permet que le swipe vers le bas (swipeY > 0)
                            scope.launch { swipeY.snapTo((swipeY.value + dragAmount.y).coerceAtLeast(0f)) }
                        }
                    )
                }
                .offset { IntOffset(0, swipeY.value.roundToInt()) },
            contentAlignment = Alignment.Center
        ) {
            if (asset.type == "VIDEO") {
                VideoPlayer(assetId = asset.id, baseUrl = baseUrl, apiKey = apiKey)
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=JPEG&size=preview")
                        .addHeader("x-api-key", apiKey)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
        }
    }
}

@Composable
fun IndicatorBadge(text: String, color: Color, align: Alignment) {
    Box(
        modifier = Modifier
            .padding(40.dp)
            .fillMaxSize(),
        contentAlignment = align
    ) {
        Surface(
            color = Color.Transparent,
            contentColor = color,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(2.dp, color.copy(alpha = 0.6f))
        ) {
            Text(
                text = text,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .graphicsLayer { rotationZ = if (align == Alignment.TopStart) -15f else 15f }
                    .alpha(0.8f)
            )
        }
    }
}

@Composable
fun MetadataPanel(asset: Asset, onClose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            // Petite barre de tirage (Handle) pour le look
            Box(
                modifier = Modifier
                    .size(40.dp, 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .align(Alignment.CenterHorizontally)
            )
            
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Détails de l'image",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) { 
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp)) 
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            MetadataRow(Icons.Default.Description, "Fichier", asset.originalFileName ?: "Inconnu")
            MetadataRow(Icons.Default.CalendarToday, "Date", asset.fileCreatedAt.substringBefore("T"))
            
            // Format plus précis avec l'extension réelle
            val formatLabel = if (asset.fileExtension != null) {
                "${asset.type} (.${asset.fileExtension.lowercase()})"
            } else {
                asset.type
            }
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
fun MetadataRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(assetId: String, baseUrl: String?, apiKey: String) {
    if (baseUrl == null) return

    val context = LocalContext.current
    var isReady by remember { mutableStateOf(false) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    isReady = state == Player.STATE_READY
                }
            })
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(assetId, baseUrl, apiKey) {
        isReady = false
        val videoUrl = "$baseUrl/api/assets/$assetId/video/playback"
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf("x-api-key" to apiKey))
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUrl))
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!isReady) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("$baseUrl/api/assets/$assetId/thumbnail?format=JPEG&size=preview")
                    .addHeader("x-api-key", apiKey)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
        }

        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize().alpha(if (isReady) 1f else 0f)
        )
    }
}
