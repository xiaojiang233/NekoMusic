package top.xiaojiang233.nekomusic.ui

import ArtistScreen
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import nekomusic.composeapp.generated.resources.Res
import nekomusic.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import top.xiaojiang233.nekomusic.player.AudioManager
import top.xiaojiang233.nekomusic.ui.components.PlayerBar
import top.xiaojiang233.nekomusic.ui.home.HomeScreen
import top.xiaojiang233.nekomusic.ui.profile.ProfileScreen
import top.xiaojiang233.nekomusic.ui.profile.FollowsScreen
import top.xiaojiang233.nekomusic.ui.settings.SettingsScreen
import top.xiaojiang233.nekomusic.ui.comment.CommentScreen

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun MainLayout() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val playbackState by AudioManager.state.collectAsState()

    // Check if we are on the player screen to hide bars
    val isPlayerScreen = currentRoute == "player"

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Determine layout based on screen width
        val useBottomNav = maxWidth < 600.dp // Use bottom nav for narrow screens (like phone portrait)

        // Main app UI: navigation + content.
        if (useBottomNav) {
            // Bottom navigation layout for narrow screens
            Column(Modifier.fillMaxSize()) {
                // Main content area
                Box(Modifier.weight(1f)) {
                    // Mobile NavHost
                    NavHost(
                        navController,
                        startDestination = "home",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("home") {
                            HomeScreen(
                                onPlaylistClick = { id ->
                                    navController.navigate("playlist/$id")
                                },
                                onDailySongsClick = {
                                    navController.navigate("daily_songs")
                                },
                                onSearchClick = {
                                    navController.navigate("search")
                                }
                            )
                        }
                        composable("settings") { SettingsScreen() }
                        composable("profile") {
                            // The ProfileScreen handles fetching.
                            ProfileScreen(
                                onPlaylistClick = { id -> navController.navigate("playlist/$id") },
                                onArtistClick = { id -> navController.navigate("artist/$id") },
                                onFollowsClick = { navController.navigate("follows") }
                            )
                        }
                        composable("follows") {
                            FollowsScreen(
                                onBackClick = { navController.popBackStack() },
                                onArtistClick = { id -> navController.navigate("artist/$id") }
                            )
                        }
                        composable("search") {
                            top.xiaojiang233.nekomusic.ui.search.SearchScreen(
                                onBackClick = { navController.popBackStack() },
                                onPlaylistClick = { id -> navController.navigate("playlist/$id") },
                                onArtistClick = { id -> navController.navigate("artist/$id") },
                                onAlbumClick = { id -> navController.navigate("album/$id") }
                            )
                        }
                        composable(
                            "playlist/{id}",
                            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType })
                        ) { backStackEntry ->
                            val playlistId = backStackEntry.arguments?.getLong("id") ?: return@composable
                            top.xiaojiang233.nekomusic.ui.playlist.PlaylistScreen(
                                playlistId = playlistId,
                                onBackClick = { navController.popBackStack() },
                                onArtistClick = { id -> navController.navigate("artist/$id") }
                            )
                        }
                        composable("daily_songs") {
                            top.xiaojiang233.nekomusic.ui.daily.DailySongsScreen(onBackClick = { navController.popBackStack() })
                        }
                        composable(
                            "artist/{id}",
                            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType })
                        ) { backStackEntry ->
                            val artistId = backStackEntry.arguments?.getLong("id") ?: return@composable
                            ArtistScreen(
                                artistId = artistId,
                                onBackClick = { navController.popBackStack() },
                                onAlbumClick = { id -> navController.navigate("album/$id") }
                            )
                        }
                        composable(
                            "album/{id}",
                            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType })
                        ) { backStackEntry ->
                            val albumId = backStackEntry.arguments?.getLong("id") ?: return@composable
                            top.xiaojiang233.nekomusic.ui.album.AlbumScreen(albumId = albumId, onBackClick = { navController.popBackStack() })
                        }
                        // Player Screen Route
                        composable(
                            "player",
                            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
                            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) },
                            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
                        ) {
                            top.xiaojiang233.nekomusic.ui.player.PlayerScreen(onBack = { navController.popBackStack() })
                        }
                        composable(
                            "comment/{id}",
                            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType })
                        ) { backStackEntry ->
                            val songId = backStackEntry.arguments?.getLong("id") ?: return@composable
                            CommentScreen(songId = songId, onBackClick = { navController.popBackStack() })
                        }
                    }

                    // Floating Player Bar (Hidden on Player Screen)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = playbackState.currentSong != null && !isPlayerScreen,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    ) {
                        PlayerBar(
                            onClick = { navController.navigate("player") },
                            onCommentClick = { id -> navController.navigate("comment/$id") }
                        )
                    }
                }

                // Bottom navigation bar (Hidden on Player Screen)
                if (!isPlayerScreen) {
                    NavigationBar {
                        val currentDestination = navBackStackEntry?.destination

                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                            label = { Text(stringResource(Res.string.home)) },
                            selected = currentDestination?.hierarchy?.any { it.route == "home" } == true,
                            onClick = { navController.navigate("home") { popUpTo("home"); launchSingleTop = true } }
                        )

                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                            label = { Text(stringResource(Res.string.settings)) },
                            selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                            onClick = { navController.navigate("settings") { popUpTo("home"); launchSingleTop = true } }
                        )

                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                            label = { Text(stringResource(Res.string.profile)) },
                            selected = currentDestination?.hierarchy?.any { it.route == "profile" } == true,
                            onClick = { navController.navigate("profile") { popUpTo("home"); launchSingleTop = true } }
                        )
                    }
                }
            }
        } else {
            // Side navigation layout for wide screens (original layout)
            Row(Modifier.fillMaxSize()) {
                // Navigation Rail (Hidden on full screen Player)
                if (!isPlayerScreen) {
                    NavigationRail {
                        val currentDestination = navBackStackEntry?.destination

                        NavigationRailItem(
                            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                            label = { Text(stringResource(Res.string.home)) },
                            selected = currentDestination?.hierarchy?.any { it.route == "home" } == true,
                            onClick = { navController.navigate("home") { popUpTo("home"); launchSingleTop = true } }
                        )

                        NavigationRailItem(
                            icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                            label = { Text(stringResource(Res.string.settings)) },
                            selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                            onClick = { navController.navigate("settings") { popUpTo("home"); launchSingleTop = true } }
                        )

                        NavigationRailItem(
                            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                            label = { Text(stringResource(Res.string.profile)) },
                            selected = currentDestination?.hierarchy?.any { it.route == "profile" } == true,
                            onClick = { navController.navigate("profile") { popUpTo("home"); launchSingleTop = true } }
                        )
                    }
                }

                Box(Modifier.weight(1f)) {
                    Column {
                        // Desktop NavHost
                        NavHost(
                            navController,
                            startDestination = "home",
                            modifier = Modifier.weight(1f)
                        ) {
                            composable("home") {
                                HomeScreen(
                                    onPlaylistClick = { id -> navController.navigate("playlist/$id") },
                                    onDailySongsClick = { navController.navigate("daily_songs") },
                                    onSearchClick = { navController.navigate("search") }
                                )
                            }
                            composable("settings") { SettingsScreen() }
                            composable("profile") {
                                // The ProfileScreen handles fetching.
                                ProfileScreen(
                                    onPlaylistClick = { id -> navController.navigate("playlist/$id") },
                                    onArtistClick = { id -> navController.navigate("artist/$id") },
                                    onFollowsClick = { navController.navigate("follows") }
                                )
                            }
                            composable("follows") {
                                FollowsScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onArtistClick = { id -> navController.navigate("artist/$id") }
                                )
                            }
                            composable("search") {
                                top.xiaojiang233.nekomusic.ui.search.SearchScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onPlaylistClick = { id -> navController.navigate("playlist/$id") },
                                    onArtistClick = { id -> navController.navigate("artist/$id") },
                                    onAlbumClick = { id -> navController.navigate("album/$id") }
                                )
                            }
                            composable(
                                "playlist/{id}",
                                arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType })
                            ) { backStackEntry ->
                                val playlistId = backStackEntry.arguments?.getLong("id") ?: return@composable
                                top.xiaojiang233.nekomusic.ui.playlist.PlaylistScreen(
                                    playlistId = playlistId,
                                    onBackClick = { navController.popBackStack() },
                                    onArtistClick = { id -> navController.navigate("artist/$id") }
                                )
                            }
                            composable("daily_songs") {
                                top.xiaojiang233.nekomusic.ui.daily.DailySongsScreen(onBackClick = { navController.popBackStack() })
                            }
                            composable(
                                "artist/{id}",
                                arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType })
                            ) { backStackEntry ->
                                val artistId = backStackEntry.arguments?.getLong("id") ?: return@composable
                                ArtistScreen(
                                    artistId = artistId,
                                    onBackClick = { navController.popBackStack() },
                                    onAlbumClick = { id -> navController.navigate("album/$id") }
                                )
                            }
                            composable(
                                "album/{id}",
                                arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType })
                            ) { backStackEntry ->
                                val albumId = backStackEntry.arguments?.getLong("id") ?: return@composable
                                top.xiaojiang233.nekomusic.ui.album.AlbumScreen(
                                    albumId = albumId,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                            // Player Screen Route (Desktop/Tablet)
                            composable(
                                "player",
                                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
                                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) },
                                popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
                            ) {
                                top.xiaojiang233.nekomusic.ui.player.PlayerScreen(onBack = { navController.popBackStack() })
                            }
                            composable(
                                "comment/{id}",
                                arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType })
                            ) { backStackEntry ->
                                val songId = backStackEntry.arguments?.getLong("id") ?: return@composable
                                CommentScreen(songId = songId, onBackClick = { navController.popBackStack() })
                            }
                        }
                    }

                    // Floating Player Bar (Hidden on Player Screen)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = playbackState.currentSong != null && !isPlayerScreen,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                    ) {
                        PlayerBar(
                            onClick = { navController.navigate("player") },
                            onCommentClick = { id -> navController.navigate("comment/$id") }
                        )
                    }
                }
            }
        }
    }
}
