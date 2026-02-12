package top.xiaojiang233.nekomusic.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import top.xiaojiang233.nekomusic.player.AudioManager
import top.xiaojiang233.nekomusic.utils.FavoritesManager
import top.xiaojiang233.nekomusic.utils.thumbnail
import kotlinx.coroutines.launch
import nekomusic.composeapp.generated.resources.Res
import nekomusic.composeapp.generated.resources.pause
import nekomusic.composeapp.generated.resources.play
import nekomusic.composeapp.generated.resources.queue
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlayerBar(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onCommentClick: () -> Unit = {} // Added parameter
) {
    val state by AudioManager.state.collectAsState()
    val song = state.currentSong
    val likedIds by FavoritesManager.likedSongIds.collectAsState()
    val scope = rememberCoroutineScope()

    // Animate visibility of the dock
    AnimatedVisibility(
        visible = song != null,
        enter = slideInVertically(animationSpec = tween(150)) { it } + fadeIn(animationSpec = tween(150)),
        exit = slideOutVertically(animationSpec = tween(150)) { it } + fadeOut(animationSpec = tween(150)),
        modifier = modifier.padding(16.dp) // Outer padding for floating effect
    ) {
        if (song != null) {
            val targetProgress = (if (state.duration > 0) state.currentPosition.toFloat() / state.duration.toFloat() else 0f).coerceIn(0f, 1f)
            val progress by animateFloatAsState(
                targetValue = targetProgress,
                animationSpec = tween(durationMillis = 500, easing = LinearEasing), // Smooth linear interpolation
                label = "Progress"
            )


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
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    )

                    // Content
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onClick() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cover
                        AsyncImage(
                            model = song.al.cover.thumbnail(100),
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
                        IconButton(onClick = onCommentClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Comment,
                                contentDescription = "Comments",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Like Button
                        IconButton(
                            onClick = {
                                scope.launch { FavoritesManager.toggleLike(song.id) }
                            }
                        ) {
                            val isLiked = likedIds.contains(song.id)
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isLiked) "Unlike" else "Like",
                                // Use Primary color for active, or standard
                                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Play/Pause
                        IconButton(
                            onClick = {
                                if (state.isPlaying) AudioManager.pause() else AudioManager.resume()
                            },
                            modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying) stringResource(Res.string.pause) else stringResource(Res.string.play),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }


                    }
                }
            }
        }
    }
}
