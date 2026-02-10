package top.xiaojiang233.nekomusic.player

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.model.Song
import top.xiaojiang233.nekomusic.NekoPlayerApplication

// Use a simple Application context reference or similar.
// Ideally, we start a Service, but for simplicity we'll wrap ExoPlayer here.
// In a production app, AudioManager would bind to a Service.
// We will use a static ExoPlayer instance for now.

actual object AudioManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _state = MutableStateFlow(PlaybackState())
    actual val state: StateFlow<PlaybackState> = _state.asStateFlow()

    actual var onNext: (() -> Unit)? = null
    actual var onPrevious: (() -> Unit)? = null

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var _playerInitialized = false

    private fun ensurePlayer() {
        if (_playerInitialized) return
        val context = NekoPlayerApplication.context ?: return

        player = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.value = _state.value.copy(isPlaying = isPlaying)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _state.value = _state.value.copy(duration = duration)
                    }
                }
            })
        }

        // Initialize MediaSession
        mediaSession = MediaSession.Builder(context, player!!)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                        .build()
                }

                // Note: Forward/Rewind are handled by player automatically.
                // Next/Previous require queue knowledge or callback.
                // ExoPlayer session automatically calls player.seekToNext/Previous.
                // We need to override player behavior or just listen to keys?
                // MediaSession usually defaults to calling player methods.
                // If we want to capture Next/Prev when buttons are clicked:
                // We typically use a Custom Player or set session commands.
                // Simpler: The UI and System buttons call player.seekToNext().
                // We can intercept this via the Player.Listener or by wrapping ExoPlayer?
                // Actually, overriding onCustomCommand or setting session commands is standard.
                // For simplicity, we assume player.seekToNext() triggers the listener.
                // But better: Use Session Callback.
            })
            .build()

        // Hack: To intercept "Next" on ExoPlayer, we usually need a Queue.
        // Since we are managing queue externally (via simple currentSong), "Next" on system UI might do nothing
        // if the player has no playlist.
        // We can force it to think it has items or handle the intent.

        _playerInitialized = true

        // Polling for progress
        scope.launch {
            while (true) {
                player?.let { p ->
                    if (p.isPlaying) {
                        _state.value = _state.value.copy(currentPosition = p.currentPosition)
                    }
                }
                delay(1000)
            }
        }
    }

    actual fun play(url: String, song: Song) {
        ensurePlayer()
        println("AudioManager: Playing URL on Android: $url")
        player?.let {
            val metadata = MediaMetadata.Builder()
                .setTitle(song.name)
                .setArtist(song.ar.joinToString(", ") { it.name })
                .setArtworkUri(android.net.Uri.parse(song.al.picUrl))
                .build()

            val item = MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(metadata)
                .build()

            it.setMediaItem(item)
            it.prepare()
            it.play()
            _state.value = _state.value.copy(currentSong = song, isPlaying = true)
        }
    }

    actual fun pause() {
        player?.pause()
    }

    actual fun resume() {
        player?.play()
    }

    actual fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    actual fun release() {
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        _playerInitialized = false
    }
}
