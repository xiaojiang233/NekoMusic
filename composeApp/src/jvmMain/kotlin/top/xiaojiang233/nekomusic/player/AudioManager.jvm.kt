package top.xiaojiang233.nekomusic.player

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
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
import java.util.logging.Level
import java.util.logging.Logger

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
            } catch (e: Exception) {
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
                val bytes = NetworkClient.client.get(url).readBytes()
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
        // Initialize Global Hotkeys (SMTC / Media Keys)
        try {
            // Disable JNativeHook logging
            val logger = Logger.getLogger("com.github.kwhat.jnativehook")
            logger.level = Level.OFF
            logger.useParentHandlers = false

            GlobalScreen.registerNativeHook()
            GlobalScreen.addNativeKeyListener(object : NativeKeyListener {
                override fun nativeKeyPressed(e: NativeKeyEvent) {
                    when (e.keyCode) {
                        NativeKeyEvent.VC_MEDIA_PLAY -> {
                            if (_state.value.isPlaying) pause() else resume()
                        }
                        NativeKeyEvent.VC_MEDIA_STOP -> pause()
                        NativeKeyEvent.VC_MEDIA_NEXT -> onNext?.invoke()
                        NativeKeyEvent.VC_MEDIA_PREVIOUS -> onPrevious?.invoke()
                    }
                }
            })
        } catch (e: Exception) {
            println("Failed to register global hotkeys: ${e.message}")
        }

        scope.launch {
            while(true) {
                delay(1000)
                mediaPlayer?.let { p ->
                    try {
                        Platform.runLater {
                            val time = p.currentTime.toMillis().toLong()
                            _state.value = _state.value.copy(currentPosition = time)
                        }
                    } catch(e: Exception) { }
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

    actual fun release() {
        Platform.runLater { mediaPlayer?.dispose() }
    }
}
