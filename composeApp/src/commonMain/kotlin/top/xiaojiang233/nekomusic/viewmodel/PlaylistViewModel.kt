package top.xiaojiang233.nekomusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.api.NeteaseApi
import top.xiaojiang233.nekomusic.model.PlaylistDetail
import top.xiaojiang233.nekomusic.model.Song
import top.xiaojiang233.nekomusic.model.TrackId
import top.xiaojiang233.nekomusic.player.AudioManager
import top.xiaojiang233.nekomusic.settings.SettingsManager

data class PlaylistUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val playlist: PlaylistDetail? = null,
    val tracks: List<Song> = emptyList(),
    val error: String? = null,
    val hasMore: Boolean = false,
    val currentUserId: Long = 0
)

class PlaylistViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    private var allTrackIds: List<TrackId> = emptyList()
    private var currentOffset = 0
    private val pageSize = 50

    init {
        // Try to get current user ID
        viewModelScope.launch {
            try {
                // We should cache this or get from SettingsManager if stored?
                // For now, simple fetch if not expensive, or better:
                // Since this might be called frequently, maybe we store UID in Settings?
                // Re-using NeteaseApi.getLoginStatus() is okay-ish.
                val status = NeteaseApi.getLoginStatus()
                println("PlaylistViewModel: Current user ID: ${status.data.profile?.userId}")
                _uiState.value = _uiState.value.copy(currentUserId = status.data.profile?.userId ?: 0)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun loadPlaylist(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val detailResponse = NeteaseApi.getPlaylistDetail(id)
                if (detailResponse.code == 200) {
                    val detail = detailResponse.playlist
                    allTrackIds = detail.trackIds
                    currentOffset = 0

                    // Fetch first page
                    val initialIds = allTrackIds.take(pageSize).map { it.id }
                    val songsResponse = if (initialIds.isNotEmpty()) {
                        NeteaseApi.getSongDetails(initialIds)
                    } else {
                        null
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        playlist = detail,
                        tracks = songsResponse?.songs ?: emptyList(),
                        hasMore = allTrackIds.size > pageSize
                    )
                    currentOffset = pageSize
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load playlist: Code ${detailResponse.code}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadMoreSongs() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            try {
                val nextIds = allTrackIds.drop(currentOffset).take(pageSize).map { it.id }
                if (nextIds.isNotEmpty()) {
                    val songsResponse = NeteaseApi.getSongDetails(nextIds)
                    val newTracks = _uiState.value.tracks + songsResponse.songs

                    currentOffset += pageSize
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        tracks = newTracks,
                        hasMore = currentOffset < allTrackIds.size
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false, hasMore = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoadingMore = false) // Fail silently or show toast?
            }
        }
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            try {
                // Get preferred quality
                val quality = SettingsManager.getAudioQuality().first()
                println("Requesting song URL for id=${song.id} with level=$quality")

                val response = NeteaseApi.getSongUrl(song.id, level = quality)
                if (response.code == 200 && response.data.isNotEmpty()) {
                    val url = response.data[0].url
                    println("Got song URL: $url")
                    if (url != null) {
                        AudioManager.play(url, song)
                    } else {
                        // TODO: Handle no URL (e.g. VIP only)
                        println("Song URL is null (VIP required?)")
                    }
                } else {
                    println("Failed to get song URL: ${response.code}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        allTrackIds = emptyList()
        _uiState.value = PlaylistUiState() // Reset
    }
}
