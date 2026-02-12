package top.xiaojiang233.nekomusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import top.xiaojiang233.nekomusic.settings.SettingsManager
import top.xiaojiang233.nekomusic.utils.LocalFullscreenHandler
import java.awt.Dimension

fun main() {
    // Initialize app logging before anything else
    try { initAppLogging() } catch (_: Exception) {}

    System.setProperty("skiko.renderApi", "OPENGL")
    application {
        val windowState = rememberWindowState(width = 1280.dp, height = 800.dp)
        val isDark by SettingsManager.isDarkTheme().collectAsState(initial = true)

        Window(
            onCloseRequest = ::exitApplication,
            title = "NekoMusic",
            state = windowState,
            undecorated = true,
            onPreviewKeyEvent = { event ->
                if ((event.key == Key.F11 || (event.key == Key.Escape && windowState.placement == WindowPlacement.Fullscreen))
                    && event.type == KeyEventType.KeyUp) {
                    windowState.placement = if (windowState.placement == WindowPlacement.Fullscreen) {
                        WindowPlacement.Floating
                    } else {
                        WindowPlacement.Fullscreen
                    }
                    true
                } else {
                    false
                }
            }
        ) {
            window.minimumSize = Dimension(1024, 640)

            val toggleFullscreen = {
                windowState.placement = if (windowState.placement == WindowPlacement.Fullscreen) {
                    WindowPlacement.Floating
                } else {
                    WindowPlacement.Fullscreen
                }
            }

            CompositionLocalProvider(LocalFullscreenHandler provides toggleFullscreen) {
                MaterialTheme(colorScheme = if(isDark) darkColorScheme() else lightColorScheme()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        if (windowState.placement != WindowPlacement.Fullscreen) {
                            this@Window.WindowDraggableArea {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "NekoMusic",
                                        modifier = Modifier.padding(start = 16.dp),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row {
                                        IconButton(onClick = { windowState.isMinimized = true }) {
                                            Icon(Icons.Filled.Remove, contentDescription = "Minimize")
                                        }
                                        IconButton(onClick = ::exitApplication) {
                                            Icon(Icons.Filled.Close, contentDescription = "Close")
                                        }
                                    }
                                }
                            }
                        }

                        // Main Content
                        App()
                    }
                }
            }
        }
    }
}
