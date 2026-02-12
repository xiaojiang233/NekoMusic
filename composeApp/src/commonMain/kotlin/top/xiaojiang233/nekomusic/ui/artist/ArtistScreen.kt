import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import top.xiaojiang233.nekomusic.ui.playlist.SongListItem
import top.xiaojiang233.nekomusic.viewmodel.ArtistViewModel
import top.xiaojiang233.nekomusic.utils.FavoritesManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import top.xiaojiang233.nekomusic.model.Album
import top.xiaojiang233.nekomusic.utils.thumbnail
import nekomusic.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource


@Suppress("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    artistId: Long,
    onBackClick: () -> Unit,
    onAlbumClick: (Long) -> Unit = {},
    viewModel: ArtistViewModel = viewModel { ArtistViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val likedIds by FavoritesManager.likedSongIds.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(
        stringResource(Res.string.top_50),
        stringResource(Res.string.all_songs_label),
        stringResource(Res.string.albums)
    )

    LaunchedEffect(artistId) {
        viewModel.loadArtist(artistId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.artist?.name ?: stringResource(Res.string.artist_fallback)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val cols = if (maxWidth > 600.dp) 5 else 2

            if (uiState.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(stringResource(Res.string.error_format, uiState.error!!), modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                         // Artist Info Header
                         Column(
                             Modifier.fillMaxWidth().padding(16.dp),
                             horizontalAlignment = Alignment.CenterHorizontally
                         ) {
                             AsyncImage(
                                 model = uiState.artist?.avatar ?: uiState.artist?.cover,
                                 contentDescription = null,
                                 modifier = Modifier.size(120.dp).clip(CircleShape),
                                 contentScale = ContentScale.Crop
                             )
                             Spacer(Modifier.height(16.dp))
                             Text(uiState.artist?.name ?: "", style = MaterialTheme.typography.headlineMedium)
                             uiState.artist?.briefDesc?.let {
                                 Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 3)
                             }
                             Spacer(Modifier.height(16.dp))
                             Button(
                                 onClick = { viewModel.toggleFollow() },
                                 colors = ButtonDefaults.buttonColors(
                                     containerColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                     contentColor = if (uiState.isSubscribed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                                 )
                             ) {
                                 Text(if (uiState.isSubscribed) stringResource(Res.string.followed) else stringResource(Res.string.follow))
                             }
                         }
                    }

                    item {
                        TabRow(selectedTabIndex = selectedTabIndex) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = { Text(title) }
                                )
                            }
                        }
                    }

                    when (selectedTabIndex) {
                        0 -> {
                            item {
                                Text(stringResource(Res.string.hot_songs), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                            }

                            itemsIndexed(uiState.hotSongs) { index, song ->
                                SongListItem(
                                    index = index + 1,
                                    song = song,
                                    onClick = { viewModel.playSong(song) },
                                    isLiked = likedIds.contains(song.id),
                                    onLikeClick = { id -> scope.launch { FavoritesManager.toggleLike(id) } }
                                )
                            }
                        }
                        1 -> {
                            // All songs
                            item {
                                Text(stringResource(Res.string.all_songs_label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                            }

                            itemsIndexed(uiState.allSongs) { index, song ->
                                SongListItem(
                                    index = index + 1,
                                    song = song,
                                    onClick = { viewModel.playAllSong(song) },
                                    isLiked = likedIds.contains(song.id),
                                    onLikeClick = { id -> scope.launch { FavoritesManager.toggleLike(id) } }
                                )
                            }

                            item {
                                LaunchedEffect(Unit) {
                                    viewModel.loadMoreAllSongs()
                                }
                                if (uiState.isLoadingAllSongs) {
                                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                } else if (uiState.allSongsHasMore) {
                                     // Trigger load more when visible
                                     LaunchedEffect(Unit) {
                                         viewModel.loadMoreAllSongs()
                                     }
                                }
                            }
                        }
                        2 -> {
                            item {
                                Text(stringResource(Res.string.albums), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                            }

                            val chunkedAlbums = uiState.albums.chunked(cols)
                            items(chunkedAlbums) { rowAlbums ->
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (album in rowAlbums) {
                                        Box(Modifier.weight(1f)) {
                                            AlbumGridItem(album, onClick = { onAlbumClick(album.id) })
                                        }
                                    }
                                    if (rowAlbums.size < cols) {
                                        repeat(cols - rowAlbums.size) {
                                            Spacer(Modifier.weight(1f))
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

@Composable
fun AlbumGridItem(album: Album, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
             // Vinyl Record (Black circle)
             Box(
                 modifier = Modifier
                     .fillMaxWidth(0.9f)
                     .aspectRatio(1f)
                     .align(Alignment.TopCenter)
                     .offset(y = (-15).dp)
                     .clip(CircleShape)
                     .background(Color.Black)
             ) {
                 // Center hole label
                 Box(
                     modifier = Modifier
                         .fillMaxSize(0.35f)
                         .align(Alignment.Center)
                         .clip(CircleShape)
                         .background(Color(0xFF222222))
                 )
             }

             // Cover
             AsyncImage(
                 model = album.picUrl?.thumbnail(300),
                 contentDescription = null,
                 modifier = Modifier
                     .fillMaxWidth()
                     .aspectRatio(1f)
                     .clip(RoundedCornerShape(8.dp)),
                 contentScale = ContentScale.Crop
             )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))

        val date = try {
            Instant.fromEpochMilliseconds(album.publishTime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date.toString()
        } catch (_: Exception) {
            ""
        }

        Text(
            text = "${album.size}首 · $date",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
