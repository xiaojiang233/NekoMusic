package top.xiaojiang233.nekomusic.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun LoginWebView(
    onCookieFound: (String) -> Unit,
    onDismiss: () -> Unit
)

