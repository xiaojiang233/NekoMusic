package top.xiaojiang233.nekomusic.player

import kotlinx.coroutines.flow.StateFlow
import top.xiaojiang233.nekomusic.model.Song

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 0,
    val currentSong: Song? = null
)

expect object AudioManager {
    val state: StateFlow<PlaybackState>

    fun play(url: String, song: Song)
    fun pause()
    fun resume()
    fun seekTo(position: Long)
    fun release()

    // Callbacks for Queue Management
    var onNext: (() -> Unit)?
    var onPrevious: (() -> Unit)?
}
