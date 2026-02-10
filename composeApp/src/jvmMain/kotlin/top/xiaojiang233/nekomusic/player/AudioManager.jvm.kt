package top.xiaojiang233.nekomusic.player

import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.xiaojiang233.nekomusic.model.Song
import top.xiaojiang233.nekomusic.network.NetworkClient
import java.io.File

actual object AudioManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _state = MutableStateFlow(PlaybackState())
    actual val state: StateFlow<PlaybackState> = _state.asStateFlow()

    actual var onNext: (() -> Unit)? = null
    actual var onPrevious: (() -> Unit)? = null

    private var mediaPlayer: MediaPlayer? = null
    private var isInitialized = false
    private var downloadJob: Job? = null

    private fun initJavaFX() {
        if (!isInitialized) {
            // Force JavaFX initialization
            try {
                JFXPanel()
            } catch (_: Exception) {
                // Already started or headless?
            }
            isInitialized = true
        }
    }

    actual fun play(url: String, song: Song) {
        initJavaFX()
        downloadJob?.cancel()
        println("AudioManager: Playing URL (streaming/downloading): $url")

        Platform.runLater {
            mediaPlayer?.stop()
            mediaPlayer?.dispose()
        }

        _state.value = _state.value.copy(currentSong = song, isPlaying = true) // Optimistic

        downloadJob = scope.launch {
            try {
                // Download using Ktor (authenticated)
                val bytes = NetworkClient.client.get(url).readRawBytes()
                val tempFile = withContext(Dispatchers.IO) {
                    val file = File.createTempFile("neko_", ".mp3")
                    file.deleteOnExit()
                    file.writeBytes(bytes)
                    file
                }

                Platform.runLater {
                    try {
                        val media = Media(tempFile.toURI().toString())
                        val player = MediaPlayer(media)
                        mediaPlayer = player

                        player.setOnReady {
                            _state.value = _state.value.copy(duration = media.duration.toMillis().toLong())
                            player.play()
                        }

                        player.currentTimeProperty().addListener { _, _, _ -> }

                        player.setOnEndOfMedia {
                            println("MediaPlayer: End of Media")
                            onNext?.invoke()
                        }

                        player.statusProperty().addListener { _, _, newValue ->
                            val isPlaying = newValue == MediaPlayer.Status.PLAYING
                            // Sync state only if matches song?
                            // Simple sync
                            val currentState = _state.value
                            if (currentState.isPlaying != isPlaying) {
                                 _state.value = currentState.copy(isPlaying = isPlaying)
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                        // _state.value = _state.value.copy(error = e.message)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle download fail
            }
        }
    }

    init {
        // Removed global hotkey (jnativehook) registration per request.
        // Keep periodic updater to sync media position.
        scope.launch {
            while(true) {
                delay(1000)
                mediaPlayer?.let { p ->
                    try {
                        Platform.runLater {
                            val time = p.currentTime.toMillis().toLong()
                            _state.value = _state.value.copy(currentPosition = time)
                        }
                    } catch (_: Exception) { }
                }
            }
        }
    }

    actual fun pause() {
        Platform.runLater { mediaPlayer?.pause() }
    }

    actual fun resume() {
        Platform.runLater { mediaPlayer?.play() }
    }

    actual fun seekTo(position: Long) {
        Platform.runLater { mediaPlayer?.seek(Duration.millis(position.toDouble())) }
    }

    actual fun setVolume(volume: Float) {
        Platform.runLater { mediaPlayer?.volume = volume.toDouble() }
    }

    actual fun release() {
        Platform.runLater { mediaPlayer?.dispose() }
    }
}
