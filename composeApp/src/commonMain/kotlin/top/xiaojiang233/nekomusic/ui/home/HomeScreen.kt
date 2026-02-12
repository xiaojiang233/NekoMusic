package top.xiaojiang233.nekomusic.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import top.xiaojiang233.nekomusic.model.Banner
import top.xiaojiang233.nekomusic.model.Playlist
import top.xiaojiang233.nekomusic.model.PrivateRoamingItem
import top.xiaojiang233.nekomusic.viewmodel.HomeViewModel
import top.xiaojiang233.nekomusic.player.PlayerController
import nekomusic.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel { HomeViewModel() },
    onPlaylistClick: (Long) -> Unit = {},
    onDailySongsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onOpenPlayer: () -> Unit = {} // New callback for opening player
) {
    val uiState by viewModel.uiState.collectAsState()

    // Filter "Private Radar" playlists
    val privateRadarPlaylists = uiState.recommendedPlaylists.filter {
        it.name.contains("Private Radar", ignoreCase = true) || it.name.contains("私人雷达")
    }
    // Avoid duplicates if specific radar playlists are already in recommendations
    val otherRadarPlaylists = uiState.otherRadarPlaylists.filter { other ->
        privateRadarPlaylists.none { it.id == other.id }
    }

    val otherPlaylists = uiState.recommendedPlaylists.filterNot {
        it.name.contains("Private Radar", ignoreCase = true) ||
        it.name.contains("私人雷达") ||
        uiState.otherRadarPlaylists.any { other -> other.id == it.id }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(String.format(stringResource(Res.string.error_format), uiState.error))
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.loadData() }) {
                    Text(stringResource(Res.string.retry))
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Banner
                if (uiState.showBanner && uiState.banners.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        BannerSection(uiState.banners)
                    }
                }

                // Top Cards Section (Daily + Private Roaming + etc)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Daily Recommendations
                        DailyRecommendationsCard(
                            onClick = onDailySongsClick,
                            modifier = Modifier.width(160.dp).height(160.dp)
                        )

                        // Private FM
                        QuickAccessCard(
                            title = stringResource(Res.string.private_fm),
                            icon = Icons.Default.Radio,
                            onClick = {
                                PlayerController.playPersonalFm()
                                onOpenPlayer()
                            },
                            modifier = Modifier.width(160.dp).height(160.dp),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )

                        // Liked Songs
                        QuickAccessCard(
                            title = stringResource(Res.string.liked_songs),
                            icon = Icons.Default.Favorite,
                            onClick = { viewModel.openLikedSongsPlaylist(onPlaylistClick) },
                            modifier = Modifier.width(160.dp).height(160.dp),
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )

                        // Private Radar Playlists from Recommendations
                        privateRadarPlaylists.forEach { playlist ->
                            RadarPlaylistItem(
                                playlist = playlist,
                                onClick = onPlaylistClick,
                                modifier = Modifier.width(160.dp).height(160.dp)
                            )
                        }

                        // Other Radar Playlists (Explicitly fetched)
                        otherRadarPlaylists.forEach { playlist ->
                            RadarPlaylistItem(
                                playlist = playlist,
                                onClick = onPlaylistClick,
                                modifier = Modifier.width(160.dp).height(160.dp)
                            )
                        }
                    }
                }

                // Recommended Playlists Title
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(Res.string.recommended_playlists),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Playlists Grid
                items(otherPlaylists) { playlist ->
                    PlaylistItem(playlist, onPlaylistClick)
                }
            }
        }
    }
}

@Composable
fun QuickAccessCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DailyRecommendationsCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally // Center content like screenshot
        ) {
            Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.daily_recommendations),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PrivateContentCard(item: PrivateRoamingItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
         Box(Modifier.fillMaxSize()) {
             AsyncImage(
                 model = item.picUrl,
                 contentDescription = item.name,
                 contentScale = ContentScale.Crop,
                 modifier = Modifier.fillMaxSize()
             )
             // Overlay Text
              Box(
                 modifier = Modifier
                     .align(Alignment.BottomStart)
                     .fillMaxWidth()
                     .background(Color.Black.copy(alpha = 0.4f))
                     .padding(8.dp)
             ) {
                 Text(
                     text = item.name,
                     style = MaterialTheme.typography.titleSmall,
                     color = Color.White,
                     maxLines = 2,
                     overflow = TextOverflow.Ellipsis
                 )
             }

             // Top Label "Private Roaming" or Copywriter
             item.copywriter?.let {
                  Text(
                     text = it,
                     style = MaterialTheme.typography.labelSmall,
                     color = Color.White,
                     modifier = Modifier.align(Alignment.TopStart).padding(4.dp).background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(horizontal=4.dp)
                 )
             }
         }
    }
}

@Composable
fun RadarPlaylistItem(playlist: Playlist, onClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable { onClick(playlist.id) },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = playlist.picUrl,
                contentDescription = playlist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Overlay Text
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(8.dp)
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BannerSection(banners: List<Banner>) {
    if (banners.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { banners.size })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(8.dp))
    ) { page ->
        val banner = banners[page]
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = banner.displayImage,
                contentDescription = banner.typeTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            banner.typeTitle?.let {
                Text(
                    text = it,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(
                            (if (banner.titleColor == "red") Color.Red else Color.Blue).copy(alpha = 0.8f),
                            RoundedCornerShape(topStart = 8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// Extension to handle mismatch in naming if any
val Banner.displayImage: String
    get() = pic ?: imageUrl ?: url ?: ""


@Composable
fun PlaylistItem(playlist: Playlist, onClick: (Long) -> Unit, modifier: Modifier = Modifier.width(120.dp)) {
    Column(
        modifier = modifier
            .clickable { onClick(playlist.id) }
    ) {
        AsyncImage(
            model = playlist.picUrl,
            contentDescription = playlist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
        )
        Text(
            text = playlist.name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
