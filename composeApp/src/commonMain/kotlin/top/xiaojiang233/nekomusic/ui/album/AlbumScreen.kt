package top.xiaojiang233.nekomusic.ui.album

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import top.xiaojiang233.nekomusic.ui.playlist.SongListItem
import top.xiaojiang233.nekomusic.viewmodel.AlbumViewModel
import top.xiaojiang233.nekomusic.utils.FavoritesManager
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.utils.thumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    albumId: Long,
    onBackClick: () -> Unit,
    viewModel: AlbumViewModel = viewModel { AlbumViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val likedIds by FavoritesManager.likedSongIds.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(albumId) {
        viewModel.loadAlbum(albumId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.album?.name ?: "Album") },
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
                         // Album Info Header
                         Row(
                             Modifier.fillMaxWidth().padding(16.dp),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             AsyncImage(
                                 model = uiState.album?.picUrl?.thumbnail(300),
                                 contentDescription = null,
                                 modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)),
                                 contentScale = ContentScale.Crop
                             )
                             Spacer(Modifier.width(16.dp))
                             Column {
                                 Text(uiState.album?.name ?: "", style = MaterialTheme.typography.titleLarge)
                             }
                         }
                    }

                    itemsIndexed(uiState.tracks) { index, song ->
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


