package top.xiaojiang233.nekomusic.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.net.URI

@Composable
actual fun LoginWebView(
    onCookieFound: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Login Required") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Desktop version requires manual login due to system limitations.")
                Text("1. Click 'Open Browser' to log in at music.163.com")
                Text("2. Copy the cookie as per instructions.")
                Text("3. Paste it in the input field manually.")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        Desktop.getDesktop().browse(URI("https://music.163.com/login"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    onDismiss()
                }
            ) {
                Text("Open Browser")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


