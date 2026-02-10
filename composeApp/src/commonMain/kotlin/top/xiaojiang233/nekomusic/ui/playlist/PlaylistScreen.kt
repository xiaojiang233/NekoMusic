package top.xiaojiang233.nekomusic.ui.playlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import top.xiaojiang233.nekomusic.model.Song
import top.xiaojiang233.nekomusic.player.PlayerController
import top.xiaojiang233.nekomusic.utils.thumbnail
import top.xiaojiang233.nekomusic.viewmodel.PlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlistId: Long,
    onBackClick: () -> Unit,
    viewModel: PlaylistViewModel = viewModel { PlaylistViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylist(playlistId)
    }

    val listState = rememberLazyListState()

    // Calculate TopBar alpha based on scroll
    val topBarAlpha by remember {
        derivedStateOf {
            val firstIndex = listState.firstVisibleItemIndex
            val firstOffset = listState.firstVisibleItemScrollOffset
            if (firstIndex > 0) {
                1f
            } else {
                // Fade in over the first 300px roughly
                (firstOffset / 300f).coerceIn(0f, 1f)
            }
        }
    }

    // Pagination checking
    val isScrollToEnd by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf false
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= totalItems - 5 // Buffer of 5 items
        }
    }

    LaunchedEffect(isScrollToEnd) {
        if (isScrollToEnd) {
            viewModel.loadMoreSongs()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlist") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = topBarAlpha),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.error}")
            }
        } else {
            // Search Filter
            var searchQuery by remember { mutableStateOf("") }
            val filteredTracks = remember(uiState.tracks, searchQuery) {
                if (searchQuery.isBlank()) uiState.tracks
                else uiState.tracks.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.ar.any { artist -> artist.name.contains(searchQuery, ignoreCase = true) }
                }
            }

            val playlist = uiState.playlist
            if (playlist != null) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Header
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            // Blurred Background (Could be implemented with heavy modifiers, simplified here with scrim)
                            AsyncImage(
                                model = playlist.coverImgUrl.thumbnail(400),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alpha = 0.3f
                            )
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                                        startY = 0.0f
                                    )
                                )
                            )

                            // Content
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(24.dp)
                                    .padding(bottom = 32.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                AsyncImage(
                                    model = playlist.coverImgUrl.thumbnail(200),
                                    contentDescription = "Cover",
                                    modifier = Modifier.size(160.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(24.dp))
                                Column {
                                    Text(
                                        playlist.name,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (playlist.creator != null) {
                                        Spacer(Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AsyncImage(
                                                model = playlist.creator.avatarUrl.thumbnail(50),
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp).clip(RoundedCornerShape(12.dp))
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                playlist.creator.nickname,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    }
                                    if (!playlist.description.isNullOrEmpty()) {
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            playlist.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = {
                                        PlayerController.playList(filteredTracks)
                                    }) {
                                        Icon(Icons.Filled.PlayArrow, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Play All")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            placeholder = { Text("Filter songs...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true
                        )
                    }

                    // Songs
                    itemsIndexed(filteredTracks) { index, song ->
                        SongListItem(index + 1, song, onClick = {
                            PlayerController.playList(filteredTracks, index)
                        })
                    }

                    if (uiState.isLoadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongListItem(index: Int, song: Song, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$index",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.width(40.dp)
        )

        // Lazy load cover
        AsyncImage(
            model = song.al.picUrl.thumbnail(100),
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.ar.joinToString(", ") { it.name }} - ${song.al.name}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Duration
        Text(
            text = formatDuration(song.dt),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}
