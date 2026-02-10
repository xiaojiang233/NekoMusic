package top.xiaojiang233.nekomusic.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import nekomusic.composeapp.generated.resources.Res
import nekomusic.composeapp.generated.resources.cancel
import nekomusic.composeapp.generated.resources.desktop_login_tip
import nekomusic.composeapp.generated.resources.login_step_1
import nekomusic.composeapp.generated.resources.login_step_2
import nekomusic.composeapp.generated.resources.login_step_3
import nekomusic.composeapp.generated.resources.manual_login_required
import nekomusic.composeapp.generated.resources.open_browser
import org.jetbrains.compose.resources.stringResource
import java.awt.Desktop
import java.net.URI

@Composable
actual fun LoginWebView(
    onCookieFound: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.manual_login_required)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.desktop_login_tip))
                Text(stringResource(Res.string.login_step_1))
                Text(stringResource(Res.string.login_step_2))
                Text(stringResource(Res.string.login_step_3))
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
                Text(stringResource(Res.string.open_browser))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
