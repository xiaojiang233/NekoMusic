package top.xiaojiang233.nekomusic.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import top.xiaojiang233.nekomusic.utils.thumbnail
import top.xiaojiang233.nekomusic.viewmodel.PlayerViewModel

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
    val song = uiState.playbackState.currentSong ?: return // Should handle empty state better? For overlay ok.

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

    // Hardcoded logic for now (no wearable/config check ported purely)
    // Assuming standard mobile/desktop layout
    // Original Nekoplayer handles Landscape logic differently. I'll stick to Pager for Portrait default.
    // If desktop (wide), Nekoplayer used `if (isLandscape && !isWearable) { Row ... }`.
    // I can replicate that if I can detect orientation/width.
    // `BoxWithConstraints` is better for Multiplatform.

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        // Single root Box: background (blur), dark overlay, content and top-right close button
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

            // dark overlay to improve readability
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

            // content (player / lyrics / pager)
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
                            title = title,
                            artist = artist,
                            totalDuration = totalDuration,
                            currentPosition = currentPosition,
                            isDragging = isSliderDragging,
                            sliderPosition = sliderPosition,
                            isPlaying = isPlaying,
                            onValueChange = { sliderPosition = it },
                            onValueChangeFinished = { viewModel.seekTo(sliderPosition.toLong()) },
                            onDragChange = { dragging -> isSliderDragging = dragging },
                            onPlayPauseClick = { viewModel.togglePlay() },
                            onPreviousClick = { viewModel.playPrevious() },
                            onNextClick = { viewModel.playNext() },
                            isLandscape = true
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        LyricsScreen(viewModel)
                    }
                }
            } else {
                // Portrait
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
                                    title = title,
                                    artist = artist,
                                    totalDuration = totalDuration,
                                    currentPosition = currentPosition,
                                    isDragging = isSliderDragging,
                                    sliderPosition = sliderPosition,
                                    isPlaying = isPlaying,
                                    onValueChange = { sliderPosition = it },
                                    onValueChangeFinished = { viewModel.seekTo(sliderPosition.toLong()) },
                                    onDragChange = { dragging -> isSliderDragging = dragging },
                                    onPlayPauseClick = { viewModel.togglePlay() },
                                    onPreviousClick = { viewModel.playPrevious() },
                                    onNextClick = { viewModel.playNext() },
                                    isLandscape = false
                                )
                            }
                        }
                        1 -> {
                            LyricsScreen(viewModel)
                        }
                    }
                }
            }

            // top-right close button
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
fun CustomProgressBar(
    value: Float,
    maxValue: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    onDragChange: (Boolean) -> Unit = {}
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isDragging) {
        onDragChange(isDragging)
    }

    // Sync dragProgress with value when not dragging
    LaunchedEffect(value, isDragging) {
        if (!isDragging) {
            dragProgress = if (maxValue > 0) (value / maxValue).coerceIn(0f, 1f) else 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
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
        contentAlignment = Alignment.Center
    ) {
        // Calculate dimensions based on drag state
        val thumbRadius = if (isDragging) 6.dp else 3.dp
        val trackHeight = if (isDragging) 2.dp else 1.dp

        // Use local dragProgress when dragging, otherwise calculated fraction
        val fraction = if (isDragging) dragProgress else if (maxValue > 0) (value / maxValue).coerceIn(0f, 1f) else 0f

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp) // Fixed height to contain the thumb
        ) {
            val trackY = size.height / 2
            val trackHeightPx = trackHeight.toPx()
            val thumbRadiusPx = thumbRadius.toPx()
            val trackCornerRadius = CornerRadius(trackHeightPx / 2)

            // Background track
            drawRoundRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(0f, trackY - trackHeightPx / 2),
                size = androidx.compose.ui.geometry.Size(size.width, trackHeightPx),
                cornerRadius = trackCornerRadius
            )

            // Progress track
            val progressWidth = size.width * fraction
            drawRoundRect(
                color = Color.White.copy(alpha = 0.7f),
                topLeft = Offset(0f, trackY - trackHeightPx / 2),
                size = androidx.compose.ui.geometry.Size(progressWidth, trackHeightPx),
                cornerRadius = trackCornerRadius
            )

            // Thumb
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = thumbRadiusPx,
                center = Offset(x = progressWidth, y = trackY)
            )
        }
    }
}


@Composable
fun PlayerControls(
    displayAlbumArt: Any?,
    title: String,
    artist: String,
    totalDuration: Long,
    currentPosition: Long,
    isDragging: Boolean,
    sliderPosition: Float,
    isPlaying: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    onDragChange: (Boolean) -> Unit = {},
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    isLandscape: Boolean
) {
    // Dynamic sizing logic adapted for simple Compose Multiplatform
    // Hardcoding some responsive values or relying on BoxWithConstraints in parent could work,
    // but here trying to match visual of typical mobile player.
    // For Landscape, album art is smaller.

    val artSize = if (isLandscape) 200.dp else 300.dp

    Crossfade(
        targetState = displayAlbumArt,
        label = "AlbumArtCrossfade",
        animationSpec = tween(500)
    ) { art ->
        AsyncImage(
            model = art,
            contentDescription = "Album Art",
            modifier = Modifier
                .size(artSize)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
        )
    }

    Spacer(modifier = Modifier.height(32.dp))

    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.5f),
        offset = Offset(0f, 4f),
        blurRadius = 8f
    )

    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium.copy(color = Color.White, shadow = textShadow),
        textAlign = TextAlign.Center
    )
    Text(
        text = artist,
        style = MaterialTheme.typography.titleMedium.copy(color = Color.White.copy(alpha = 0.8f), shadow = textShadow),
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(32.dp))

    Column(modifier = Modifier.fillMaxWidth()) {
        val duration = totalDuration.coerceAtLeast(1L).toFloat()
        // Always use sliderPosition when dragging to ensure real-time UI updates
        val position = if (isDragging) sliderPosition else currentPosition.toFloat()

        CustomProgressBar(
            value = position,
            maxValue = duration,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            onDragChange = onDragChange
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(position.toLong()),
                style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow),
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = formatTime(totalDuration),
                style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow),
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isLandscape) Arrangement.SpaceBetween else Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Simplified controls without repeat/shuffle for MVP port as per request "copy authentically" usually implies visuals + core logic
        // But original code had them. I'll omit them for now as I don't have those in my ViewModel yet,
        // OR add placeholders. The prompt said "copy authentically", so I should probably keep the buttons even if they don't work or wire them if I can.
        // My ViewModel doesn't expose repeat/shuffle yet. I'll place icons but dummy click for now.

        if (isLandscape) {
             // Landscape shuffle/repeat placeholder
             IconButton(onClick = {}) {
                 Icon(Icons.Default.Shuffle, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
             }
        }

        IconButton(onClick = onPreviousClick) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White)
        }
        IconButton(onClick = onPlayPauseClick) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
        }
        IconButton(onClick = onNextClick) {
            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
        }

        if (isLandscape) {
             Spacer(modifier = Modifier.size(48.dp))
        }
    }

    if (!isLandscape) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
             // Portrait shuffle/repeat placeholder
             IconButton(onClick = {}) {
                 Icon(Icons.Default.Shuffle, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
             }
             IconButton(onClick = {}) {
                 Icon(Icons.Default.Repeat, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
             }
        }
    }
}
