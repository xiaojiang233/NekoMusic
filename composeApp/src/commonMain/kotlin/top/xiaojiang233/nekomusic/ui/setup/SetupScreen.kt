package top.xiaojiang233.nekomusic.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nekomusic.composeapp.generated.resources.Res
import nekomusic.composeapp.generated.resources.api_url
import nekomusic.composeapp.generated.resources.api_url_placeholder
import nekomusic.composeapp.generated.resources.choose_theme
import nekomusic.composeapp.generated.resources.dark
import nekomusic.composeapp.generated.resources.get_started
import nekomusic.composeapp.generated.resources.light
import nekomusic.composeapp.generated.resources.welcome_title
import org.jetbrains.compose.resources.stringResource
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
        Text(stringResource(Res.string.welcome_title), style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Text(stringResource(Res.string.choose_theme))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.light))
            Switch(checked = isDark, onCheckedChange = { isDark = it })
            Text(stringResource(Res.string.dark))
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = apiUrl,
            onValueChange = { apiUrl = it },
            label = { Text(stringResource(Res.string.api_url)) },
            placeholder = { Text(stringResource(Res.string.api_url_placeholder)) },
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
            Text(stringResource(Res.string.get_started))
        }
    }
}
