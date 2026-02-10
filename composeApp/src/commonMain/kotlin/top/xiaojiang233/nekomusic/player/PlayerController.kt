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

object PlayerController {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    init {
        // Wire up AudioManager callbacks
        AudioManager.onNext = { playNext() }
        AudioManager.onPrevious = { playPrevious() }
    }

    fun playList(songs: List<Song>, startIndex: Int = 0) {
        _queue.value = songs
        _currentIndex.value = startIndex
        playCurrent()
    }

    fun playNext() {
        if (_queue.value.isNotEmpty()) {
            val nextIndex = (_currentIndex.value + 1) % _queue.value.size
            _currentIndex.value = nextIndex
            playCurrent()
        }
    }

    fun playPrevious() {
        if (_queue.value.isNotEmpty()) {
            val prevIndex = if (_currentIndex.value - 1 < 0) _queue.value.size - 1 else _currentIndex.value - 1
            _currentIndex.value = prevIndex
            playCurrent()
        }
    }

    private fun playCurrent() {
        val song = _queue.value.getOrNull(_currentIndex.value) ?: return
        scope.launch {
            try {
                val quality = SettingsManager.getAudioQuality().first()
                val response = NeteaseApi.getSongUrl(song.id, level = quality)
                if (response.code == 200 && response.data.isNotEmpty()) {
                    val url = response.data[0].url
                    if (url != null) {
                        AudioManager.play(url, song)
                    } else {
                        println("No URL for song")
                        // Try next?
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
}

