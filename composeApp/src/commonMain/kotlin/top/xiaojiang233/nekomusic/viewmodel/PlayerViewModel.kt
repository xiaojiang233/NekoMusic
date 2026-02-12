package top.xiaojiang233.nekomusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.api.NeteaseApi
import top.xiaojiang233.nekomusic.player.AudioManager
import top.xiaojiang233.nekomusic.player.PlaybackMode
import top.xiaojiang233.nekomusic.player.PlaybackState
import top.xiaojiang233.nekomusic.player.PlayerController
import top.xiaojiang233.nekomusic.utils.FavoritesManager
import top.xiaojiang233.nekomusic.utils.LyricsParser

data class PlayerUiState(
    val playbackState: PlaybackState = PlaybackState(),
    val lyrics: List<LyricsParser.LyricLine> = emptyList(),
    val currentLineIndex: Int = 0,
    val isLoadingLyrics: Boolean = false,
    val playbackMode: PlaybackMode = PlaybackMode.Order,
    val isLiked: Boolean = false
)

class PlayerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentSongId: Long = -1

    init {
        viewModelScope.launch {
            PlayerController.playbackMode.collect { mode ->
                _uiState.value = _uiState.value.copy(playbackMode = mode)
            }
        }

        // Observer favorites changes
        viewModelScope.launch {
            FavoritesManager.likedSongIds.collect { likes ->
                val currentSong = AudioManager.state.value.currentSong
                if (currentSong != null) {
                    val isLiked = likes.contains(currentSong.id)
                    _uiState.value = _uiState.value.copy(isLiked = isLiked)
                }
            }
        }

        viewModelScope.launch {
            AudioManager.state.collect { playbackState ->
                val currentSong = playbackState.currentSong
                if (currentSong != null && currentSong.id != currentSongId) {
                    currentSongId = currentSong.id
                    loadLyrics(currentSongId)

                    // Update liked status when song changes
                    val isLiked = FavoritesManager.isLiked(currentSongId)
                    _uiState.value = _uiState.value.copy(isLiked = isLiked)
                }

                // Update lyrics position
                val currentPos = playbackState.currentPosition
                val lines = _uiState.value.lyrics
                if (lines.isNotEmpty()) {
                    var index = lines.indexOfLast { it.time <= currentPos }
                    if (index == -1 && lines.isNotEmpty()) index = 0
                    if (index != _uiState.value.currentLineIndex) {
                        _uiState.value = _uiState.value.copy(
                            playbackState = playbackState,
                            currentLineIndex = index
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(playbackState = playbackState)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(playbackState = playbackState)
                }
            }
        }
    }

    private fun loadLyrics(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingLyrics = true)
            try {
                // Try to get verbatim lyrics first (API /lyric/new returns yrc in LyricResponse)
                // Note: The API call might return code 200 but no yrc if not available.
                // We use NeteaseApi.getLyrics which requests /lyric/new?id=...
                val response = NeteaseApi.getLyrics(id)

                var parsedLyrics: List<LyricsParser.LyricLine> = emptyList()

                // Check if yrc is present and valid
                val yrcContent = response.yrc?.lyric
                if (!yrcContent.isNullOrBlank()) {
                    parsedLyrics = LyricsParser.parseYrc(yrcContent)
                }

                // Fallback to lrc if yrc parsed empty or not present
                if (parsedLyrics.isEmpty()) {
                    val rawLrc = response.lrc?.lyric ?: ""
                    parsedLyrics = LyricsParser.parse(rawLrc)
                }

                // Process translations (tlyric)
                val tlyricContent = response.tlyric?.lyric
                if (!tlyricContent.isNullOrBlank() && parsedLyrics.isNotEmpty()) {
                    try {
                        val translations = LyricsParser.parse(tlyricContent.trim())
                        // Pre-process translations to fill empty lines?
                        // If we have [t1][t2]Text, parser gives [t1]"" [t2]"" [?]"Text".
                        // Actually standard parser should handle [t1][t2]Text --> t1:Text, t2:Text.
                        // If logic splits by \n, then we get empty lines.
                        // We will try to map loosely.

                        val mutableTranslations = translations.toMutableList()

                        parsedLyrics = parsedLyrics.map { line ->
                            var transText: String? = null

                            // 1. Try exact match
                            val exactIndex = mutableTranslations.indexOfFirst { it.time == line.time }
                            if (exactIndex != -1) {
                                transText = mutableTranslations[exactIndex].text
                                // Don't remove if it might be used for other lines? No, translations usually 1-to-1 or 1-to-many timestamps.
                                // If 1-to-many timestamps, we consume one instance.
                                // But if text is empty?
                                if (transText.isBlank()) {
                                    // Fallback logic could go here
                                }
                                mutableTranslations.removeAt(exactIndex)
                            } else {
                                // 2. Tolerance match (e.g. within 500ms -> increased from 200)
                                val toleranceIndex = mutableTranslations.indexOfFirst { kotlin.math.abs(it.time - line.time) < 500 }
                                if (toleranceIndex != -1) {
                                    transText = mutableTranslations[toleranceIndex].text
                                    mutableTranslations.removeAt(toleranceIndex)
                                }
                            }

                            if (!transText.isNullOrBlank()) {
                                line.copy(translation = transText)
                            } else {
                                line
                            }
                        }
                    } catch (e: Exception) {
                        println("Failed to parse translations: ${e.message}")
                    }
                }

                _uiState.value = _uiState.value.copy(
                    lyrics = parsedLyrics,
                    isLoadingLyrics = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoadingLyrics = false)
            }
        }
    }

    fun togglePlay() {
        if (_uiState.value.playbackState.isPlaying) {
            AudioManager.pause()
        } else {
            AudioManager.resume()
        }
    }

    fun playPrevious() {
        PlayerController.playPrevious()
    }

    fun playNext() {
        PlayerController.playNext()
    }

    fun seekTo(position: Long) {
        AudioManager.seekTo(position)
    }

    fun setVolume(volume: Float) {
        PlayerController.setVolume(volume)
    }

    fun togglePlaybackMode() {
        PlayerController.togglePlaybackMode()
    }

    fun toggleLike() {
        val currentSong = AudioManager.state.value.currentSong ?: return
        viewModelScope.launch {
            FavoritesManager.toggleLike(currentSong.id)
        }
    }
}
