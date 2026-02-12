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

import androidx.compose.material.icons.filled.PlayArrow
import top.xiaojiang233.nekomusic.ui.comment.CommentItem
import androidx.compose.foundation.lazy.items
import nekomusic.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

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
                title = { Text(uiState.album?.name ?: stringResource(Res.string.album_fallback)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back))
                    }
                },
                actions = {
                   // Play All Button in TopBar or keep in content?
                   // User asked for "in bar" or just "like I circled"
                   // Screenshot shows Play All usually in header.
                   // Let's put a "Play All" icon action or similar if desired, but sticking to content is safer for Layout.
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(stringResource(Res.string.error_format, uiState.error!!), modifier = Modifier.align(Alignment.Center))
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
                                 Spacer(Modifier.height(8.dp))
                                 Button(onClick = { viewModel.playAll() }) {
                                      Icon(Icons.Default.PlayArrow, null)
                                      Spacer(Modifier.width(8.dp))
                                      Text(stringResource(Res.string.play_all))
                                 }
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

                    item {
                        HorizontalDivider(Modifier.padding(vertical = 16.dp))
                        Text(
                            text = stringResource(Res.string.comments_count_format, uiState.comments.size),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                        if (uiState.isLoadingComments) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                            }
                        }
                    }

                    items(uiState.comments) { comment ->
                        // Reuse CommentItem from CommentScreen or simplified version
                        CommentItem(comment)
                    }
                }
            }
        }
    }
}
