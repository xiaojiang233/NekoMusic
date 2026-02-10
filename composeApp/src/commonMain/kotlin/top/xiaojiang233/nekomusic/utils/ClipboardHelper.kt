package top.xiaojiang233.nekomusic.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@Composable
fun CopyHelper(text: String, label: String = "Text") {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    clipboardManager.setText(AnnotatedString(text))
    // Could show a toast here if we had a global toast manager
}

