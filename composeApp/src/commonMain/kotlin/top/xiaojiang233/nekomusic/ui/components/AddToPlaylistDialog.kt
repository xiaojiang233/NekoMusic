package top.xiaojiang233.nekomusic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import top.xiaojiang233.nekomusic.model.Playlist
import top.xiaojiang233.nekomusic.viewmodel.ProfileViewModel

@Composable
fun AddToPlaylistDialog(
    songId: Long,
    onDismiss: () -> Unit,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist") },
        text = {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    // Only standard playlists (not special ones for now, checking canDislike is a weak signal, usually rely on creator)
                    val myPlaylists = uiState.userPlaylists.filter { it.creator?.userId == uiState.userId }
                    items(myPlaylists) { playlist ->
                         ListItem(
                             headlineContent = { Text(playlist.name) },
                             supportingContent = { Text("${playlist.trackCount} tracks") },
                             modifier = Modifier.clickable {
                                 // Add logic
                                 viewModel.addSongToPlaylist(playlist.id, songId)
                                 onDismiss()
                             }
                         )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

