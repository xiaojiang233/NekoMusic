package top.xiaojiang233.nekomusic.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import top.xiaojiang233.nekomusic.player.AudioManager
import top.xiaojiang233.nekomusic.utils.thumbnail

@Composable
fun PlayerBar(
    modifier: Modifier = Modifier, // Add modifier param for positioning
    onClick: () -> Unit
) {
    val state by AudioManager.state.collectAsState()
    val song = state.currentSong

    // Animate visibility of the dock
    AnimatedVisibility(
        visible = song != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier.padding(16.dp) // Outer padding for floating effect
    ) {
        if (song != null) {
            // Limit width so it won't cover full window; center it via parent alignment
            Surface(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 920.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .height(72.dp),
                shape = RoundedCornerShape(36.dp), // Pill shape
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                // Click handled on inner row so only the pill area is clickable
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onClick() }
                            .padding(horizontal = 12.dp, vertical = 8.dp), // Inner padding
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cover
                        AsyncImage(
                            model = song.al.picUrl.thumbnail(100),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(28.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(Modifier.width(12.dp))

                        // Info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.ar.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Controls
                        FilledIconButton(
                            onClick = {
                                if (state.isPlaying) AudioManager.pause() else AudioManager.resume()
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play"
                            )
                        }

                        Spacer(Modifier.width(8.dp))
                    }

                    // Thin Progress Indicator at bottom sized to the pill width
                    if (state.duration > 0) {
                        val progress = (state.currentPosition.toFloat() / state.duration.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(start = 8.dp, end = 8.dp, bottom = 6.dp)
                                .height(3.dp)
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}
