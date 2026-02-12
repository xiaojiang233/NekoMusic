package top.xiaojiang233.nekomusic.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Filter
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
import top.xiaojiang233.nekomusic.utils.FavoritesManager
import top.xiaojiang233.nekomusic.utils.thumbnail
import top.xiaojiang233.nekomusic.viewmodel.PlaylistViewModel
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.ui.components.AddToPlaylistDialog
import top.xiaojiang233.nekomusic.api.NeteaseApi
import nekomusic.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Suppress("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlistId: Long,
    onBackClick: () -> Unit,
    onArtistClick: (Long) -> Unit = {},
    viewModel: PlaylistViewModel = viewModel { PlaylistViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylist(playlistId)
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val likedIds by FavoritesManager.likedSongIds.collectAsState()

    // Retrieve subscription state from playlist details
    val isSubscribed = uiState.playlist?.subscribed ?: false

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
                title = { Text(stringResource(Res.string.playlist_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
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
                Text(stringResource(Res.string.error_format, uiState.error!!))
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
                BoxWithConstraints {
                    val isNarrowScreen = maxWidth < 600.dp

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
                                val coverSize = if (isNarrowScreen) 120.dp else 160.dp
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(24.dp)
                                        .padding(bottom = 32.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    AsyncImage(
                                        model = playlist.coverImgUrl.thumbnail(200),
                                        contentDescription = stringResource(Res.string.cover),
                                        modifier = Modifier.size(coverSize).clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(24.dp))
                                    Column {
                                        Text(
                                            playlist.name,
                                            style = if (isNarrowScreen)
                                                MaterialTheme.typography.titleMedium
                                            else
                                                MaterialTheme.typography.headlineLarge,
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
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                            }
                                        }
                                        if (!playlist.description.isNullOrEmpty()) {
                                            Spacer(Modifier.height(12.dp))
                                            Text(
                                                playlist.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(Modifier.height(12.dp))

                                        // Actions: compact on narrow screens, full buttons on wide screens
                                        val isCreator = playlist.creator?.userId == uiState.currentUserId
                                        val isLikedSongs = playlist.specialType == 5

                                        if (isNarrowScreen) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Play icon button
                                                FilledIconButton(
                                                    onClick = {
                                                        if (searchQuery.isEmpty()) {
                                                            PlayerController.playList(
                                                                songs = filteredTracks,
                                                                sourceId = playlist.id,
                                                                totalTrackIds = playlist.trackIds.map { it.id }
                                                            )
                                                        } else {
                                                            PlayerController.playList(filteredTracks, sourceId = playlist.id)
                                                        }
                                                    },
                                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    modifier = Modifier.size(44.dp)
                                                ) {
                                                    Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(Res.string.play), tint = MaterialTheme.colorScheme.onPrimary)
                                                }

                                                // Hide Subscribe button for Liked Songs or if user is creator
                                                if (!isLikedSongs && !isCreator) {
                                                    Spacer(Modifier.width(8.dp))

                                                    // Subscribe / Unsubscribe as add/remove
                                                    FilledIconButton(
                                                        onClick = {
                                                            scope.launch {
                                                                NeteaseApi.subscribePlaylist(playlist.id, !isSubscribed)
                                                                viewModel.loadPlaylist(playlistId)
                                                            }
                                                        },
                                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (isSubscribed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary),
                                                        modifier = Modifier.size(44.dp)
                                                    ) {
                                                        Icon(if (isSubscribed) Icons.Filled.Remove else Icons.Filled.Add, contentDescription = if (isSubscribed) stringResource(Res.string.unsubscribe) else stringResource(Res.string.subscribe), tint = MaterialTheme.colorScheme.onSecondary)
                                                    }
                                                }
                                            }
                                        } else {
                                            Button(onClick = {
                                                if (searchQuery.isEmpty()) {
                                                    PlayerController.playList(
                                                        songs = filteredTracks,
                                                        sourceId = playlist.id,
                                                        totalTrackIds = playlist.trackIds.map { it.id }
                                                    )
                                                } else {
                                                    PlayerController.playList(filteredTracks, sourceId = playlist.id)
                                                }
                                            }) {
                                                Icon(Icons.Filled.PlayArrow, null)
                                                Spacer(Modifier.width(8.dp))
                                                Text(stringResource(Res.string.play_all))
                                            }

                                            // Subscription Button
                                            // Hide if playlist creator is current user or if it's the Liked Songs list
                                            if (!isCreator && !isLikedSongs) {
                                                Spacer(Modifier.height(8.dp))
                                                OutlinedButton(onClick = {
                                                    scope.launch {
                                                        NeteaseApi.subscribePlaylist(playlist.id, !isSubscribed)
                                                        viewModel.loadPlaylist(playlistId) // Refresh
                                                    }
                                                }) {
                                                    Text(if (isSubscribed) stringResource(Res.string.unsubscribe) else stringResource(Res.string.subscribe))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text(stringResource(Res.string.filter_songs)) },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                leadingIcon = { Icon(Icons.Default.Filter, null) },
                                singleLine = true
                            )
                        }

                        // Songs
                        itemsIndexed(filteredTracks) { index, song ->
                            SongListItem(
                                index = index + 1,
                                song = song,
                                onClick = { PlayerController.playList(filteredTracks, index, sourceId = playlist.id) },
                                onArtistClick = onArtistClick,
                                isLiked = likedIds.contains(song.id),
                                onLikeClick = { id -> scope.launch { FavoritesManager.toggleLike(id) } }
                            )
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
}

@Composable
fun SongListItem(
    index: Int,
    song: Song,
    onClick: () -> Unit = {},
    onArtistClick: ((Long) -> Unit)? = null,
    isLiked: Boolean = false,
    onLikeClick: ((Long) -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Helper to trigger copy - in Compose usage usually triggers side effect or directly calls platform
    // We will just use clipboard locally if possible or simple text copy
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    if (showAddDialog) {
        AddToPlaylistDialog(songId = song.id, onDismiss = { showAddDialog = false })
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // Detect screen width for responsive design - using this.maxWidth from BoxWithConstraintsScope
        val isCompact = this.maxWidth < 600.dp

        // Use compact sizes for narrow screens
        val itemHeight = if (isCompact) 56.dp else 64.dp
        val coverSize = if (isCompact) 40.dp else 48.dp
        val indexWidth = if (isCompact) 32.dp else 40.dp
        val horizontalPadding = if (isCompact) 12.dp else 16.dp
        val verticalPadding = if (isCompact) 4.dp else 0.dp
        val titleStyle = if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
        val indexStyle = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium

        SongListItemContent(
            index = index,
            song = song,
            onClick = onClick,
            onArtistClick = onArtistClick,
            isLiked = isLiked,
            onLikeClick = onLikeClick,
            itemHeight = itemHeight,
            coverSize = coverSize,
            indexWidth = indexWidth,
            horizontalPadding = horizontalPadding,
            verticalPadding = verticalPadding,
            titleStyle = titleStyle,
            indexStyle = indexStyle,
            clipboardManager = clipboardManager,
            showMenu = showMenu,
            onMenuChange = { showMenu = it },
            onShowAddDialog = { showAddDialog = true }
        )
    }
}

@Composable
private fun SongListItemContent(
    index: Int,
    song: Song,
    onClick: () -> Unit,
    onArtistClick: ((Long) -> Unit)?,
    isLiked: Boolean,
    onLikeClick: ((Long) -> Unit)?,
    itemHeight: androidx.compose.ui.unit.Dp,
    coverSize: androidx.compose.ui.unit.Dp,
    indexWidth: androidx.compose.ui.unit.Dp,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    verticalPadding: androidx.compose.ui.unit.Dp,
    titleStyle: androidx.compose.ui.text.TextStyle,
    indexStyle: androidx.compose.ui.text.TextStyle,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    showMenu: Boolean,
    onMenuChange: (Boolean) -> Unit,
    onShowAddDialog: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$index",
            style = indexStyle,
            color = Color.Gray,
            modifier = Modifier.width(indexWidth)
        )

        // Lazy load cover
        AsyncImage(
            model = song.al.cover.thumbnail(100),
            contentDescription = null,
            modifier = Modifier.size(coverSize).clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(horizontalPadding))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                style = titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (onArtistClick != null) {
                Text(
                    text = buildString {
                        song.ar.forEachIndexed { i, artist ->
                            append(artist.name)
                            if (i < song.ar.size - 1) append(", ")
                        }
                        append(" - ${song.al.name}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "${song.ar.joinToString(", ") { it.name }} - ${song.al.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Duration
        Text(
            text = formatDuration(song.dt),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        if (onLikeClick != null) {
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { onLikeClick(song.id) }) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isLiked) stringResource(Res.string.unlike) else stringResource(Res.string.like),
                    tint = if (isLiked) Color.Red else Color.Gray
                )
            }
        }

        // More Menu
        Box {
            IconButton(onClick = { onMenuChange(true) }) {
                Icon(Icons.Default.MoreVert, stringResource(Res.string.more), tint = Color.Gray)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { onMenuChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.copy_id)) },
                    onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(song.id.toString()))
                        onMenuChange(false)
                    },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, stringResource(Res.string.copy_id)) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.share_link)) },
                    onClick = {
                         clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("https://music.163.com/#/song?id=${song.id}"))
                         onMenuChange(false)
                    },
                    leadingIcon = { Icon(Icons.Default.Share, stringResource(Res.string.share_link)) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.add_to_playlist)) },
                    onClick = {
                        onMenuChange(false)
                        onShowAddDialog()
                    },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, stringResource(Res.string.add_to_playlist)) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.play_next)) },
                    onClick = {
                        PlayerController.addToNext(song)
                        onMenuChange(false)
                    },
                    leadingIcon = { Icon(Icons.Default.Add, stringResource(Res.string.play_next)) }
                )
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}
