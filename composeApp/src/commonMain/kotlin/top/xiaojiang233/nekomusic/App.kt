package top.xiaojiang233.nekomusic

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*

import top.xiaojiang233.nekomusic.settings.SettingsManager
import top.xiaojiang233.nekomusic.ui.MainLayout
import top.xiaojiang233.nekomusic.ui.setup.SetupScreen

@Composable

fun App() {
    val isFirstLaunchCompleted by SettingsManager.isFirstLaunchCompleted().collectAsState(initial = false)
    val isDark by SettingsManager.isDarkTheme().collectAsState(initial = false)

    MaterialTheme(colorScheme = if(isDark) darkColorScheme() else lightColorScheme()) {
        Surface {
           if (isFirstLaunchCompleted) {
               MainLayout()
           } else {
               SetupScreen(onFinished = { })
           }
        }
    }
}

