package top.xiaojiang233.nekomusic.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.settings.SettingsManager

@Composable
fun SetupScreen(onFinished: () -> Unit) {
    var apiUrl by remember { mutableStateOf("") }
    var isDark by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Load initial values (optional, or rely on defaults)
    LaunchedEffect(Unit) {
        // Can pre-fill if needed
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to NekoMusic", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Text("Choose Theme")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Light")
            Switch(checked = isDark, onCheckedChange = { isDark = it })
            Text("Dark")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = apiUrl,
            onValueChange = { apiUrl = it },
            label = { Text("API URL") },
            placeholder = { Text("http://localhost:3000") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    SettingsManager.setDarkTheme(isDark)
                    if (apiUrl.isNotBlank()) {
                        SettingsManager.setApiUrl(apiUrl)
                    }
                    SettingsManager.setFirstLaunchCompleted(true)
                    onFinished()
                }
            }
        ) {
            Text("Get Started")
        }
    }
}

