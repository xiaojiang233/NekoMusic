package top.xiaojiang233.nekomusic.ui.artist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import top.xiaojiang233.nekomusic.ui.playlist.SongListItem
import top.xiaojiang233.nekomusic.viewmodel.ArtistViewModel
import top.xiaojiang233.nekomusic.utils.FavoritesManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    artistId: Long,
    onBackClick: () -> Unit,
    viewModel: ArtistViewModel = viewModel { ArtistViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val likedIds by FavoritesManager.likedSongIds.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(artistId) {
        viewModel.loadArtist(artistId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.artist?.name ?: "Artist") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text("Error: ${uiState.error}", modifier = Modifier.align(Alignment.Center))
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
                         }
                    }

                    item {
                        Text("Hot Songs", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
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
            }
        }
    }
}
