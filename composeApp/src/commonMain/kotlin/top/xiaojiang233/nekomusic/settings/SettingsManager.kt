package top.xiaojiang233.nekomusic.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import top.xiaojiang233.nekomusic.getPlatform

object SettingsManager {
    private val dataStore = createDataStore()

    private val KEY_API_URL = stringPreferencesKey("api_url")
    private val KEY_IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    private val KEY_FIRST_LAUNCH_COMPLETED = booleanPreferencesKey("first_launch_completed")
    private val KEY_COOKIE = stringPreferencesKey("cookie")
    private val KEY_SHOW_BANNER = booleanPreferencesKey("show_banner")
    private val KEY_AUDIO_QUALITY = stringPreferencesKey("audio_quality")
    private val KEY_SEARCH_HISTORY = stringPreferencesKey("search_history_json")
    private val KEY_LYRICS_FONT_SIZE = floatPreferencesKey("lyrics_font_size")
    private val KEY_LYRICS_BLUR_INTENSITY = floatPreferencesKey("lyrics_blur_intensity")
    private val KEY_LYRICS_FONT_FAMILY = stringPreferencesKey("lyrics_font_family")
    private val KEY_MAX_CACHE_SIZE = longPreferencesKey("max_cache_size")
    private val KEY_THEME_SEED_COLOR = longPreferencesKey("theme_seed_color")
    private val KEY_ENABLE_SCROBBLE = booleanPreferencesKey("enable_scrobble")

    // Defaults
    private const val DEFAULT_DEBUG_API = "http://192.168.1.4:3000"
    private const val DEFAULT_RELEASE_API = "http://localhost:3000"

    fun getApiUrl(): Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_API_URL] ?: if (getPlatform().isDebug) DEFAULT_DEBUG_API else DEFAULT_RELEASE_API
    }

    suspend fun setApiUrl(url: String) {
        dataStore.edit { it[KEY_API_URL] = url }
    }

    fun isDarkTheme(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_IS_DARK_THEME] ?: false // Default system? Assume light or allow system follower. let's default false.
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        dataStore.edit { it[KEY_IS_DARK_THEME] = isDark }
    }

    suspend fun completeSetup(isDark: Boolean, apiUrl: String?) {
        dataStore.edit { prefs ->
            prefs[KEY_IS_DARK_THEME] = isDark
            if (apiUrl != null) {
                prefs[KEY_API_URL] = apiUrl
            }
            prefs[KEY_FIRST_LAUNCH_COMPLETED] = true
        }
    }

    fun isFirstLaunchCompleted(): Flow<Boolean> = dataStore.data.map { prefs ->
         prefs[KEY_FIRST_LAUNCH_COMPLETED] ?: false
    }

    suspend fun setFirstLaunchCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_FIRST_LAUNCH_COMPLETED] = completed }
    }

    fun getCookie(): Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_COOKIE]
    }

    suspend fun setCookie(cookie: String) {
        dataStore.edit { it[KEY_COOKIE] = cookie }
    }

    fun showBanner(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SHOW_BANNER] ?: true
    }

    suspend fun setShowBanner(show: Boolean) {
        dataStore.edit { it[KEY_SHOW_BANNER] = show }
    }

    fun getAudioQuality(): Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_AUDIO_QUALITY] ?: "standard"
    }

    suspend fun setAudioQuality(quality: String) {
        dataStore.edit { it[KEY_AUDIO_QUALITY] = quality }
    }

    fun getSearchHistory(): Flow<List<String>> = dataStore.data.map { prefs ->
        val json = prefs[KEY_SEARCH_HISTORY] ?: "[]"
        try {
            kotlinx.serialization.json.Json.decodeFromString(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addSearchHistory(query: String) {
        if (query.isBlank()) return
        dataStore.edit { prefs ->
            val currentJson = prefs[KEY_SEARCH_HISTORY] ?: "[]"
            val list = try {
                kotlinx.serialization.json.Json.decodeFromString<MutableList<String>>(currentJson)
            } catch (e: Exception) {
                mutableListOf()
            }
            // Remove if exists to move to top
            list.remove(query)
            list.add(0, query)
            // Limit size
            if (list.size > 20) list.removeAt(list.lastIndex)

            prefs[KEY_SEARCH_HISTORY] = kotlinx.serialization.json.Json.encodeToString(list)
        }
    }

    suspend fun clearSearchHistory() {
        dataStore.edit { it.remove(KEY_SEARCH_HISTORY) }
    }

    fun getLyricsFontSize(): Flow<Float> = dataStore.data.map { prefs ->
        prefs[KEY_LYRICS_FONT_SIZE] ?: 40f
    }

    suspend fun setLyricsFontSize(size: Float) {
        dataStore.edit { it[KEY_LYRICS_FONT_SIZE] = size }
    }

    fun getLyricsBlurIntensity(): Flow<Float> = dataStore.data.map { prefs ->
        prefs[KEY_LYRICS_BLUR_INTENSITY] ?: 10f
    }

    suspend fun setLyricsBlurIntensity(intensity: Float) {
        dataStore.edit { it[KEY_LYRICS_BLUR_INTENSITY] = intensity }
    }

    fun getLyricsFontFamily(): Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_LYRICS_FONT_FAMILY] ?: "Default"
    }

    suspend fun setLyricsFontFamily(family: String) {
        dataStore.edit { it[KEY_LYRICS_FONT_FAMILY] = family }
    }

    fun getMaxCacheSize(): Flow<Long> = dataStore.data.map { prefs ->
        prefs[KEY_MAX_CACHE_SIZE] ?: (512 * 1024 * 1024L) // Default 512MB
    }

    suspend fun setMaxCacheSize(size: Long) {
        dataStore.edit { it[KEY_MAX_CACHE_SIZE] = size }
    }

    fun getThemeSeedColor(): Flow<Long?> = dataStore.data.map { prefs ->
        prefs[KEY_THEME_SEED_COLOR] ?: 0L // 0L represents no custom color
    }

    suspend fun setThemeSeedColor(color: Long?) {
        dataStore.edit {
            if (color == null || color == 0L) {
                 it.remove(KEY_THEME_SEED_COLOR)
            } else {
                 it[KEY_THEME_SEED_COLOR] = color
            }
        }
    }

    fun enableScrobble(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[KEY_ENABLE_SCROBBLE] ?: false
        }
    }

    suspend fun setEnableScrobble(enable: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ENABLE_SCROBBLE] = enable
        }
    }

    fun clearCache() {
        // Platform specific implementation should be injected or handled via expect/actual
        // For now, no-op or clear coil cache if possible
        // Ideally: ImageLoader should be configured with a singleton that can be cleared.
    }
}
