package top.xiaojiang233.nekomusic.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import nekomusic.composeapp.generated.resources.Res
import nekomusic.composeapp.generated.resources.intelligence_mode_disabled
import nekomusic.composeapp.generated.resources.intelligence_mode_enabled
import org.jetbrains.compose.resources.getString
import top.xiaojiang233.nekomusic.api.NeteaseApi
import top.xiaojiang233.nekomusic.model.Song
import top.xiaojiang233.nekomusic.settings.SettingsManager

enum class PlaybackMode {
    LoopOne,    // Single loop
    Shuffle,    // Random
    Order,      // Order (with repeat list usually, or just stop at end?)
    Intelligence // Heartbeat mode
}

object PlayerController {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _playbackMode = MutableStateFlow(PlaybackMode.Order)
    val playbackMode: StateFlow<PlaybackMode> = _playbackMode.asStateFlow()

    private var isPersonalFm = false
    private var currentSourceId: Long = 0L
    private var originalQueue: List<Song>? = null
    private var originalIndex: Int? = null // Store original index
    // Store all track IDs for pagination
    private var allPlaylistTrackIds: List<Long>? = null

    // Helper to request Intelligence Mode
    fun enableIntelligenceMode() {
        if (_playbackMode.value != PlaybackMode.Intelligence) {
            _playbackMode.value = PlaybackMode.Intelligence
            setupIntelligenceMode()
            scope.launch {
                top.xiaojiang233.nekomusic.utils.showToast(getString(Res.string.intelligence_mode_enabled))
            }
        } else {
            // Already in Intelligence mode, maybe user wants to exit?
            // Usually clicking the heart mode button again should probably toggle it off or do nothing.
            // If the user feels it's "not triggering", maybe they exited it implicitly?
            // If they want to toggle it OFF by clicking the button again:
            togglePlaybackMode() // This handles exit from Intelligence -> LoopOne
            scope.launch {
                top.xiaojiang233.nekomusic.utils.showToast(getString(Res.string.intelligence_mode_disabled))
            }
        }
    }

    init {
        // Wire up AudioManager callbacks
        AudioManager.onNext = { playNext(auto = true) }
        AudioManager.onPrevious = { playPrevious() }
    }

    private fun attemptScrobble() {
        val currentSong = _queue.value.getOrNull(_currentIndex.value) ?: return
        val state = AudioManager.state.value
        // Converting to seconds
        val durationSec = state.duration / 1000
        val positionSec = state.currentPosition / 1000

        // If duration is invalid, skip
        if (durationSec <= 0) return

        // Scrobble condition:
        // 1. Played more than 60 seconds
        // OR
        // 2. Played more than 1/3 of the song
        val progress = positionSec.toFloat() / durationSec.toFloat()
        val shouldScrobble = positionSec > 60 || progress > 0.33f

        if (shouldScrobble) {
            scope.launch {
                try {
                    if (SettingsManager.enableScrobble().first()) {
                        NeteaseApi.scrobble(currentSong.id, currentSourceId, positionSec)
                        println("Scrobbled: ${currentSong.name} ($positionSec/$durationSec s)")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun playList(songs: List<Song>, startIndex: Int = 0, sourceId: Long = 0L, totalTrackIds: List<Long>? = null) {
        attemptScrobble()
        isPersonalFm = false
        // If Intelligence Mode was active, disable it when manually switching context
        if (_playbackMode.value == PlaybackMode.Intelligence) {
            _playbackMode.value = PlaybackMode.Order
            originalQueue = null
            originalIndex = null
        }
        currentSourceId = sourceId
        allPlaylistTrackIds = totalTrackIds
        originalQueue = null // Clear backup
        originalIndex = null
        _queue.value = songs
        _currentIndex.value = startIndex
        playCurrent()
    }

    fun playPersonalFm() {
        attemptScrobble()
        isPersonalFm = true
        // Disable Intelligence Mode if active
        if (_playbackMode.value == PlaybackMode.Intelligence) {
            _playbackMode.value = PlaybackMode.Order
        }
        originalQueue = null // Clear backup
        originalIndex = null
        _queue.value = emptyList()
        fetchAndPlayFm()
    }

    private fun fetchAndPlayFm() {
        scope.launch {
            try {
                val response = NeteaseApi.getPersonalFm()
                val newSongs = response.data?.map { it.toSong() } ?: emptyList()
                if (newSongs.isNotEmpty()) {
                    val currentQueue = _queue.value.toMutableList()
                    val wasEmpty = currentQueue.isEmpty()
                    currentQueue.addAll(newSongs)
                    _queue.value = currentQueue

                    if (wasEmpty) {
                         _currentIndex.value = 0
                         playCurrent()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun togglePlaybackMode() {
        val modes = PlaybackMode.entries

        // If currently in Intelligence mode, try to restore
        if (_playbackMode.value == PlaybackMode.Intelligence) {
            restoreFromIntelligenceMode()
        }

        // Logic adjusted: Cycle through LoopOne -> Shuffle -> Order -> LoopOne
        // Explicitly Skip Intelligence in toggle cycle if current is Order, Logic skips Intelligence?
        // Original cycle: LoopOne(0) -> Shuffle(1) -> Order(2) -> Intelligence(3)
        // User wants to remove Intelligence from toggle.
        // So toggle logic:
        var nextMode = when (_playbackMode.value) {
            PlaybackMode.LoopOne -> PlaybackMode.Shuffle
            PlaybackMode.Shuffle -> PlaybackMode.Order
            PlaybackMode.Order -> PlaybackMode.LoopOne
            PlaybackMode.Intelligence -> PlaybackMode.LoopOne // If in intell, exit to Loop or Order? Usually LoopOne starts the cycle.
        }

        _playbackMode.value = nextMode

        // Intelligence setup is handled only via dedicated button now
        // if (_playbackMode.value == PlaybackMode.Intelligence) ... removed from toggle
    }

    fun restoreFromIntelligenceMode() {
        // Only restore if we have a backup
        val backup = originalQueue ?: return
        val savedIndex = originalIndex

        // Restore original queue
        _queue.value = backup

        // If we have a saved index, restore it and play that song
        // This fulfills "return to original song"
        if (savedIndex != null && savedIndex in backup.indices) {
            _currentIndex.value = savedIndex
            playCurrent()
        } else {
            // Fallback: try to find current song in backup
            val currentSong = _queue.value.getOrNull(_currentIndex.value)
            if (currentSong != null) {
                val indexInOriginal = backup.indexOfFirst { it.id == currentSong.id }
                if (indexInOriginal != -1) {
                    _currentIndex.value = indexInOriginal
                    // No need to playCurrent() if it's the same song, but since we just swapped queue,
                    // the played song object might be different instance?
                    // Usually safer to just not interrupt if same song, but user asked to "return to original song".
                    // If current song is NOT from original queue (intelligence song),
                    // we definitely want to switch back to original.
                } else {
                    // Current song not in original queue.
                    // Just go to 0 or keep last known good?
                    // User said "return to original song".
                    _currentIndex.value = 0
                    playCurrent()
                }
            } else {
                 _currentIndex.value = 0
                 playCurrent()
            }
        }

        originalQueue = null
        originalIndex = null
    }

    private fun setupIntelligenceMode() {
        val currentSong = _queue.value.getOrNull(_currentIndex.value)
        if (currentSong == null) {
            // Can't start intelligence without a song. Revert or skip?
            // Revert for now
            _playbackMode.value = PlaybackMode.Order
            return
        }

        // Backup if not already backed up
        if (originalQueue == null) {
            originalQueue = _queue.value
            originalIndex = _currentIndex.value // Save current index
        }

        scope.launch {
            try {
                // Fetch intelligence list based on current song
                fetchAndAppendIntelligenceList(currentSong.id, currentSourceId)
            } catch (e: Exception) {
                e.printStackTrace()
                // On failure, maybe revert mode?
                _playbackMode.value = PlaybackMode.Order
                restoreFromIntelligenceMode() // Restore if we failed
            }
        }
    }

    private suspend fun fetchAndAppendIntelligenceList(songId: Long, playlistId: Long) {
        // Fallback for playlistId if 0 (e.g. daily recommend)
        var pid = playlistId
        if (pid == 0L) {
            try {
                val status = NeteaseApi.getLoginStatus()
                val uid = status.data.profile?.userId
                if (uid != null) {
                    val playlists = NeteaseApi.getUserPlaylist(uid)
                    // Usually first playlist is liked songs
                    pid = playlists.playlist.firstOrNull()?.id ?: 0L
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // It seems pid being 0 is not fatal for the API call if we just fallback to user's liked songs or similar?
        // But if we fail to get pid, we return.
        if (pid == 0L) {
            // Try to proceed with just songId? Or fail? The API seems to require pid.
            // If we can't get PID, we probably can't run intelligence mode properly.
            // But maybe we should not fail silently?
            println("Intelligence Mode: Failed to determine PID")
            return
        }

        val response = NeteaseApi.getIntelligenceList(id = songId, pid = pid)
        val newSongs = response.data.mapNotNull { it.songInfo }

        if (newSongs.isNotEmpty()) {
            val currentQueue = _queue.value.toMutableList()
            val currentIndex = _currentIndex.value

            // Keep history up to current, replace future
            // If current is valid
            if (currentIndex in currentQueue.indices) {
                // Remove everything after current
                while (currentQueue.size > currentIndex + 1) {
                    currentQueue.removeAt(currentQueue.lastIndex)
                }
                // Append new songs
                currentQueue.addAll(newSongs)
                _queue.value = currentQueue
            }
        }
    }

    fun playNext(auto: Boolean = false) {
        val queue = _queue.value
        if (queue.isEmpty()) return

        // Check scrobble before moving index, regardless of auto or manual
        // But only if we are actually playing a song.
        // If auto=true, we assume the song finished, so checking position is valid (should be near end).
        // If manual (auto=false), we check if playback was long enough.
        attemptScrobble()

        if (isPersonalFm) {
            val nextIndex = _currentIndex.value + 1
            if (nextIndex >= queue.size) {
                // End of current buffer
                // We should have prefetched, but if not, fetch now
                fetchAndPlayFm()
                // Wait? No, fetch is async. We might stop for a bit.
                // Ideally we handle this state. For now just return.
            } else {
                _currentIndex.value = nextIndex
                playCurrent()
                // Prefetch if running low
                if (queue.size - nextIndex < 2) {
                    fetchAndPlayFm()
                }
            }
            return
        }

        if (_playbackMode.value == PlaybackMode.Intelligence) {
             val nextIndex = _currentIndex.value + 1
             // If we are reaching the end, fetch more?
             // Intelligence list usually returns ~10 songs.
             // If we are at the last song, we need to fetch more based on THIS song.

             if (nextIndex >= queue.size) {
                 // Should have fetched already? Or fetch now.
                 // Fetch based on current song (last one)
                 val currentSong = queue.lastOrNull()
                 if (currentSong != null) {
                     scope.launch {
                         fetchAndAppendIntelligenceList(currentSong.id, currentSourceId)
                         // If success, we have more songs now.
                         if (_queue.value.size > nextIndex) {
                             _currentIndex.value = nextIndex
                             playCurrent()
                         }
                     }
                     // Getting more might take time.
                     return
                 }
             } else {
                 _currentIndex.value = nextIndex
                 playCurrent()

                 // Pre-fetch if near end
                 if (queue.size - nextIndex < 2) {
                     val currentSong = queue[nextIndex]
                     scope.launch {
                         fetchAndAppendIntelligenceList(currentSong.id, currentSourceId)
                     }
                 }
                 return
             }
        }

        // Pagination Check for Playlist
        // Only if not in Shuffle mode (shuffle logic usually works on loaded set, or we shuffle ALL ids?)
        // If Shuffle, we might want to ensure we have all songs? Or shuffle the loaded 50?
        // Usually Play All -> Order.
        // If we are at the end of the loaded queue, and we have more IDs in the playlist
        if (_playbackMode.value == PlaybackMode.Order &&
            _currentIndex.value >= queue.size - 1 &&
            allPlaylistTrackIds != null &&
            queue.size < allPlaylistTrackIds!!.size) {

            val nextOffset = queue.size
            val limit = 50
            val idsToFetch = allPlaylistTrackIds!!.drop(nextOffset).take(limit)

            if (idsToFetch.isNotEmpty()) {
                scope.launch {
                    try {
                        val response = NeteaseApi.getSongDetails(idsToFetch)
                        if (response.code == 200 && response.songs.isNotEmpty()) {
                            val currentQ = _queue.value.toMutableList()
                            currentQ.addAll(response.songs)
                            _queue.value = currentQ

                            // Now play next (which is at nextOffset)
                            _currentIndex.value = nextOffset
                            playCurrent()
                        } else {
                            // Failed to fetch, just loop back
                            _currentIndex.value = 0
                            playCurrent()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Fallback
                        _currentIndex.value = 0
                        playCurrent()
                    }
                }
                return // Wait for fetch
            }
        }

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

        attemptScrobble()

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

    fun addToNext(song: Song) {
        val currentQueue = _queue.value.toMutableList()
        val current = _currentIndex.value

        if (currentQueue.isEmpty()) {
            playList(listOf(song))
            return
        }

        // Add after current
        currentQueue.add(current + 1, song)
        _queue.value = currentQueue
    }

    fun removeFromQueue(index: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (index !in currentQueue.indices) return

        currentQueue.removeAt(index)
        _queue.value = currentQueue

        // Adjust index
        val current = _currentIndex.value
        if (index < current) {
            _currentIndex.value = current - 1
        } else if (index == current) {
            // Removed currently playing song
            // Either play next (now at current index) or stop if empty
            if (currentQueue.isEmpty()) {
                // Stop? For now just keep index 0
                _currentIndex.value = 0
            } else {
                // Determine what to play? Usually play next which is now at `current`.
                // If it was the last one, current is now out of bounds
                if (current >= currentQueue.size) {
                    _currentIndex.value = 0 // Loop back or stop?
                }
                playCurrent()
            }
        }
    }

    fun reorderQueue(from: Int, to: Int) {
        val currentQueue = _queue.value.toMutableList()
        val item = currentQueue.removeAt(from)
        currentQueue.add(to, item)
        _queue.value = currentQueue

        // Adjust current index
        val current = _currentIndex.value
        if (current == from) {
            _currentIndex.value = to
        } else if (current in (to + 1)..from) {
             _currentIndex.value = current + 1
        } else if (current in from until to) {
             _currentIndex.value = current - 1
        }
    }

    fun playAt(index: Int) {
        val queue = _queue.value
        // If Intelligence Mode was active, disable it and revert to order when manually picking a song
        if (_playbackMode.value == PlaybackMode.Intelligence) {
            _playbackMode.value = PlaybackMode.Order
            originalQueue = null
            originalIndex = null
        }
        if (index in queue.indices) {
            _currentIndex.value = index
            playCurrent()
        }
    }

    fun disableIntelligenceMode() {
        if (_playbackMode.value == PlaybackMode.Intelligence) {
            restoreFromIntelligenceMode()
            _playbackMode.value = PlaybackMode.Order // Or restore previous? Order is safe default.
            scope.launch {
                top.xiaojiang233.nekomusic.utils.showToast(getString(Res.string.intelligence_mode_disabled))
            }
        }
    }
}
