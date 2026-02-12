package top.xiaojiang233.nekomusic.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
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
import nekomusic.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import top.xiaojiang233.nekomusic.model.Playlist
import top.xiaojiang233.nekomusic.model.Song
import top.xiaojiang233.nekomusic.model.UserProfile
import top.xiaojiang233.nekomusic.utils.thumbnail
import top.xiaojiang233.nekomusic.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    onPlaylistClick: (Long) -> Unit, // Add callback
    onArtistClick: (Long) -> Unit,
    onFollowsClick: () -> Unit,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    // Auto refresh if needed, for instance if we navigate here
    LaunchedEffect(Unit) {
        if (!uiState.isLoggedIn && !uiState.isLoading) {
             viewModel.loadProfile()
        }
    }

    Scaffold(
        topBar = {
           // Maybe a simple title or none if we want full page
        },
        floatingActionButton = {
            if (uiState.isLoggedIn) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Filled.Add, stringResource(Res.string.create_playlist))
                }
            }
        }
    ) { padding ->
        if (showCreateDialog) {
             CreatePlaylistDialog(
                 onDismiss = { showCreateDialog = false },
                 onConfirm = { name, isPublic ->
                     viewModel.createPlaylist(name, isPublic)
                     showCreateDialog = false
                 }
             )
        }

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                !uiState.isLoggedIn -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(Res.string.please_login_first))
                        Button(onClick = { viewModel.loadProfile() }) {
                            Text(stringResource(Res.string.retry_refresh))
                        }
                    }
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(Res.string.error_format, uiState.error!!))
                        Button(onClick = { viewModel.loadProfile() }) {
                            Text(stringResource(Res.string.retry))
                        }
                    }
                }
                else -> {
                    val profile = uiState.userDetail?.profile
                    if (profile != null) {
                        val myPlaylists = uiState.userPlaylists.filter { it.creator?.userId == profile.userId }
                        val subscribedPlaylists = uiState.userPlaylists.filter { it.creator?.userId != profile.userId }

                    ProfileContent(
                        profile = profile,
                        level = uiState.userDetail?.level ?: 0,
                        listenSongs = uiState.userDetail?.listenSongs ?: 0,
                        listenHours = uiState.listenHours,
                        createdPlaylists = myPlaylists,
                        subscribedPlaylists = subscribedPlaylists,
                        onPlaylistClick = onPlaylistClick,
                        onFollowsClick = onFollowsClick,
                        onDeletePlaylist = { pid -> viewModel.deletePlaylist(pid) }
                    )
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(onDismiss: () -> Unit, onConfirm: (String, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.create_playlist)) },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(Res.string.playlist_name)) })
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                    Text(stringResource(Res.string.public))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, isPublic) }) { Text(stringResource(Res.string.create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        }
    )
}

@Composable
fun ProfileContent(
    profile: UserProfile,
    level: Int,
    listenSongs: Int,
    listenHours: Int,
    createdPlaylists: List<Playlist>,
    subscribedPlaylists: List<Playlist>,
    onPlaylistClick: (Long) -> Unit,
    onFollowsClick: () -> Unit,
    onDeletePlaylist: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            ProfileHeader(profile, level, listenSongs, listenHours, onFollowsClick)
        }

        item {
             Column {
                 Text(
                    stringResource(Res.string.created_playlists_format, createdPlaylists.size),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                 )
                 Column(Modifier.padding(vertical = 16.dp)) {
                    createdPlaylists.forEach { playlist ->
                        PlaylistRowItem(playlist, onClick = onPlaylistClick, onDelete = { onDeletePlaylist(playlist.id) })
                    }
                 }
             }
        }

        if (subscribedPlaylists.isNotEmpty()) {
            item {
                 Column {
                     Text(
                        stringResource(Res.string.subscribed_playlists_format, subscribedPlaylists.size),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                     )
                     Column(Modifier.padding(vertical = 16.dp)) {
                        subscribedPlaylists.forEach { playlist ->
                            // Subscribed playlists usually cannot be deleted by user here (only unsubscribed), passing null to onDelete hides the delete option
                            PlaylistRowItem(playlist, onClick = onPlaylistClick, onDelete = null)
                        }
                     }
                 }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    profile: UserProfile,
    level: Int,
    listenSongs: Int,
    listenHours: Int,
    onFollowsClick: () -> Unit
) {
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f) // Take remaining space
            ) {
                Text(
                    text = profile.nickname,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Text(stringResource(Res.string.lv_format, level), modifier = Modifier.padding(4.dp))
                    }
                    Text(
                        stringResource(Res.string.id_format, profile.userId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                if (profile.signature?.isNotBlank() == true) {
                    Text(
                        profile.signature,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2
                    )
                }
                Text(stringResource(Res.string.listening_time_format, listenHours), style = MaterialTheme.typography.bodySmall)

                Spacer(Modifier.height(4.dp))

                Button(onClick = { onFollowsClick() }) {
                    Text(stringResource(Res.string.following_format, profile.follows))
                }
            }
        }

    }
}

@Composable
fun PlaylistRowItem(playlist: Playlist, onClick: (Long) -> Unit, onDelete: (() -> Unit)? = null) {
    var showMenu by remember { mutableStateOf(false) }

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
                text = stringResource(Res.string.listening_format, playlist.trackCount),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Spacer(Modifier.weight(1f))

        if (onDelete != null) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(Res.string.more))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.delete)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, stringResource(Res.string.delete)) }
                    )
                }
            }
        }
    }
}
