package top.xiaojiang233.nekomusic

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "NekoMusic",
    ) {
        App()
    }
}