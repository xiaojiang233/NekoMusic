package top.xiaojiang233.nekomusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.xiaojiang233.nekomusic.player.PlayerController
import top.xiaojiang233.nekomusic.utils.formatDuration

@Composable
fun PlaylistOverlay(
    onDismissRequest: () -> Unit
) {
    val queue by PlayerController.queue.collectAsState()
    val currentIndex by PlayerController.currentIndex.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp) // Limit height
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current Queue (${queue.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, "Close")
                }
            }

            HorizontalDivider()

            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(queue) { index, song ->
                    val isPlaying = index == currentIndex
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Jump to this song
                                // We don't have jumpTo yet? We can use logic to set index
                                // PlayerController doesn't expose public setIndex directly but we can reuse playList?
                                // Or playNext/Prev until there.
                                // Actually PlayerController has playList but that resets queue.
                                // We might need a `playAt(index)` method.
                                // For now, we can implement it or add it to Controller.
                            }
                            .background(if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPlaying) {
                            Text("♪", color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(24.dp))
                        } else {
                            Text("${index + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(24.dp))
                        }

                        Column(Modifier.weight(1f)) {
                            Text(song.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text(
                                song.ar.joinToString { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Text(
                            formatDuration(song.dt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        IconButton(onClick = { PlayerController.removeFromQueue(index) }) {
                            Icon(Icons.Default.Delete, "Remove", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

