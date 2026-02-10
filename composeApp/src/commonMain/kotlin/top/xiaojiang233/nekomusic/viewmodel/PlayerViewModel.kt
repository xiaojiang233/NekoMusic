package top.xiaojiang233.nekomusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.api.NeteaseApi
import top.xiaojiang233.nekomusic.player.AudioManager
import top.xiaojiang233.nekomusic.player.PlaybackState
import top.xiaojiang233.nekomusic.player.PlayerController
import top.xiaojiang233.nekomusic.utils.LyricsParser

data class PlayerUiState(
    val playbackState: PlaybackState = PlaybackState(),
    val lyrics: List<LyricsParser.LyricLine> = emptyList(),
    val currentLineIndex: Int = 0,
    val isLoadingLyrics: Boolean = false
)

class PlayerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentSongId: Long = -1

    init {
        viewModelScope.launch {
            AudioManager.state.collect { playbackState ->
                val currentSong = playbackState.currentSong
                if (currentSong != null && currentSong.id != currentSongId) {
                    currentSongId = currentSong.id
                    loadLyrics(currentSongId)
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

    fun togglePlay() = PlayerController.toggle()
    fun playNext() = PlayerController.playNext()
    fun playPrevious() = PlayerController.playPrevious()
    fun seekTo(pos: Long) = PlayerController.seekTo(pos)
}
