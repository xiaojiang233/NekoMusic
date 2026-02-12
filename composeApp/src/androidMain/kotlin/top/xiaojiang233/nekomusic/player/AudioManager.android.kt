package top.xiaojiang233.nekomusic.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import top.xiaojiang233.nekomusic.model.Song
import top.xiaojiang233.nekomusic.NekoApp
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.media3.common.PlaybackException
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ListenableFuture

// Use a simple Application context reference or similar.
// Ideally, we start a Service, but for simplicity we'll wrap ExoPlayer here.
// In a production app, AudioManager would bind to a Service.
// We will use a static ExoPlayer instance for now.

@UnstableApi
@OptIn(UnstableApi::class)
actual object AudioManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _state = MutableStateFlow(PlaybackState())
    actual val state: StateFlow<PlaybackState> = _state.asStateFlow()

    actual var onNext: (() -> Unit)? = null
    actual var onPrevious: (() -> Unit)? = null

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private fun ensureController(callback: (MediaController) -> Unit) {
        val existing = controller
        if (existing != null) {
            callback(existing)
            return
        }

        if (controllerFuture == null) {
            val context = NekoApp.INSTANCE
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val future = MediaController.Builder(context, sessionToken).buildAsync()
            future.addListener({
                try {
                    val newController = future.get()
                    this.controller = newController
                    setupController(newController)
                    callback(newController)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
            controllerFuture = future
        } else {
            controllerFuture?.addListener({
                controller?.let { callback(it) }
            }, MoreExecutors.directExecutor())
        }
    }

    private fun setupController(player: MediaController) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Keep UI updated if track changes via system controls
                if (mediaItem != null) {
                    // Note: We don't have a direct reverse mapping from mediaId to Song object here easily
                    // But our QueueManager in common code should handle it via callbacks.
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _state.value = _state.value.copy(duration = player.duration)
                } else if (playbackState == Player.STATE_ENDED) {
                    onNext?.invoke()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                println("AudioManager: Controller error: ${error.message}")
                scope.launch {
                    delay(2000)
                    onNext?.invoke()
                }
            }
        })

        // Poll position
        scope.launch {
            while (true) {
                if (player.isPlaying) {
                    _state.value = _state.value.copy(currentPosition = player.currentPosition)
                }
                delay(1000)
            }
        }
    }

    actual fun play(url: String, song: Song) {
        ensureController { p ->
            scope.launch {
                withContext(Dispatchers.Main) {
                    println("AudioManager: Playing URL via Controller: $url")

                    val artworkUri = song.al.cover?.takeIf { it.isNotEmpty() }?.toUri()

                    val metadata = MediaMetadata.Builder()
                        .setTitle(song.name)
                        .setArtist(song.ar.joinToString(", ") { it.name })
                        .setAlbumTitle(song.al.name)
                        .setArtworkUri(artworkUri)
                        .build()

                    // Media3 MediaController doesn't pass the URI directly in MediaItem.localConfiguration.
                    // We MUST pass it via setMediaUri in RequestMetadata for the Service to "resolve" it.
                    val requestMetadata = MediaItem.RequestMetadata.Builder()
                        .setMediaUri(url.toUri())
                        .build()

                    val item = MediaItem.Builder()
                        .setMediaId(song.id.toString())
                        .setUri(url) // For local use if any
                        .setMediaMetadata(metadata)
                        .setRequestMetadata(requestMetadata)
                        .build()

                    p.setMediaItem(item)
                    p.prepare()
                    p.play()
                    _state.value = _state.value.copy(currentSong = song, isPlaying = true)
                }
            }
        }
    }

    actual fun pause() {
        controller?.pause()
    }

    actual fun resume() {
        controller?.play()
    }

    actual fun seekTo(position: Long) {
        controller?.seekTo(position)
    }

    actual fun setVolume(volume: Float) {
        controller?.volume = volume
    }

    actual fun release() {
        controller?.release()
        controller = null
        controllerFuture = null
    }
}
