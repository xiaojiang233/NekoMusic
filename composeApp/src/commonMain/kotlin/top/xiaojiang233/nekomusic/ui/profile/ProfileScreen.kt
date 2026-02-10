package top.xiaojiang233.nekomusic.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import top.xiaojiang233.nekomusic.model.Playlist
import top.xiaojiang233.nekomusic.model.UserProfile
import top.xiaojiang233.nekomusic.utils.thumbnail
import top.xiaojiang233.nekomusic.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    onPlaylistClick: (Long) -> Unit, // Add callback
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()

    // Auto refresh if needed, for instance if we navigate here
    LaunchedEffect(Unit) {
        if (!uiState.isLoggedIn && !uiState.isLoading) {
             viewModel.loadProfile()
        }
    }

    Scaffold(
        topBar = {
           // Maybe a simple title or none if we want full page
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (!uiState.isLoggedIn) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Please Login first")
                    Button(onClick = { viewModel.loadProfile() }) {
                        Text("Retry / Refresh")
                    }
                }
            } else if (uiState.error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error: ${uiState.error}")
                    Button(onClick = { viewModel.loadProfile() }) {
                        Text("Retry")
                    }
                }
            } else {
                val profile = uiState.userDetail?.profile
                if (profile != null) {
                    ProfileContent(
                        profile = profile,
                        level = uiState.userDetail?.level ?: 0,
                        listenSongs = uiState.userDetail?.listenSongs ?: 0,
                        playlists = uiState.userPlaylists,
                        onPlaylistClick = onPlaylistClick // Pass through
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileContent(
    profile: UserProfile,
    level: Int,
    listenSongs: Int,
    playlists: List<Playlist>,
    onPlaylistClick: (Long) -> Unit // Add callback
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = profile.avatarUrl.thumbnail(200),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(24.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = profile.nickname,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                Text("Lv.$level", modifier = Modifier.padding(4.dp))
                            }
                            Text("ID: ${profile.userId}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                        if (profile.signature?.isNotBlank() == true) {
                            Text(profile.signature, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                        }
                        Text("Listening: $listenSongs songs", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Text(
                "My Playlists (${playlists.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(playlists) { playlist ->
            PlaylistRowItem(playlist, onPlaylistClick) // Pass through
        }
    }
}

@Composable
fun PlaylistRowItem(playlist: Playlist, onClick: (Long) -> Unit) { // Add callback
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick(playlist.id) } // Implement click
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = (playlist.coverImgUrl ?: playlist.picUrl).thumbnail(100), // Note: Playlist model in User response uses coverImgUrl? no, let's check
            contentDescription = playlist.name,
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
            Text(
                text = "${playlist.trackCount} songs",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
