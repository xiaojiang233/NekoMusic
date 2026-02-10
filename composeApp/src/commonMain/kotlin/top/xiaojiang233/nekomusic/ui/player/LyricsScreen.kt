package top.xiaojiang233.nekomusic.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import top.xiaojiang233.nekomusic.settings.SettingsManager
import top.xiaojiang233.nekomusic.ui.components.KaraokeLyricLine
import top.xiaojiang233.nekomusic.viewmodel.PlayerViewModel
import kotlin.math.abs

@Composable
fun LyricsScreen(
    viewModel: PlayerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val lyrics = uiState.lyrics
    val isLoadingLyrics = uiState.isLoadingLyrics
    // Instead of directly using uiState position, we use a local interpolated one for smoothness
    val playbackState = uiState.playbackState

    // Lyrics settings
    val lyricsFontSize by SettingsManager.getLyricsFontSize().collectAsState(initial = 24f)
    val lyricsBlurIntensity by SettingsManager.getLyricsBlurIntensity().collectAsState(initial = 10f)
    val lyricsFontFamilyName by SettingsManager.getLyricsFontFamily().collectAsState(initial = "Default")

    val fontFamily = when (lyricsFontFamilyName) {
        "Serif" -> FontFamily.Serif
        "SansSerif" -> FontFamily.SansSerif
        "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        else -> FontFamily.Default
    }

    // High-frequency position for smooth lyrics
    var currentPosition by remember { mutableStateOf(playbackState.currentPosition) }

    LaunchedEffect(playbackState) {
        val startTime = System.currentTimeMillis()
        val startPos = playbackState.currentPosition

        if (playbackState.isPlaying) {
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                currentPosition = startPos + elapsed
                withFrameMillis { }
            }
        } else {
            currentPosition = startPos
        }
    }

    val listState = rememberLazyListState()
    val isDragged by listState.interactionSource.collectIsDraggedAsState()

    var isUserScrolling by remember { mutableStateOf(false) }
    var isAutoScrolling by remember { mutableStateOf(false) }

    // Detect manual scroll (including mouse wheel)
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && !isAutoScrolling) {
            isUserScrolling = true
        }
    }

    LaunchedEffect(isUserScrolling, isDragged, listState.isScrollInProgress) {
        if (isUserScrolling && !isDragged && !listState.isScrollInProgress) {
            delay(3000)
            isUserScrolling = false
        }
    }

    val displayLyrics = remember(lyrics) {
        buildList {
            if (lyrics.isNotEmpty()) {
                val firstLyricTime = lyrics[0].time
                if (firstLyricTime > 1000) {
                    add(LyricItem.Interlude(time = 0, endTime = firstLyricTime))
                }
            }
            lyrics.forEachIndexed { index, lyric ->
                add(LyricItem.Content(lyric.time, lyric.text, lyric.translation))
                val nextTime = lyrics.getOrNull(index + 1)?.time
                if (nextTime != null && nextTime - lyric.time >= 10_000) {
                    add(LyricItem.Interlude(time = lyric.time + 5000, endTime = nextTime))
                }
            }
        }
    }

    val currentDisplayIndex by remember(currentPosition, displayLyrics) {
        derivedStateOf {
            val adjustedPosition = currentPosition // Removed offset for accuracy with smooth timer
            displayLyrics.indexOfLast { it.time <= adjustedPosition }.coerceAtLeast(-1)
        }
    }

    LaunchedEffect(isDragged) {
        if (isDragged) {
            isUserScrolling = true
        }
    }

    LaunchedEffect(currentDisplayIndex, isUserScrolling) {
        if (currentDisplayIndex >= 0 && !isUserScrolling) {
            isAutoScrolling = true
            listState.animateScrollToItem(
                index = currentDisplayIndex,
                scrollOffset = 0
            )
            isAutoScrolling = false
        }
    }

    // Get container height using BoxWithConstraints to calculate padding
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerHeight = this.maxHeight
        val verticalPadding = containerHeight * 0.30f
        val horizontalPadding = 32.dp

        LaunchedEffect(currentDisplayIndex, isUserScrolling) {
            if (currentDisplayIndex >= 0 && !isUserScrolling) {
                isAutoScrolling = true
                listState.animateScrollToItem(
                    index = currentDisplayIndex,
                    scrollOffset = 0
                )
                isAutoScrolling = false
            }
        }

        // Inner Box for content
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingLyrics) {
                CircularProgressIndicator(color = Color.White)
            } else if (displayLyrics.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No Lyrics", // Hardcoded string resource for now
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = verticalPadding),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(displayLyrics) { index, item ->
                        val isCurrent = index == currentDisplayIndex
                        val distance = abs(index - currentDisplayIndex)

                        val scale by animateFloatAsState(
                            targetValue = if (isCurrent) 1f else 0.95f,
                            animationSpec = tween(durationMillis = 300),
                            label = "scale"
                        )

                        val targetAlpha = if (isUserScrolling) 1f else (1f - (distance * 0.15f)).coerceIn(0.1f, 1f)
                        val animatedAlpha by animateFloatAsState(
                            targetValue = if (isCurrent) 1f else targetAlpha,
                            animationSpec = tween(durationMillis = 300),
                            label = "alpha"
                        )

                        val targetBlur = if (isUserScrolling) 0f else if (isCurrent) 0f else (distance * 2f * (lyricsBlurIntensity / 10f)).coerceAtMost(lyricsBlurIntensity)
                        val blurRadius by animateFloatAsState(
                            targetValue = targetBlur,
                            animationSpec = tween(durationMillis = 300),
                            label = "blur"
                        )

                        when (item) {
                            is LyricItem.Content -> {
                                val lyricLine = lyrics.find { it.time == item.time }

                                // Need to manually recreate the padding Modifier due to scope
                                // The boxWithConstraints scope isn't automatically available inside items
                                // But `horizontalPadding` is just a value here.
                                // The issue is BoxWithConstraints content must use the scope or return Unit.
                                // The previous error "BoxWithConstraints scope is not used" is actually fine in Kotlin if we don't need it,
                                // but sometimes Compose compiler is strict or the nesting structure was slightly off.
                                // Re-structuring to ensure `maxHeight` usage counts as usage.
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = horizontalPadding, vertical = 12.dp)
                                        .graphicsLayer {
                                            alpha = 0.99f
                                            clip = false
                                        }
                                        .scale(scale)
                                        .alpha(animatedAlpha)
                                        .blur(blurRadius.dp)
                                        .clickable {
                                            viewModel.seekTo(item.time)
                                            isUserScrolling = false
                                        }
                                ) {
                                    if (lyricLine?.words != null && isCurrent) {
                                        // Verbatim lyrics rendering (Fluid Version)
                                        KaraokeLyricLine(
                                            text = item.text,
                                            words = lyricLine.words,
                                            currentPosition = currentPosition,
                                            fontSize = lyricsFontSize.sp,
                                            fontFamily = fontFamily,
                                            activeColor = Color.White,
                                            inactiveColor = Color.Gray
                                        )
                                    } else {
                                        // Standard text
                                        Text(
                                            text = item.text,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = lyricsFontSize.sp,
                                                lineHeight = (lyricsFontSize * 1.4).sp,
                                                fontFamily = fontFamily
                                            ),
                                            color = if (index > currentDisplayIndex) Color.Gray else Color.White,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    if (!item.translation.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.translation,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Normal,
                                                fontSize = (lyricsFontSize * 0.7f).sp,
                                                lineHeight = (lyricsFontSize * 0.7f * 1.3).sp,
                                                fontFamily = fontFamily
                                            ),
                                            color = (if (index > currentDisplayIndex) Color.Gray else Color.White).copy(alpha = 0.75f),
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            is LyricItem.Interlude -> {
                                val duration = item.endTime - item.time
                                val progress by remember(currentPosition, item) {
                                    derivedStateOf {
                                        ((currentPosition - item.time).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                                    }
                                }

                                val infiniteTransition = rememberInfiniteTransition(label = "dots")
                                val dotPulseAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.5f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "dotPulseAlpha"
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = horizontalPadding, vertical = 12.dp)
                                        .graphicsLayer {
                                            alpha = 0.99f
                                            clip = false
                                        }
                                        .scale(scale)
                                        .alpha(animatedAlpha)
                                        .blur(blurRadius.dp)
                                        .clickable {
                                            viewModel.seekTo(item.time)
                                            isUserScrolling = false
                                        },
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val dotSize = (lyricsFontSize * 0.8).dp
                                    val spacing = 16.dp

                                    for (i in 0 until 3) {
                                        val disappearThreshold = when(i) {
                                            2 -> 0.33f
                                            1 -> 0.66f
                                            else -> 0.90f
                                        }
                                        val isVisible = progress < disappearThreshold

                                        val fadeAlpha by animateFloatAsState(
                                            targetValue = if (isVisible) 1f else 0f,
                                            animationSpec = tween(durationMillis = 200, easing = LinearEasing),
                                            label = "fadeAlpha$i"
                                        )

                                        if (fadeAlpha > 0f) {
                                            val currentDotAlpha = if (isCurrent) dotPulseAlpha * fadeAlpha else 0.5f * fadeAlpha
                                            Box(
                                                modifier = Modifier
                                                    .size(dotSize)
                                                    .background(Color.White.copy(alpha = currentDotAlpha), CircleShape)
                                            )
                                            if (i < 2) {
                                                Spacer(modifier = Modifier.width(spacing))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed interface LyricItem {
    val time: Long
    data class Content(override val time: Long, val text: String, val translation: String? = null) : LyricItem
    data class Interlude(override val time: Long, val endTime: Long) : LyricItem
}
