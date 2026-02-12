package top.xiaojiang233.nekomusic.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.xiaojiang233.nekomusic.utils.ToastManager

@Composable
fun ToastOverlay() {
    val messages by ToastManager.messages.collectAsState()

    // Overlay Box
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            messages.forEach { message ->
                ToastItem(message.message)
            }
        }
    }
}

@Composable
fun ToastItem(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 6.dp,
        modifier = Modifier.widthIn(max = 300.dp)
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
