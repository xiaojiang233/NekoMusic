package top.xiaojiang233.nekomusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.xiaojiang233.nekomusic.api.NeteaseApi
import top.xiaojiang233.nekomusic.model.Artist
import top.xiaojiang233.nekomusic.model.Playlist
import top.xiaojiang233.nekomusic.model.UserDetailResponse
import top.xiaojiang233.nekomusic.model.Song

data class ProfileUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val userId: Long = 0,
    val userDetail: UserDetailResponse? = null,
    val userPlaylists: List<Playlist> = emptyList(),
    val likedSongs: List<Song> = emptyList(),
    val likedSongsCount: Int = 0,
    val followedArtists: List<Artist> = emptyList(),
    val listenHours: Int = 0,
    val error: String? = null
)

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()


    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // 1. Get Login Status to find UID
                println("ProfileViewModel: Fetching login status...")
                val status = NeteaseApi.getLoginStatus()
                val profile = status.data.profile

                if (profile != null) {
                    val uid = profile.userId
                    println("ProfileViewModel: Logged in as $uid")

                    // Initial update to show content immediately (even if empty)
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true, userId = uid) }

                    // 2. Fetch Data in Parallel
                    launch {
                        try {
                            val detail = NeteaseApi.getUserDetail(uid)
                            _uiState.update { it.copy(userDetail = detail) }
                        } catch (e: Exception) { e.printStackTrace() }
                    }

                    launch {
                        try {
                            val playlistsResponse = NeteaseApi.getUserPlaylist(uid)
                            val playlists = playlistsResponse.playlist
                            _uiState.update { it.copy(userPlaylists = playlists) }

                            // Load Liked Songs Preview after playlists
                            if (playlists.isNotEmpty()) {
                                 val likedPlaylistId = playlists[0].id
                                 val songCount = playlists[0].trackCount
                                 _uiState.update { it.copy(likedSongsCount = songCount) }

                                 try {
                                     val playlistDetail = NeteaseApi.getPlaylistDetail(likedPlaylistId)
                                     val trackIds = playlistDetail.playlist.trackIds.take(20).map { it.id }
                                     if (trackIds.isNotEmpty()) {
                                         val songsRes = NeteaseApi.getSongDetails(trackIds)
                                         _uiState.update { it.copy(likedSongs = songsRes.songs) }
                                     }
                                 } catch (e: Exception) { e.printStackTrace() }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }

                    launch {
                        try {
                            val artistsResponse = NeteaseApi.getArtistSublist(limit = 6)
                            _uiState.update { it.copy(followedArtists = artistsResponse.data) }
                        } catch (e: Exception) { e.printStackTrace() }
                    }

                    launch {
                        try {
                            val listenDataRes = NeteaseApi.getListenDataTotal()
                            if (listenDataRes.code == 200 && listenDataRes.data != null) {
                                val hours = (listenDataRes.data.totalDuration / 3600).toInt()
                                _uiState.update { it.copy(listenHours = hours) }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }

                } else {
                    println("ProfileViewModel: Not logged in")
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = false) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadFollows() {
        loadFullFollowedArtists()
    }

    fun loadFullFollowedArtists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val artistsResponse = NeteaseApi.getArtistSublist(limit = 1000)
                _uiState.update { it.copy(
                    isLoading = false,
                    followedArtists = artistsResponse.data
                ) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun unfollowArtist(artistId: Long) {
        viewModelScope.launch {
            try {
                val res = NeteaseApi.subArtist(artistId, false)
                if (res.code == 200) {
                    // Refresh the list
                    loadFullFollowedArtists()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addSongToPlaylist(pid: Long, songId: Long) {
        viewModelScope.launch {
            try {
                NeteaseApi.addSongToPlaylist("add", pid, listOf(songId))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePlaylist(pid: Long) {
        viewModelScope.launch {
            try {
                val res = NeteaseApi.deletePlaylist(pid)
                if (res.code == 200) {
                     loadProfile() // refresh
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createPlaylist(name: String, isPublic: Boolean) {
        viewModelScope.launch {
            try {
                val res = NeteaseApi.createPlaylist(name, if (isPublic) 0 else 10)
                if (res.code == 200) {
                    loadProfile()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
