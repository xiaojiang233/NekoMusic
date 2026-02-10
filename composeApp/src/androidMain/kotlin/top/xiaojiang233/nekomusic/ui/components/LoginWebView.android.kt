package top.xiaojiang233.nekomusic.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun LoginWebView(
    onCookieFound: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberWebViewState("https://music.163.com/login")

    // Polling for cookie
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(2000)
            val cookies = state.cookieManager.getCookies("https://music.163.com")
            val cookieStr = cookies.joinToString("; ") { cookie -> "${cookie.name}=${cookie.value}" }

            if (cookieStr.contains("MUSIC_U") || cookieStr.contains("__csrf")) {
                onCookieFound(cookieStr)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Login via Netease") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                )
            }
        ) { padding ->
            WebView(
                state = state,
                modifier = Modifier.fillMaxSize().padding(padding),
                onCreated = { webView ->
                    webView.settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }
            )
        }
    }
}

