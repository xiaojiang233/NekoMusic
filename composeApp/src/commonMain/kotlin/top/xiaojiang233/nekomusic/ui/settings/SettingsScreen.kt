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
import nekomusic.composeapp.generated.resources.Res
import nekomusic.composeapp.generated.resources.about
import nekomusic.composeapp.generated.resources.account
import nekomusic.composeapp.generated.resources.api_url
import nekomusic.composeapp.generated.resources.appearance
import nekomusic.composeapp.generated.resources.change_url
import nekomusic.composeapp.generated.resources.dark_theme
import nekomusic.composeapp.generated.resources.logged_in
import nekomusic.composeapp.generated.resources.login
import nekomusic.composeapp.generated.resources.login_browser
import nekomusic.composeapp.generated.resources.login_manual_title
import nekomusic.composeapp.generated.resources.logout
import nekomusic.composeapp.generated.resources.network
import nekomusic.composeapp.generated.resources.paste_cookie
import nekomusic.composeapp.generated.resources.quality_preference
import nekomusic.composeapp.generated.resources.save
import nekomusic.composeapp.generated.resources.settings_title
import nekomusic.composeapp.generated.resources.show_banner
import nekomusic.composeapp.generated.resources.audio_quality
import nekomusic.composeapp.generated.resources.blur_intensity
import nekomusic.composeapp.generated.resources.set_api_url_title
import nekomusic.composeapp.generated.resources.retry
import nekomusic.composeapp.generated.resources.cancel
import nekomusic.composeapp.generated.resources.cookie_tutorial_body
import nekomusic.composeapp.generated.resources.cookie_tutorial_title
import nekomusic.composeapp.generated.resources.font_family
import nekomusic.composeapp.generated.resources.font_size
import nekomusic.composeapp.generated.resources.lyrics_settings
import org.jetbrains.compose.resources.stringResource
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
            title = { Text(stringResource(Res.string.set_api_url_title)) },
            text = {
                OutlinedTextField(
                    value = apiUrlInput,
                    onValueChange = { apiUrlInput = it },
                    label = { Text(stringResource(Res.string.api_url)) },
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
                ) { Text(stringResource(Res.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) { Text(stringResource(Res.string.cancel)) }
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
        Text(stringResource(Res.string.settings_title), style = MaterialTheme.typography.headlineLarge)

        // --- Account Section ---
        SettingsCard(stringResource(Res.string.account)) {
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
                        Text(stringResource(Res.string.logged_in), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Button(onClick = { viewModel.logout() }) {
                        Text(stringResource(Res.string.logout))
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.login_manual_title), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))

                    // Manual Tutorial text
                    TutorialText()
                    Spacer(Modifier.height(16.dp))

                    Button(onClick = { showWebViewLogin = true }) {
                        Icon(Icons.AutoMirrored.Filled.Login, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.login_browser))
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    var cookieInput by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = cookieInput,
                        onValueChange = { cookieInput = it },
                        label = { Text(stringResource(Res.string.paste_cookie)) },
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
                        Text(stringResource(Res.string.login))
                    }

                    if (loginState.message != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(loginState.message!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // --- Appearance Section ---
        SettingsCard(stringResource(Res.string.appearance)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(Res.string.dark_theme))
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
                Text(stringResource(Res.string.show_banner))
                Switch(
                    checked = showBanner,
                    onCheckedChange = {
                        scope.launch { SettingsManager.setShowBanner(it) }
                    }
                )
            }
        }

        // --- Audio Quality Section ---
        SettingsCard(stringResource(Res.string.audio_quality)) {
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
                Text(stringResource(Res.string.quality_preference))
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
        SettingsCard(stringResource(Res.string.lyrics_settings)) {
            val lyricsFontSize by SettingsManager.getLyricsFontSize().collectAsState(initial = 16f)
            val lyricsBlurIntensity by SettingsManager.getLyricsBlurIntensity().collectAsState(initial = 0f)
            val lyricsFontFamily by SettingsManager.getLyricsFontFamily().collectAsState(initial = "Default")

            val fontFamilies = listOf("Default", "Serif", "SansSerif", "Monospace", "Cursive")
            var expanded by remember { mutableStateOf(false) }

            Column {
                Text("${stringResource(Res.string.font_size)}: ${lyricsFontSize.toInt()} sp")
                Slider(
                    value = lyricsFontSize,
                    onValueChange = { viewModel.setLyricsFontSize(it) },
                    valueRange = 12f..48f,
                    steps = 35
                )

                Spacer(Modifier.height(16.dp))

                Text("${stringResource(Res.string.blur_intensity)}: ${lyricsBlurIntensity.toInt()}")
                Slider(
                    value = lyricsBlurIntensity,
                    onValueChange = { viewModel.setLyricsBlurIntensity(it) },
                    valueRange = 0f..20f,
                    steps = 20
                )

                Spacer(Modifier.height(16.dp))

                Text(stringResource(Res.string.font_family))
                Box {
                    var expanded by remember { mutableStateOf(false) }
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
        SettingsCard(stringResource(Res.string.network)) {
            Column {
                Text(stringResource(Res.string.api_url), style = MaterialTheme.typography.labelMedium)
                Text(currentApiUrl, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    apiUrlInput = currentApiUrl
                    showUrlDialog = true
                }) {
                    Text(stringResource(Res.string.change_url))
                }
            }
        }

        // --- About Section ---
        SettingsCard(stringResource(Res.string.about)) {
            Text("NekoMusic v1.0-snapshot")
            Text("Based on Compose Multiplatform")
            Text("By xiaojiang233")
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
            Text(stringResource(Res.string.cookie_tutorial_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(Res.string.cookie_tutorial_body), style = MaterialTheme.typography.bodySmall)
    }
}
