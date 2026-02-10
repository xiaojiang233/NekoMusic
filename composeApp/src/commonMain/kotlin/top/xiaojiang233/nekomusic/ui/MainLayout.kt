package top.xiaojiang233.nekomusic.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import top.xiaojiang233.nekomusic.ui.settings.SettingsScreen

@Composable
fun MainLayout() {
    val navController = rememberNavController()
    // Observe player state to determine if we need bottom padding for NavHost
    val playbackState by AudioManager.state.collectAsState()
    val isPlayerVisible = playbackState.currentSong != null

    var showFullPlayer by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        // Main app UI: navigation rail + content. Hidden when full player is shown.
        AnimatedVisibility(
            visible = !showFullPlayer,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 })
        ) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    NavigationRailItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text(stringResource(Res.string.home)) },
                        selected = currentDestination?.hierarchy?.any { it.route == "home" } == true,
                        onClick = {
                            navController.navigate("home") {
                                popUpTo("home")
                                launchSingleTop = true
                            }
                        }
                    )

                    NavigationRailItem(
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        label = { Text(stringResource(Res.string.settings)) },
                        selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                        onClick = {
                            navController.navigate("settings") {
                                popUpTo("home")
                                launchSingleTop = true
                            }
                        }
                    )

                    NavigationRailItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                        label = { Text(stringResource(Res.string.profile)) },
                        selected = currentDestination?.hierarchy?.any { it.route == "profile" } == true,
                        onClick = {
                            navController.navigate("profile") {
                                popUpTo("home")
                                launchSingleTop = true
                            }
                        }
                    )
                }

                Box(Modifier.weight(1f)) {
                    Column {
                        NavHost(
                            navController,
                            startDestination = "home",
                            modifier = Modifier.weight(1f) // Fill available space
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
                            composable("settings") {
                                SettingsScreen()
                            }
                            composable("profile") {
                                ProfileScreen(
                                    onPlaylistClick = { id ->
                                        navController.navigate("playlist/$id")
                                    }
                                )
                            }
                            composable(
                                "playlist/{id}",
                                arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType })
                            ) { backStackEntry ->
                                val playlistId = backStackEntry.arguments?.getLong("id") ?: return@composable
                                top.xiaojiang233.nekomusic.ui.playlist.PlaylistScreen(
                                    playlistId = playlistId,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                            composable(
                                "daily_songs",
                            ) {
                                top.xiaojiang233.nekomusic.ui.daily.DailySongsScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                            composable("search") {
                                top.xiaojiang233.nekomusic.ui.search.SearchScreen(
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                        }
                    }

                    // Floating Player Bar: only show when not showing full player
                    PlayerBar(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onClick = { showFullPlayer = true }
                    )
                }
            }
        }

        // Full-screen Player Overlay (slides from bottom)
        AnimatedVisibility(
            visible = showFullPlayer,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            // Full-screen player covers entire window
            top.xiaojiang233.nekomusic.ui.player.PlayerScreen(onBack = { showFullPlayer = false })
        }
    }
}
