package top.xiaojiang233.nekomusic

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*

import top.xiaojiang233.nekomusic.settings.SettingsManager
import top.xiaojiang233.nekomusic.ui.MainLayout
import top.xiaojiang233.nekomusic.ui.setup.SetupScreen
import top.xiaojiang233.nekomusic.ui.theme.createColorSchemeFromSeed
import top.xiaojiang233.nekomusic.ui.theme.getDynamicColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import top.xiaojiang233.nekomusic.ui.components.ToastOverlay

@Composable

fun App() {
    val isFirstLaunchCompleted by SettingsManager.isFirstLaunchCompleted().collectAsState(initial = false)
    val isDark by SettingsManager.isDarkTheme().collectAsState(initial = false)
    val seedColorLong by SettingsManager.getThemeSeedColor().collectAsState(initial = 0L)

    val dynamicScheme = getDynamicColorScheme(isDark)

    val colorScheme = if (seedColorLong != null && seedColorLong != 0L) {
        createColorSchemeFromSeed(Color(seedColorLong!!.toInt()), isDark)
    } else {
        dynamicScheme ?: if(isDark) darkColorScheme() else lightColorScheme()
    }

    MaterialTheme(colorScheme = colorScheme) {
        Surface {
            Box {
                if (isFirstLaunchCompleted) {
                    MainLayout()
                } else {
                    SetupScreen(onFinished = { })
                }

                // Show ToastOverlay (Visible logic handled inside)
                ToastOverlay()
            }
        }
    }
}
