package top.xiaojiang233.nekomusic.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import nekomusic.composeapp.generated.resources.Res
import nekomusic.composeapp.generated.resources.clear
import nekomusic.composeapp.generated.resources.history
import nekomusic.composeapp.generated.resources.no_history
import nekomusic.composeapp.generated.resources.search_placeholder
import org.jetbrains.compose.resources.stringResource
import top.xiaojiang233.nekomusic.ui.playlist.SongListItem
import top.xiaojiang233.nekomusic.viewmodel.SearchViewModel
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.InputChip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import top.xiaojiang233.nekomusic.model.Artist
import top.xiaojiang233.nekomusic.model.Playlist
import top.xiaojiang233.nekomusic.utils.thumbnail
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import top.xiaojiang233.nekomusic.utils.FavoritesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onPlaylistClick: (Long) -> Unit = {},
    onArtistClick: (Long) -> Unit = {},
    viewModel: SearchViewModel = viewModel { SearchViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val likedIds by FavoritesManager.likedSongIds.collectAsState()
    val scope = rememberCoroutineScope()
    val tabs = listOf("Songs", "Playlists", "Artists")
    val selectedTabIndex = when(uiState.searchType) {
        1000 -> 1
        100 -> 2
        else -> 0
    }

    Scaffold(
        topBar = {
            Column {
                SearchBar(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    query = uiState.query,
                    onQueryChange = { viewModel.onQueryChange(it) },
                    onSearch = { viewModel.onSearch(it) },
                    active = false,
                    onActiveChange = {},
                    placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                    leadingIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    }
                ) { }
                if (uiState.query.isNotBlank()) {
                     TabRow(selectedTabIndex = selectedTabIndex) {
                         tabs.forEachIndexed { index, title ->
                             Tab(
                                 selected = selectedTabIndex == index,
                                 onClick = {
                                     val type = when(index) {
                                         1 -> 1000
                                         2 -> 100
                                         else -> 1
                                     }
                                     viewModel.onTabChange(type)
                                 },
                                 text = { Text(title) }
                             )
                         }
                     }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(
                    text = "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.query.isBlank()) {
                // History View
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(Res.string.history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text(stringResource(Res.string.clear))
                        }
                    }

                    if (uiState.history.isNotEmpty()) {
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            uiState.history.forEach { historyItem ->
                                InputChip(
                                    selected = false,
                                    onClick = { viewModel.onSearch(historyItem) },
                                    label = { Text(historyItem) }
                                )
                            }
                        }
                    } else {
                        Text(stringResource(Res.string.no_history), color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    when (uiState.searchType) {
                        1 -> {
                            itemsIndexed(uiState.songResults) { index, song ->
                                SongListItem(
                                    index = index + 1,
                                    song = song,
                                    onClick = { viewModel.playSong(song) },
                                    onArtistClick = onArtistClick,
                                    isLiked = likedIds.contains(song.id),
                                    onLikeClick = { id -> scope.launch { FavoritesManager.toggleLike(id) } }
                                )
                            }
                        }
                        1000 -> {
                            itemsIndexed(uiState.playlistResults) { _, playlist ->
                                PlaylistListItem(playlist, onClick = { onPlaylistClick(playlist.id) })
                            }
                        }
                        100 -> {
                            itemsIndexed(uiState.artistResults) { _, artist ->
                                ArtistListItem(artist, onClick = { onArtistClick(artist.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistListItem(playlist: Playlist, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = (playlist.coverImgUrl ?: playlist.picUrl)?.thumbnail(100),
            contentDescription = null,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text("${playlist.trackCount} tracks", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun ArtistListItem(artist: Artist, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = (artist.picUrl ?: artist.img1v1Url)?.thumbnail(100),
            contentDescription = null,
            modifier = Modifier.size(56.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Text(artist.name, style = MaterialTheme.typography.titleMedium)
    }
}
