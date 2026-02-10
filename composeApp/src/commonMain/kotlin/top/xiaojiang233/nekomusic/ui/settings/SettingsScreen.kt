package top.xiaojiang233.nekomusic.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.settings.SettingsManager
import top.xiaojiang233.nekomusic.ui.components.LoginWebView
import top.xiaojiang233.nekomusic.viewmodel.LoginViewModel

@Composable
fun SettingsScreen(
    viewModel: LoginViewModel = viewModel { LoginViewModel() }
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val isDark by SettingsManager.isDarkTheme().collectAsState(initial = false)
    val showBanner by SettingsManager.showBanner().collectAsState(initial = true)
    val loginState by viewModel.uiState.collectAsState()

    // For manual API URL editing
    var apiUrlInput by remember { mutableStateOf("") }
    val currentApiUrl by SettingsManager.getApiUrl().collectAsState(initial = "")
    var showUrlDialog by remember { mutableStateOf(false) }

    // WebView Dialog State
    var showWebViewLogin by remember { mutableStateOf(false) }

    if (showWebViewLogin) {
        LoginWebView(
            onCookieFound = { cookie ->
                viewModel.loginWithCookie(cookie)
                showWebViewLogin = false
            },
            onDismiss = { showWebViewLogin = false }
        )
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Set API URL") },
            text = {
                OutlinedTextField(
                    value = apiUrlInput,
                    onValueChange = { apiUrlInput = it },
                    label = { Text("API URL") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            SettingsManager.setApiUrl(apiUrlInput)
                            showUrlDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)

        // --- Account Section ---
        SettingsCard("Account") {
            if (loginState.isLoggedIn) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = loginState.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier.size(64.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(loginState.accountName ?: "User", style = MaterialTheme.typography.titleMedium)
                        Text("Logged in", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Button(onClick = { viewModel.logout() }) {
                        Text("Logout")
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Manual Cookie Login", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))

                    // Manual Tutorial text
                    TutorialText()
                    Spacer(Modifier.height(16.dp))

                    Button(onClick = { showWebViewLogin = true }) {
                        Icon(Icons.AutoMirrored.Filled.Login, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Login via Built-in Browser")
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    var cookieInput by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = cookieInput,
                        onValueChange = { cookieInput = it },
                        label = { Text("Paste Cookie Here") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.loginWithCookie(cookieInput) },
                        enabled = !loginState.isLoading && cookieInput.isNotBlank()
                    ) {
                        if (loginState.isLoading) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Login")
                    }

                    if (loginState.message != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(loginState.message!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // --- Appearance Section ---
        SettingsCard("Appearance") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dark Theme")
                Switch(
                    checked = isDark,
                    onCheckedChange = {
                        scope.launch { SettingsManager.setDarkTheme(it) }
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Banner on Home")
                Switch(
                    checked = showBanner,
                    onCheckedChange = {
                        scope.launch { SettingsManager.setShowBanner(it) }
                    }
                )
            }
        }

        // --- Audio Quality Section ---
        SettingsCard("Audio Quality") {
            val quality by SettingsManager.getAudioQuality().collectAsState(initial = "standard")
            var expanded by remember { mutableStateOf(false) }
            val qualities = listOf(
                "standard" to "Standard (标准)",
                "higher" to "Higher (较高)",
                "exhigh" to "ExHigh (极高)",
                "lossless" to "Lossless (无损)",
                "hires" to "Hi-Res"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quality Preference")
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(qualities.find { it.first == quality }?.second ?: quality)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        qualities.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    scope.launch { SettingsManager.setAudioQuality(key) }
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- Lyrics Settings Section ---
        SettingsCard("Lyrics Settings") {
            val lyricsFontSize by SettingsManager.getLyricsFontSize().collectAsState(initial = 16f)
            val lyricsBlurIntensity by SettingsManager.getLyricsBlurIntensity().collectAsState(initial = 0f)
            val lyricsFontFamily by SettingsManager.getLyricsFontFamily().collectAsState(initial = "Default")

            val fontFamilies = listOf("Default", "Serif", "SansSerif", "Monospace", "Cursive")
            var expanded by remember { mutableStateOf(false) }

            Column {
                Text("Font Size: ${lyricsFontSize.toInt()} sp")
                Slider(
                    value = lyricsFontSize,
                    onValueChange = { viewModel.setLyricsFontSize(it) },
                    valueRange = 12f..48f,
                    steps = 35
                )

                Spacer(Modifier.height(16.dp))

                Text("Blur Intensity: ${lyricsBlurIntensity.toInt()}")
                Slider(
                    value = lyricsBlurIntensity,
                    onValueChange = { viewModel.setLyricsBlurIntensity(it) },
                    valueRange = 0f..20f,
                    steps = 20
                )

                Spacer(Modifier.height(16.dp))

                Text("Font Family")
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(lyricsFontFamily)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        fontFamilies.forEach { family ->
                            DropdownMenuItem(
                                text = { Text(family) },
                                onClick = {
                                    scope.launch { SettingsManager.setLyricsFontFamily(family) }
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- Network Section ---
        SettingsCard("Network") {
            Column {
                Text("API Base URL", style = MaterialTheme.typography.labelMedium)
                Text(currentApiUrl, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    apiUrlInput = currentApiUrl
                    showUrlDialog = true
                }) {
                    Text("Change URL")
                }
            }
        }

        // --- About Section ---
        SettingsCard("About") {
            Text("NekoMusic v1.0.0")
            Text("Based on Compose Multiplatform")
        }
    }
}

@Composable
fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
fun TutorialText() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("How to get Cookie (Chromium Browsers)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(buildAnnotatedString {
            append("1. Log in to ")
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("music.163.com") }
            append(" in browser.\n")

            append("2. Open Developer Tools (F12) -> Network tab.\n")
            append("3. Filter by ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Fetch/XHR") }
            append(".\n")

            append("4. Click 'Daily Recommendation' or just refresh.\n")
            append("5. Find any request (e.g. ")
            withStyle(SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)) { append("songs?csrf_token") }
            append(").\n")

            append("6. In 'Headers' -> 'Request Headers', copy the ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("cookie") }
            append(" value.\n")

            append("7. Paste it below.")
        }, style = MaterialTheme.typography.bodySmall)
    }
}
