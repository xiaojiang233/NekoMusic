package top.xiaojiang233.nekomusic.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.api.NeteaseApi
import top.xiaojiang233.nekomusic.model.Song
import top.xiaojiang233.nekomusic.settings.SettingsManager

enum class PlaybackMode {
    LoopOne,    // Single loop
    Shuffle,    // Random
    Order       // Order (with repeat list usually, or just stop at end?)
}

object PlayerController {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _playbackMode = MutableStateFlow(PlaybackMode.Order)
    val playbackMode: StateFlow<PlaybackMode> = _playbackMode.asStateFlow()

    init {
        // Wire up AudioManager callbacks
        AudioManager.onNext = { playNext(auto = true) }
        AudioManager.onPrevious = { playPrevious() }
    }

    fun playList(songs: List<Song>, startIndex: Int = 0) {
        _queue.value = songs
        _currentIndex.value = startIndex
        playCurrent()
    }

    fun togglePlaybackMode() {
        val modes = PlaybackMode.entries
        val nextModeOrdinal = (_playbackMode.value.ordinal + 1) % modes.size
        _playbackMode.value = modes[nextModeOrdinal]
    }

    fun playNext(auto: Boolean = false) {
        val queue = _queue.value
        if (queue.isEmpty()) return

        if (auto && _playbackMode.value == PlaybackMode.LoopOne) {
            // Loop One (Auto): Just replay current
            playCurrent()
            return
        }

        // Calculate next index
        val nextIndex = when (_playbackMode.value) {
            PlaybackMode.Shuffle -> {
                // Simple random for now
                queue.indices.random()
            }
            else -> {
                // Order or LoopOne (Manual click)
                (_currentIndex.value + 1) % queue.size
            }
        }

        _currentIndex.value = nextIndex
        playCurrent()
    }

    fun playPrevious() {
        val queue = _queue.value
        if (queue.isEmpty()) return

        val prevIndex = when (_playbackMode.value) {
            PlaybackMode.Shuffle -> {
                 queue.indices.random()
            }
            else -> {
                if (_currentIndex.value - 1 < 0) queue.size - 1 else _currentIndex.value - 1
            }
        }

        _currentIndex.value = prevIndex
        playCurrent()
    }

    private fun playCurrent() {
        scope.launch {
            val song = _queue.value.getOrNull(_currentIndex.value) ?: return@launch
            // Optimistic UI update or wait for AudioManager?
            // AudioManager handles buffering state.

            // Get URL
            try {
                // High quality
                val urlResult = NeteaseApi.getSongUrl(song.id, SettingsManager.getAudioQuality().first())
                // Fallback handled inside getSongUrlV1 or explicit?
                // The API call usually returns a list.
                val url = urlResult.data.find { it.id == song.id }?.url

                if (url != null) {
                    AudioManager.play(url, song)
                } else {
                    // Try standard if V1 failed or empty
                    // Or try next song?
                    println("Failed to get URL for ${song.name}, skipping")
                    playNext(auto = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // playNext(auto = true) // Auto skip on error?
            }
        }
    }

    // Resume/Pause delegation
    fun pause() = AudioManager.pause()
    fun resume() = AudioManager.resume()
    fun toggle() {
        val state = AudioManager.state.value
        if (state.isPlaying) pause() else resume()
    }
    fun seekTo(pos: Long) = AudioManager.seekTo(pos)
    fun setVolume(volume: Float) {
        AudioManager.setVolume(volume)
    }
}
