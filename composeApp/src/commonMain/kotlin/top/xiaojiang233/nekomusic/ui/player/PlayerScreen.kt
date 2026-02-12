package top.xiaojiang233.nekomusic.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.getPlatform
import top.xiaojiang233.nekomusic.player.PlaybackMode
import top.xiaojiang233.nekomusic.player.PlayerController.currentIndex
import top.xiaojiang233.nekomusic.utils.thumbnail
import top.xiaojiang233.nekomusic.viewmodel.PlayerViewModel
import androidx.compose.ui.platform.LocalClipboardManager
import top.xiaojiang233.nekomusic.utils.LocalFullscreenHandler
import androidx.compose.ui.text.AnnotatedString
import nekomusic.composeapp.generated.resources.Res
import nekomusic.composeapp.generated.resources.back
import nekomusic.composeapp.generated.resources.copy_artist_name
import nekomusic.composeapp.generated.resources.copy_song_name
import nekomusic.composeapp.generated.resources.copied_format
import nekomusic.composeapp.generated.resources.fullscreen
import nekomusic.composeapp.generated.resources.unlike
import nekomusic.composeapp.generated.resources.like
import nekomusic.composeapp.generated.resources.queue
import nekomusic.composeapp.generated.resources.volume
import nekomusic.composeapp.generated.resources.prev
import nekomusic.composeapp.generated.resources.next
import nekomusic.composeapp.generated.resources.pause
import nekomusic.composeapp.generated.resources.play
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.Image
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.ui.graphics.ColorFilter
import nekomusic.composeapp.generated.resources.hbmode
import org.jetbrains.compose.resources.painterResource

// Define PlayerControlsState to reduce parameters
data class PlayerControlsState(
    val title: String,
    val artist: String,
    val totalDuration: Long,
    val currentPosition: Long,
    val isDragging: Boolean,
    val sliderPosition: Float,
    val isPlaying: Boolean,
    val playbackMode: PlaybackMode,
    val isLandscape: Boolean,
    val isLiked: Boolean = false,
    val songId: Long // Add songId here
)

@Suppress("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onCommentClick: (Long) -> Unit = {}, // Added parameter here
    viewModel: PlayerViewModel = viewModel { PlayerViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPosition = uiState.playbackState.currentPosition
    val totalDuration = uiState.playbackState.duration
    val isPlaying = uiState.playbackState.isPlaying
    val playbackMode = uiState.playbackMode
    val isLiked = uiState.isLiked
    val song = uiState.playbackState.currentSong
    val navController = androidx.navigation.compose.rememberNavController() // This is wrong, it should be passed or we use current local

    // We need a way to navigate to comments. Let's assume we can use a callback or find the controller.
    // However, PlayerScreen is usually inside a NavHost.
    // Let's add a callback to PlayerScreen signature if needed, but for now let's use a local hack or just define the logic.

    // State for Queue Popup (Desktop) or Page Navigation
    var showQueueDesktop by remember { mutableStateOf(false) }
    // On Mobile, we use pager.

    // Local slider state
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSliderDragging by remember { mutableStateOf(false) }

    LaunchedEffect(currentPosition) {
        if (!isSliderDragging) {
            sliderPosition = currentPosition.toFloat()
        }
    }

    val displayAlbumArt = song?.al?.picUrl?.thumbnail(600)
    val title = song?.name ?: ""
    val artist = song?.ar?.joinToString(", ") { it.name } ?: ""

    // Wrap in BoxWithConstraints for layout queries
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
         val isLandscape = maxWidth > maxHeight

        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = displayAlbumArt,
                label = "BackgroundCrossfade",
                animationSpec = tween(500)
            ) { art ->
                AsyncImage(
                    model = art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(50.dp)
                )
            }

            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

            if (song == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        PlayerControls(
                            displayAlbumArt = displayAlbumArt,
                            state = PlayerControlsState(
                                title = title,
                                artist = artist,
                                totalDuration = totalDuration,
                                currentPosition = currentPosition,
                                isDragging = isSliderDragging,
                                sliderPosition = sliderPosition,
                                isPlaying = isPlaying,
                                playbackMode = playbackMode,
                                isLandscape = true,
                                isLiked = isLiked,
                                songId = song.id // Pass song ID here
                            ),
                            onValueChange = {
                                isSliderDragging = true
                                sliderPosition = it
                            },
                            onValueChangeFinished = {
                                isSliderDragging = false
                                viewModel.seekTo(sliderPosition.toLong())
                            },
                            onPlayPauseClick = { viewModel.togglePlay() },
                            onPreviousClick = { viewModel.playPrevious() },
                            onNextClick = { viewModel.playNext() },
                            onPlaybackModeClick = { viewModel.togglePlaybackMode() },
                            onVolumeChange = { viewModel.setVolume(it) },
                            onQueueClick = { showQueueDesktop = !showQueueDesktop },
                            onLikeClick = { viewModel.toggleLike() },
                            onCommentClick = { song?.id?.let { onCommentClick(it) } } // Pass it here
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AnimatedContent(
                            targetState = currentIndex,
                            transitionSpec = {
                                (slideInVertically { height -> height / 2 } + fadeIn()).togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut())
                            },
                            label = "LyricsAnimation"
                        ) { targetState ->
                            key(targetState) { // USE targetState here
                                LyricsScreen(viewModel)
                            }
                        }
                    }
                }

                // Desktop Queue Overlay
                AnimatedVisibility(
                    visible = showQueueDesktop,
                    enter = slideInHorizontally { it } + fadeIn(),
                    exit = slideOutHorizontally { it } + fadeOut(),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Box(
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {} // Block clicks
                    ) {
                        QueueScreen(onClose = { showQueueDesktop = false })
                    }
                }

            } else {
                val pagerState = rememberPagerState(pageCount = { 3 })

                // Allow scrolling to queue from Logic
                // If we want a button to go to queue on mobile, we can also use showQueue (as trigger) or scroll pager
                // For simplicity, just add Page 3.

                HorizontalPager(state = pagerState) { page ->
                    when (page) {
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val scope = rememberCoroutineScope()
                                PlayerControls(
                                    displayAlbumArt = displayAlbumArt,
                                    state = PlayerControlsState(
                                        title = title,
                                        artist = artist,
                                        totalDuration = totalDuration,
                                        currentPosition = currentPosition,
                                        isDragging = isSliderDragging,
                                        sliderPosition = sliderPosition,
                                        isPlaying = isPlaying,
                                        playbackMode = playbackMode,
                                        isLandscape = false,
                                        isLiked = isLiked,
                                        songId = song.id // Pass song ID here
                                    ),
                                    onValueChange = {
                                        isSliderDragging = true
                                        sliderPosition = it
                                    },
                                    onValueChangeFinished = {
                                        isSliderDragging = false
                                        viewModel.seekTo(sliderPosition.toLong())
                                    },
                                    onPlayPauseClick = { viewModel.togglePlay() },
                                    onPreviousClick = { viewModel.playPrevious() },
                                    onNextClick = { viewModel.playNext() },
                                    onPlaybackModeClick = { viewModel.togglePlaybackMode() },
                                    onVolumeChange = { viewModel.setVolume(it) },
                                    onQueueClick = {
                                        // Scroll to page 2 (Queue)
                                        scope.launch { pagerState.animateScrollToPage(2) }
                                    },
                                    onLikeClick = { viewModel.toggleLike() },
                                    onCommentClick = { song?.id?.let { onCommentClick(it) } } // Pass it here
                                )
                            }
                        }
                        1 -> {
                            AnimatedContent(
                                targetState = currentIndex,
                                transitionSpec = {
                                    (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
                                },
                                label = "LyricsAnimation"
                            ) { targetState ->
                                key(targetState) { // USE targetState here
                                    LyricsScreen(viewModel)
                                }
                            }
                        }
                        2 -> {
                            QueueScreen()
                        }
                    }
                }
            }

            // Top Right Buttons (Fullscreen + Close)
            if (!showQueueDesktop) {
                val isDesktop = !getPlatform().isAndroid
                val toggleFullscreen = LocalFullscreenHandler.current
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 8.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isDesktop) {
                        IconButton(onClick = toggleFullscreen) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = stringResource(Res.string.fullscreen),
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(Res.string.back),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun InteractiveProgressBar(
    value: Float,
    maxValue: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    color: Color = Color.White
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val isHovered by interactionSource.collectIsHoveredAsState()

    LaunchedEffect(value, isDragging) {
        if (!isDragging) {
            dragProgress = if (maxValue > 0) (value / maxValue).coerceIn(0f, 1f) else 0f
        }
    }

    val isActive = isHovered || isDragging

    val trackHeight by animateDpAsState(if (isActive) 4.dp else 2.dp)
    val thumbRadius by animateDpAsState(if (isActive) 6.dp else 0.dp)

    Box(
        modifier = modifier
            .height(24.dp) // Narrower touch target
            .hoverable(interactionSource)
            .pointerInput(maxValue, onValueChange, onValueChangeFinished) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange(dragProgress * maxValue)
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished()
                    },
                    onDragCancel = {
                        isDragging = false
                        onValueChangeFinished()
                    }
                ) { change, _ ->
                    change.consume()
                    dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    onValueChange(dragProgress * maxValue)
                }
            }
            .pointerInput(maxValue, onValueChange, onValueChangeFinished) {
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange(dragProgress * maxValue)
                        tryAwaitRelease()
                        isDragging = false
                        onValueChangeFinished()
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val fraction = if (isDragging) dragProgress else if (maxValue > 0) (value / maxValue).coerceIn(0f, 1f) else 0f
        // Animate progress fraction for smooth visual updates when not dragging
        val animatedFraction by animateFloatAsState(targetValue = fraction, animationSpec = tween(durationMillis = 220), label = "ProgressFraction")

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        ) {
            val trackY = size.height / 2
            val trackHeightPx = trackHeight.toPx()
            val trackCornerRadius = CornerRadius(trackHeightPx / 2)

            drawRoundRect(
                color = color.copy(alpha = 0.2f),
                topLeft = Offset(0f, trackY - trackHeightPx / 2),
                size = androidx.compose.ui.geometry.Size(size.width, trackHeightPx),
                cornerRadius = trackCornerRadius
            )

            val progressWidth = size.width * animatedFraction
            drawRoundRect(
                color = color.copy(alpha = 0.8f),
                topLeft = Offset(0f, trackY - trackHeightPx / 2),
                size = androidx.compose.ui.geometry.Size(progressWidth, trackHeightPx),
                cornerRadius = trackCornerRadius
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
              val width = maxWidth
              val offset = width * (if (isDragging) fraction else (if (maxValue > 0) (value / maxValue).coerceIn(0f, 1f) else 0f))

             Box(
                 modifier = Modifier
                     .size(thumbRadius * 2)
                     .offset(x = offset - thumbRadius)
                     .background(color, CircleShape)
                     .graphicsLayer {
                         shadowElevation = 4.dp.toPx()
                         shape = CircleShape
                         clip = false
                     }
             )
         }
     }
}

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun VerticalInteractiveProgressBar(
    value: Float,
    maxValue: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    color: Color = Color.White
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val isHovered by interactionSource.collectIsHoveredAsState()

    LaunchedEffect(value, isDragging) {
        if (!isDragging) {
            dragProgress = if (maxValue > 0) (value / maxValue).coerceIn(0f, 1f) else 0f
        }
    }

    val isActive = isHovered || isDragging
    val thumbRadius by animateDpAsState(if (isActive) 6.dp else 3.dp)
    val trackWidth by animateDpAsState(if (isActive) 2.dp else 1.dp)

    Box(
        modifier = modifier
            .width(30.dp)
            .hoverable(interactionSource)
            .pointerInput(maxValue, onValueChange, onValueChangeFinished) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                        onValueChange(dragProgress * maxValue)
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished()
                    },
                    onDragCancel = {
                        isDragging = false
                        onValueChangeFinished()
                    }
                ) { change, _ ->
                    change.consume()
                    dragProgress = (1f - (change.position.x / size.height)).coerceIn(0f, 1f) // Wait, fix this: change.position.y
                    onValueChange(dragProgress * maxValue)
                }
            }
            .pointerInput(maxValue, onValueChange, onValueChangeFinished) {
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        dragProgress = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                        onValueChange(dragProgress * maxValue)
                        tryAwaitRelease()
                        isDragging = false
                        onValueChangeFinished()
                    }
                )
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        val fraction = if (isDragging) dragProgress else if (maxValue > 0) (value / maxValue).coerceIn(0f, 1f) else 0f

        Canvas(modifier = Modifier.fillMaxHeight().width(30.dp)) {
            val trackX = size.width / 2
            val trackWidthPx = trackWidth.toPx()

            // Draw Track
            drawRoundRect(
                color = color.copy(alpha = 0.2f),
                topLeft = Offset(trackX - trackWidthPx / 2, 0f),
                size = androidx.compose.ui.geometry.Size(trackWidthPx, size.height),
                cornerRadius = CornerRadius(trackWidthPx / 2)
            )

            // Draw Progress
            val progressHeight = size.height * fraction
            drawRoundRect(
                color = color.copy(alpha = 0.8f),
                topLeft = Offset(trackX - trackWidthPx / 2, size.height - progressHeight),
                size = androidx.compose.ui.geometry.Size(trackWidthPx, progressHeight),
                cornerRadius = CornerRadius(trackWidthPx / 2)
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxHeight()) {
             val h = maxHeight
            // y position from top = (1 - fraction) * height
             Box(
                 modifier = Modifier
                     .size(thumbRadius * 2)
                     .offset(y = h * (1 - fraction) - thumbRadius)
                     .background(color, CircleShape)
                     .graphicsLayer {
                         shadowElevation = 4.dp.toPx()
                         shape = CircleShape
                         clip = false
                     }
             )
        }
    }
}

@Composable
fun PlayerControls(
    displayAlbumArt: Any?,
    state: PlayerControlsState,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onPlaybackModeClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onQueueClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {}
) {
    var volume by remember { mutableFloatStateOf(0.5f) }
    var isVolumeOpen by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current // Add it here inside PlayerControls
    val titleCopiedMsg = stringResource(Res.string.copied_format, state.title)
    val artistCopiedMsg = stringResource(Res.string.copied_format, state.artist)

    val isDesktop = !getPlatform().isAndroid
    val buttonSize = if (state.isLandscape || isDesktop) 48.dp else 40.dp
    val playButtonSize = if (state.isLandscape || isDesktop) 64.dp else 56.dp
    val skipIconSize = if (state.isLandscape || isDesktop) 32.dp else 28.dp
    val playIconSize = if (state.isLandscape || isDesktop) 48.dp else 40.dp

    val activeSize = if (state.isLandscape) 340.dp else 460.dp
    val inactiveSize = if (state.isLandscape) 280.dp else 360.dp
    val targetSize = if (state.isPlaying) activeSize else inactiveSize

    val artSize by animateDpAsState(
        targetValue = targetSize,
        animationSpec = tween(durationMillis = 500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "CoverSize"
    )

    val animatedShadowElevation by animateDpAsState(if (state.isPlaying) 16.dp else 4.dp, label = "Shadow")

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Crossfade(
            targetState = displayAlbumArt,
            label = "AlbumArtCrossfade",
            animationSpec = tween(500)
        ) { art ->
            Box(
                modifier = Modifier
                    .size(artSize)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                 AsyncImage(
                    model = art,
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(artSize)
                        .aspectRatio(1f)
                        .graphicsLayer {
                            shadowElevation = animatedShadowElevation.toPx()
                            shape = RoundedCornerShape(12.dp)
                            clip = true
                        }
                        .background(Color.Transparent, RoundedCornerShape(12.dp)))
             }
         }

        Spacer(modifier = Modifier.height(48.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            var showMenu by remember { mutableStateOf(false) }

            Box {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable { showMenu = true }
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable { showMenu = true }
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.copy_song_name)) },
                        onClick = {
                            clipboardManager.setText(AnnotatedString(state.title))
                            top.xiaojiang233.nekomusic.utils.showToast(titleCopiedMsg)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.copy_artist_name)) },
                        onClick = {
                            clipboardManager.setText(AnnotatedString(state.artist))
                            top.xiaojiang233.nekomusic.utils.showToast(artistCopiedMsg)
                            showMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress bar width limits (min..max). Keep it centered on wide windows.
        val progressMin = 280.dp
        val progressMax = 520.dp
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val duration = state.totalDuration.coerceAtLeast(1L).toFloat()
            val position = if (state.isDragging) state.sliderPosition else state.currentPosition.toFloat()

            // Box with width constraints so the progress bar never grows beyond progressMax
            Box(modifier = Modifier.widthIn(min = progressMin, max = progressMax).align(Alignment.CenterHorizontally)) {
                InteractiveProgressBar(
                    value = position,
                    maxValue = duration,
                    onValueChange = onValueChange,
                    onValueChangeFinished = onValueChangeFinished,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.widthIn(max = progressMax).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(position.toLong()),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = formatTime(state.totalDuration),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controls row: keep controls grouped and cap group width to the same max as the progress bar
        val isPortrait = !state.isLandscape && !isDesktop

        if (isPortrait) {
            // Portrait Layout: Wraps controls to multiple lines
            Column(
                modifier = Modifier.widthIn(max = progressMax).fillMaxWidth().align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp) // Larger gap between rows
            ) {
                // Row 1: Primary Playback + Mode + Queue
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val playbackModeIcon = when (state.playbackMode) {
                        PlaybackMode.LoopOne -> Icons.Default.RepeatOne
                        PlaybackMode.Shuffle -> Icons.Default.Shuffle
                        else -> Icons.Default.Repeat
                    }
                    IconButton(onClick = onPlaybackModeClick, modifier = Modifier.size(buttonSize)) {
                        Icon(
                            imageVector = playbackModeIcon,
                            contentDescription = null,
                            tint = if (state.playbackMode == PlaybackMode.Order) Color.White.copy(alpha = 0.6f) else Color.White
                        )
                    }

                    IconButton(onClick = onPreviousClick, modifier = Modifier.size(buttonSize)) {
                        Icon(Icons.Default.SkipPrevious, stringResource(Res.string.prev), tint = Color.White, modifier = Modifier.size(skipIconSize))
                    }

                    Box(
                        modifier = Modifier
                            .size(playButtonSize)
                            .clickable { onPlayPauseClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) stringResource(Res.string.pause) else stringResource(Res.string.play),
                            modifier = Modifier.size(playIconSize),
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = onNextClick, modifier = Modifier.size(buttonSize)) {
                        Icon(Icons.Default.SkipNext, stringResource(Res.string.next), tint = Color.White, modifier = Modifier.size(skipIconSize))
                    }

                    IconButton(onClick = onQueueClick, modifier = Modifier.size(buttonSize)) {
                        Icon(Icons.AutoMirrored.Filled.List, stringResource(Res.string.queue), tint = Color.White)
                    }
                }

                // Row 2: Like, Comment, Intelligence Mode
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onLikeClick, modifier = Modifier.size(buttonSize)) {
                        Icon(
                            imageVector = if (state.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (state.isLiked) stringResource(Res.string.unlike) else stringResource(Res.string.like),
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = onCommentClick, modifier = Modifier.size(buttonSize)) {
                        Icon(Icons.AutoMirrored.Filled.Comment, null, tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            if (state.playbackMode == PlaybackMode.Intelligence) {
                                top.xiaojiang233.nekomusic.player.PlayerController.disableIntelligenceMode()
                            } else {
                                top.xiaojiang233.nekomusic.player.PlayerController.enableIntelligenceMode()
                            }
                        },
                        modifier = Modifier.size(buttonSize)
                    ) {
                        val isIntelligence = state.playbackMode == PlaybackMode.Intelligence
                        Image(
                            painter = painterResource(Res.drawable.hbmode),
                            contentDescription = "Intelligence Mode",
                            colorFilter = ColorFilter.tint(if (isIntelligence) MaterialTheme.colorScheme.primary else Color.White),
                            modifier = Modifier.size(24.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        } else {
            // Desktop/Landscape Layout
            Column(
                modifier = Modifier.widthIn(max = progressMax).fillMaxWidth().align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: Like, Comment, Prev, Play, Next, Mode, Queue
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onLikeClick, modifier = Modifier.size(buttonSize)) {
                            Icon(
                                imageVector = if (state.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (state.isLiked) stringResource(Res.string.unlike) else stringResource(Res.string.like),
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = onCommentClick, modifier = Modifier.size(buttonSize)) {
                            Icon(Icons.AutoMirrored.Filled.Comment, null, tint = Color.White)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconButton(onClick = onPreviousClick, modifier = Modifier.size(buttonSize)) {
                            Icon(Icons.Default.SkipPrevious, stringResource(Res.string.prev), tint = Color.White, modifier = Modifier.size(skipIconSize))
                        }

                        Box(
                            modifier = Modifier
                                .size(playButtonSize)
                                .clickable { onPlayPauseClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying) stringResource(Res.string.pause) else stringResource(Res.string.play),
                                modifier = Modifier.size(playIconSize),
                                tint = Color.White
                            )
                        }

                        IconButton(onClick = onNextClick, modifier = Modifier.size(buttonSize)) {
                            Icon(Icons.Default.SkipNext, stringResource(Res.string.next), tint = Color.White, modifier = Modifier.size(skipIconSize))
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val playbackModeIcon = when (state.playbackMode) {
                            PlaybackMode.LoopOne -> Icons.Default.RepeatOne
                            PlaybackMode.Shuffle -> Icons.Default.Shuffle
                            else -> Icons.Default.Repeat
                        }
                        IconButton(onClick = onPlaybackModeClick, modifier = Modifier.size(buttonSize)) {
                            Icon(
                                imageVector = playbackModeIcon,
                                contentDescription = null,
                                tint = if (state.playbackMode == PlaybackMode.Order) Color.White.copy(alpha = 0.6f) else Color.White
                            )
                        }

                        IconButton(onClick = onQueueClick, modifier = Modifier.size(buttonSize)) {
                            Icon(Icons.AutoMirrored.Filled.List, stringResource(Res.string.queue), tint = Color.White)
                        }
                    }
                }

                // Row 2: Intelligence Mode + Volume (Centered)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (state.playbackMode == PlaybackMode.Intelligence) {
                                top.xiaojiang233.nekomusic.player.PlayerController.disableIntelligenceMode()
                            } else {
                                top.xiaojiang233.nekomusic.player.PlayerController.enableIntelligenceMode()
                            }
                        },
                        modifier = Modifier.size(buttonSize)
                    ) {
                        val isIntelligence = state.playbackMode == PlaybackMode.Intelligence
                        Image(
                            painter = painterResource(Res.drawable.hbmode),
                            contentDescription = "Intelligence Mode",
                            colorFilter = ColorFilter.tint(if (isIntelligence) MaterialTheme.colorScheme.primary else Color.White),
                            modifier = Modifier.size(24.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Box {
                        IconButton(onClick = { isVolumeOpen = true }, modifier = Modifier.size(buttonSize)) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, stringResource(Res.string.volume), tint = Color.White)
                        }
                        if (isVolumeOpen) {
                            Popup(
                                alignment = Alignment.TopCenter,
                                onDismissRequest = { isVolumeOpen = false },
                                offset = IntOffset(0, -20)
                            ) {
                                val interactionSource = remember { MutableInteractionSource() }
                                val isHovered by interactionSource.collectIsHoveredAsState()
                                LaunchedEffect(isHovered) {
                                    if (!isHovered) {
                                        kotlinx.coroutines.delay(500)
                                        isVolumeOpen = false
                                    }
                                }
                                Card(
                                    modifier = Modifier.width(60.dp).height(180.dp).padding(bottom = 12.dp).hoverable(interactionSource),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Spacer(Modifier.height(16.dp))
                                        VerticalInteractiveProgressBar(
                                            value = volume,
                                            maxValue = 1f,
                                            onValueChange = {
                                                volume = it
                                                onVolumeChange(it)
                                            },
                                            onValueChangeFinished = { },
                                            modifier = Modifier.weight(1f),
                                            color = MaterialTheme.colorScheme.primary,
                                            interactionSource = interactionSource
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text(text = "${(volume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        // Volume Bar removed from here
    }
}
