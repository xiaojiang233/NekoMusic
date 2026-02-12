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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Check
import top.xiaojiang233.nekomusic.getPlatform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import nekomusic.composeapp.generated.resources.Res
import nekomusic.composeapp.generated.resources.about
import nekomusic.composeapp.generated.resources.about_base
import nekomusic.composeapp.generated.resources.about_by
import nekomusic.composeapp.generated.resources.account
import nekomusic.composeapp.generated.resources.api_url
import nekomusic.composeapp.generated.resources.api_url_placeholder
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
import nekomusic.composeapp.generated.resources.cancel
import nekomusic.composeapp.generated.resources.cookie_tutorial_body
import nekomusic.composeapp.generated.resources.cookie_tutorial_title
import nekomusic.composeapp.generated.resources.font_family
import nekomusic.composeapp.generated.resources.font_size
import nekomusic.composeapp.generated.resources.lyrics_settings
import nekomusic.composeapp.generated.resources.theme_color
import nekomusic.composeapp.generated.resources.default_label
import nekomusic.composeapp.generated.resources.hex_color_label
import nekomusic.composeapp.generated.resources.apply
import nekomusic.composeapp.generated.resources.standard
import nekomusic.composeapp.generated.resources.higher
import nekomusic.composeapp.generated.resources.exhigh
import nekomusic.composeapp.generated.resources.lossless
import nekomusic.composeapp.generated.resources.hires
import nekomusic.composeapp.generated.resources.jyeffect
import nekomusic.composeapp.generated.resources.sky
import nekomusic.composeapp.generated.resources.dolby
import nekomusic.composeapp.generated.resources.jymaster
import nekomusic.composeapp.generated.resources.enable_scrobble
import nekomusic.composeapp.generated.resources.scrobble_warning
import nekomusic.composeapp.generated.resources.debug
import nekomusic.composeapp.generated.resources.api_url_debug
import nekomusic.composeapp.generated.resources.open_api_debugger
import nekomusic.composeapp.generated.resources.logged_in_as_format
import nekomusic.composeapp.generated.resources.not_logged_in
import nekomusic.composeapp.generated.resources.play_next
import org.jetbrains.compose.resources.stringResource
import top.xiaojiang233.nekomusic.settings.SettingsManager
import top.xiaojiang233.nekomusic.ui.components.LoginWebView
import top.xiaojiang233.nekomusic.viewmodel.LoginViewModel

@Composable
fun SettingsScreen(
    onDebugClick: () -> Unit = {},
    viewModel: LoginViewModel = viewModel { LoginViewModel() }
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val isDark by SettingsManager.isDarkTheme().collectAsState(initial = false)
    val showBanner by SettingsManager.showBanner().collectAsState(initial = true)
    val themeSeedColor by SettingsManager.getThemeSeedColor().collectAsState(initial = 0L)
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

            // Theme Color for Desktop (or non-dynamic Android)
            if (!getPlatform().isAndroid) { // Assuming dynamic color is default on Android, but good to have manual control too? User asked for Desktop.
                HorizontalDivider()
                Text(stringResource(Res.string.theme_color))

                val presets = listOf(
                    Color(0xFF6750A4), // M3 Purple
                    Color(0xFFB3261E), // Red
                    Color(0xFF285C98), // Blue
                    Color(0xFF3B713F), // Green
                    Color(0xFF904D00), // Orange
                    Color(0xFF8B418F), // Magenta
                    Color(0xFF006874), // Cyan
                    Color(0xFF5B6200)  // Lime
                )

                // Color Presets Row
                Row(
                   modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                   horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Reset Button (System Default)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         Box(
                             modifier = Modifier
                                 .size(40.dp)
                                 .clip(CircleShape)
                                 .background(MaterialTheme.colorScheme.surfaceVariant)
                                 .clickable { scope.launch { SettingsManager.setThemeSeedColor(0L) } }
                                 .border(if (themeSeedColor == 0L || themeSeedColor == null) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape),
                             contentAlignment = Alignment.Center
                         ) {
                             if (themeSeedColor == 0L || themeSeedColor == null) {
                                 Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                             }
                         }
                         Text(stringResource(Res.string.default_label), style = MaterialTheme.typography.labelSmall)
                    }

                    presets.forEach { color ->
                         val isSelected = themeSeedColor == color.toArgb().toLong()
                         // Need to handle ARGB properly. Color.toArgb() returns Int.
                         // SettingsManager stores Long.
                         // Let's store consistent values.

                         Box(
                             modifier = Modifier
                                 .size(40.dp)
                                 .clip(CircleShape)
                                 .background(color)
                                 .clickable {
                                     // Store as unsigned long to avoid confusion or just signed long from Int
                                     scope.launch { SettingsManager.setThemeSeedColor(color.toArgb().toLong()) }
                                 }
                                 .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
                             contentAlignment = Alignment.Center
                         ) {
                             if (isSelected) {
                                  Icon(Icons.Default.Check, null, tint = Color.White)
                             }
                         }
                    }
                }

                // Hex Input
                var hexInput by remember(themeSeedColor) {
                    mutableStateOf(
                        if (themeSeedColor != null && themeSeedColor != 0L)
                            "#" + (themeSeedColor!!.toUInt().toString(16).padStart(8, '0').drop(2).uppercase())
                        else ""
                    )
                }
                // Note: stored long might look like 0xFF...... which is negative if cast to Int then Long.
                // Safest to treat conversions carefully.
                // Color(Long) expects ARGB.
                // Let's provide a text field to enter Hex.

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = {
                            hexInput = it
                            if (it.length == 7 && it.startsWith("#")) {
                                try {
                                    // Use custom parsing
                                    val hex = it.drop(1)
                                    if (hex.all { c -> c.isDigit() || c in "a..f" || c in "A..F" }) {
                                       val r = hex.substring(0,2).toInt(16)
                                       val g = hex.substring(2,4).toInt(16)
                                       val b = hex.substring(4,6).toInt(16)
                                       val color = Color(r,g,b)
                                       scope.launch { SettingsManager.setThemeSeedColor(color.toArgb().toLong()) }
                                    }
                                } catch (e: Exception) {}
                            }
                        },
                        label = { Text(stringResource(Res.string.hex_color_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                         // Apply manual parse
                         try {
                              if (hexInput.startsWith("#") && hexInput.length == 7) {
                                   val hex = hexInput.drop(1)
                                   val r = hex.substring(0,2).toInt(16)
                                   val g = hex.substring(2,4).toInt(16)
                                   val b = hex.substring(4,6).toInt(16)
                                   val color = Color(r,g,b)
                                   scope.launch { SettingsManager.setThemeSeedColor(color.toArgb().toLong()) }
                              }
                         } catch (_: Exception) {}
                    }) {
                        Text(stringResource(Res.string.apply))
                    }
                }
            }
        }

        // --- Audio Quality Section ---
        SettingsCard(stringResource(Res.string.audio_quality)) {
            val quality by SettingsManager.getAudioQuality().collectAsState(initial = "standard")
            var expanded by remember { mutableStateOf(false) }
            val qualities = listOf(
                "standard" to stringResource(Res.string.standard),
                "higher" to stringResource(Res.string.higher),
                "exhigh" to stringResource(Res.string.exhigh),
                "lossless" to stringResource(Res.string.lossless),
                "hires" to stringResource(Res.string.hires),
                "jyeffect" to stringResource(Res.string.jyeffect),
                "sky" to stringResource(Res.string.sky),
                "dolby" to stringResource(Res.string.dolby),
                "jymaster" to stringResource(Res.string.jymaster)
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

            HorizontalDivider()

            val enableScrobble by SettingsManager.enableScrobble().collectAsState(initial = false)
            ListItem(
                headlineContent = { Text(stringResource(Res.string.enable_scrobble)) },
                supportingContent = {
                    Text(
                        stringResource(Res.string.scrobble_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                trailingContent = {
                    Switch(
                        checked = enableScrobble,
                        onCheckedChange = { checked ->
                             scope.launch { SettingsManager.setEnableScrobble(checked) }
                        }
                    )
                }
            )
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
            Text("NekoMusic 1.0.2")
            Text(stringResource(Res.string.about_base))
            Text(stringResource(Res.string.about_by))
        }

        // --- Debug Section ---
        SettingsCard(stringResource(Res.string.debug)) {
            // API URL Debugging
            var apiUrl by remember { mutableStateOf("") }
            val isLoggedIn = loginState.isLoggedIn

            // For manual API URL editing
            var apiUrlInput by remember { mutableStateOf("") }
            val currentApiUrl by SettingsManager.getApiUrl().collectAsState(initial = "")
            var showUrlDialog by remember { mutableStateOf(false) }

            if (showUrlDialog) {
                AlertDialog(
                    onDismissRequest = { showUrlDialog = false },
                    title = { Text(stringResource(Res.string.api_url_debug)) },
                    text = {
                        OutlinedTextField(
                            value = apiUrlInput,
                            onValueChange = { apiUrlInput = it },
                            label = { Text(stringResource(Res.string.api_url)) },
                            placeholder = { Text(stringResource(Res.string.api_url_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
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

            // Debug Button
            Button(onClick = onDebugClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.open_api_debugger))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Section
            if (isLoggedIn) {
                Text(stringResource(Res.string.logged_in_as_format, loginState.accountName ?: ""))
            } else {
                Text(stringResource(Res.string.not_logged_in))
            }
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
