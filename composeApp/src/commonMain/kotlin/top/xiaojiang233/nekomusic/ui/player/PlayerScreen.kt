package top.xiaojiang233.nekomusic.ui.player

import androidx.compose.animation.Crossfade
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import top.xiaojiang233.nekomusic.getPlatform
import top.xiaojiang233.nekomusic.player.PlaybackMode
import top.xiaojiang233.nekomusic.utils.thumbnail
import top.xiaojiang233.nekomusic.viewmodel.PlayerViewModel
import java.awt.Robot
import java.awt.event.KeyEvent

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
    val isLandscape: Boolean
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel { PlayerViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPlaying = uiState.playbackState.isPlaying
    val currentPosition = uiState.playbackState.currentPosition
    val totalDuration = uiState.playbackState.duration
    val playbackMode = uiState.playbackMode // Add this to retrieve mode from UI state
    val song = uiState.playbackState.currentSong ?: return

    // Local slider state
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSliderDragging by remember { mutableStateOf(false) }

    LaunchedEffect(currentPosition) {
        if (!isSliderDragging) {
            sliderPosition = currentPosition.toFloat()
        }
    }

    val displayAlbumArt = song.al.picUrl.thumbnail(600)
    val title = song.name
    val artist = song.ar.joinToString(", ") { it.name }

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

            if (isLandscape) {
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
                                isLandscape = true
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
                            onVolumeChange = { viewModel.setVolume(it) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        LyricsScreen(viewModel)
                    }
                }
            } else {
                val pagerState = rememberPagerState(pageCount = { 2 })
                HorizontalPager(state = pagerState) { page ->
                    when (page) {
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
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
                                        isLandscape = false
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
                                    onVolumeChange = { viewModel.setVolume(it) }
                                )
                            }
                        }
                        1 -> {
                            LyricsScreen(viewModel)
                        }
                    }
                }
            }

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
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
            .pointerInput(Unit) {
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
            .pointerInput(Unit) {
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
            .pointerInput(Unit) {
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
                    dragProgress = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                    onValueChange(dragProgress * maxValue)
                }
            }
            .pointerInput(Unit) {
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
    onVolumeChange: (Float) -> Unit
) {
    var volume by remember { mutableFloatStateOf(0.5f) }
    var isVolumeOpen by remember { mutableStateOf(false) }
    var controlGap by rememberSaveable { mutableFloatStateOf(16f) }

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
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.artist,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
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
        Row(
            modifier = Modifier.widthIn(max = progressMax).align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val playbackModeIcon = when(state.playbackMode) {
                PlaybackMode.LoopOne -> Icons.Default.RepeatOne
                PlaybackMode.Shuffle -> Icons.Default.Shuffle
                PlaybackMode.Order -> Icons.Default.Repeat
            }

            IconButton(onClick = onPlaybackModeClick) {
                 Icon(playbackModeIcon, null, tint = if (state.playbackMode == PlaybackMode.Order) Color.White.copy(alpha = 0.6f) else Color.White)
            }
            IconButton(onClick = onPreviousClick, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clickable { onPlayPauseClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
            }

            IconButton(onClick = onNextClick, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            // Volume Control
            Box {
                IconButton(onClick = { isVolumeOpen = true }) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        "Volume",
                        tint = Color.White
                    )
                }

                if (isVolumeOpen) {
                    Popup(
                        alignment = Alignment.TopCenter,
                        onDismissRequest = { isVolumeOpen = false },
                        offset = IntOffset(0, -20)
                    ) {
                        val interactionSource = remember { MutableInteractionSource() }
                        val isHovered by interactionSource.collectIsHoveredAsState()
                        // Monitor dragging logic inside VerticalProgressBar manually or rely on hover delay
                        // For simplicity, we delay close on hover exit.

                        LaunchedEffect(isHovered) {
                            if (!isHovered) {
                                // Delay to allow mouse to move or click, but if user drags slider they might exit hover area if slider is thin.
                                // However, pointerInput usually captures drag. But hover state might update.
                                // We rely on user tapping out or waiting
                                kotlinx.coroutines.delay(500)
                                isVolumeOpen = false
                            }
                        }

                        Card(
                            modifier = Modifier
                                .width(60.dp)
                                .height(180.dp)
                                .padding(bottom = 12.dp)
                                .hoverable(interactionSource),
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
                                        // Keep open while changing
                                    },
                                    onValueChangeFinished = { },
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.primary,
                                    interactionSource = interactionSource
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "${(volume * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // Fullscreen Button (Desktop Only)
            if (getPlatform().name == "Desktop") {
                IconButton(onClick = {
                    // Simulate F11 key press for fullscreen toggle since we can't easily access WindowState here
                    // Alternatively, we could pass a callback, but let's try AWT Robot or just adding a callback param is better.
                    // Given the constraints and the previous main.kt having F11 handler:
                    try {
                        val robot = Robot()
                        robot.keyPress(KeyEvent.VK_F11)
                        robot.keyRelease(KeyEvent.VK_F11)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }) {
                    Icon(
                        Icons.Default.Fullscreen, // Or FullscreenExit if we tracked state
                        "Fullscreen",
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        // Volume Bar removed from here
    }
}
